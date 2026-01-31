#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
STEP/IGES Assembly -> Part meshes + metadata (MVP)

- Load STEP/IGES with FreeCAD (headless via FreeCADCmd.exe)
- Iterate doc objects that have Shape
- For each object:
  - Generate stable partKey (fc:<Object.Name>)
  - Export a mesh file per part (default: STL)
  - Compute bbox-derived position/size
- Output JSON:
  - By default: stdout
  - Optional: --json-out <path> to write a clean JSON file

Usage:
  step_to_parts.py <input_path> <output_dir>
                  [--format stl|ply|obj]
                  [--linear 10.0] [--angular 0.9] [--relative]
                  [--max-parts 0]
                  [--skip-small 0.0]
                  [--skip-huge 1e50]
                  [--skip-degenerate]
                  [--exclude-keywords axis,plane,datum,origin,sketch,csys]
                  [--no-name-filter]
                  [--json-out <path>]
                  [--no-hierarchy]
                  [--quiet]

Examples:
  FreeCADCmd.exe -c "import runpy, sys; sys.argv=[r'step_to_parts.py',
      r'E:/.../sample.step',
      r'E:/.../temp/parts/999',
      r'--format', r'stl',
      r'--linear', r'10.0',
      r'--json-out', r'E:/.../temp/parts/999/parts.json'
  ]; runpy.run_path(r'E:/.../step_to_parts.py', run_name='__main__')"
"""

import sys
import os
import json
import argparse
import traceback
import math

# FreeCAD modules (available only under FreeCADCmd environment)
import FreeCAD as App
import Import
import MeshPart


# ----------------------------
# Helpers
# ----------------------------

def _safe_filename(name: str) -> str:
    # Windows-safe filename
    unsafe = ['<', '>', ':', '"', '/', '\\', '|', '?', '*']
    for ch in unsafe:
        name = name.replace(ch, "_")
    return name.strip() or "part"


def _safe_node_segment(name: str) -> str:
    # nodePath safe segment (avoid slashes)
    if name is None:
        return "part"
    name = str(name).strip()
    name = name.replace("\\", "_").replace("/", "_")
    return name or "part"


def _to_pos_size_from_boundbox(bb):
    # bb: FreeCAD BoundBox
    size_x = float(bb.XLength)
    size_y = float(bb.YLength)
    size_z = float(bb.ZLength)

    cx = float((bb.XMin + bb.XMax) / 2.0)
    cy = float((bb.YMin + bb.YMax) / 2.0)
    cz = float((bb.ZMin + bb.ZMax) / 2.0)
    return (cx, cy, cz), (size_x, size_y, size_z)


def _export_mesh_from_shape(
        shape,
        out_path: str,
        mesh_format: str,
        linear_deflection: float,
        angular_deflection: float,
        relative: bool = False,
):
    """
    Create mesh from FreeCAD shape and write to file.
    Uses MeshPart.meshFromShape (more stable than Shape.tessellate signature differences).
    """
    mesh = MeshPart.meshFromShape(
        Shape=shape,
        LinearDeflection=linear_deflection,
        AngularDeflection=angular_deflection,
        Relative=relative
    )

    mesh_format = mesh_format.lower().strip()
    if mesh_format not in ("stl", "ply", "obj"):
        raise ValueError(f"Unsupported mesh format: {mesh_format}")

    # FreeCAD mesh write chooses format by extension.
    mesh.write(out_path)


def _build_parent_index(objects):
    """
    Best-effort hierarchy:
    FreeCAD STEP import doesn't always preserve assembly tree cleanly.
    We'll attempt to infer parent using InList (links) if available.
    Returns: dict child_name -> parent_name (both are obj.Name)
    """
    name_to_obj = {o.Name: o for o in objects}
    parent_of = {}

    for obj in objects:
        try:
            in_list = getattr(obj, "InList", None)
            if not in_list:
                continue

            for parent in in_list:
                if parent is None:
                    continue
                if parent.Name == obj.Name:
                    continue
                if parent.Name in name_to_obj:
                    parent_of[obj.Name] = parent.Name
                    break
        except Exception:
            continue

    return parent_of


def _looks_like_reference(name: str, keywords) -> bool:
    """
    Detect helper/reference geometry by name/label patterns (axis/plane/datum/origin...).
    """
    if not name:
        return False
    n = str(name).lower()
    return any(k in n for k in keywords)


def _is_bad_bbox(sx: float, sy: float, sz: float, huge: float, skip_degenerate: bool) -> bool:
    """
    Filter abnormal bbox:
    - NaN/Inf
    - extremely large (e.g. 2e+100)
    - (optional) degenerate (axis/plane-like) thickness ~ 0
    """
    vals = [sx, sy, sz]

    # NaN/Inf
    if any(not math.isfinite(v) for v in vals):
        return True

    # Huge (axis/plane in FreeCAD can be effectively infinite)
    if any(v >= huge for v in vals):
        return True

    if skip_degenerate:
        tiny = 1e-9
        zero_axes = sum(v <= tiny for v in vals)
        # axis-like (two axes nearly zero)
        if zero_axes >= 2:
            return True

    return False


def parse_args(argv):
    p = argparse.ArgumentParser(add_help=True)
    p.add_argument("input_path", help="STEP/IGES file path")
    p.add_argument("output_dir", help="Directory to write per-part mesh files")

    p.add_argument("--format", default="stl", choices=["stl", "ply", "obj"], help="Mesh output format")
    p.add_argument("--linear", type=float, default=10.0, help="LinearDeflection (bigger -> coarser, faster, smaller files)")
    p.add_argument("--angular", type=float, default=0.9, help="AngularDeflection (radians)")
    p.add_argument("--relative", action="store_true", help="Use Relative deflection (FreeCAD meshing)")
    p.add_argument("--max-parts", type=int, default=0, help="0 = unlimited; otherwise stop after N parts (MVP safety)")

    p.add_argument("--skip-small", type=float, default=0.0, help="Skip parts with bbox diagonal < value (0=off)")
    p.add_argument("--skip-huge", type=float, default=1e50, help="Skip parts if any bbox axis >= value (filters axis/plane infinite boxes)")
    p.add_argument("--skip-degenerate", action="store_true", help="Skip degenerate bbox parts (axis-like thickness ~ 0)")
    p.add_argument("--exclude-keywords", default="axis,plane,datum,origin,sketch,csys",
                   help="Comma-separated keywords to filter by name/label (case-insensitive).")
    p.add_argument("--no-name-filter", action="store_true", help="Disable keyword-based name filtering")

    p.add_argument("--json-out", default="", help="Write JSON metadata to this file path (optional). If empty, print to stdout.")
    p.add_argument("--no-hierarchy", action="store_true", help="Do not attempt parent inference")
    p.add_argument("--quiet", action="store_true", help="Less stderr logs")
    return p.parse_args(argv)


def main():
    args = parse_args(sys.argv[1:])

    input_path = args.input_path
    out_dir = args.output_dir
    mesh_format = args.format.lower()
    linear_deflection = float(args.linear)
    angular_deflection = float(args.angular)
    relative = bool(args.relative)
    max_parts = int(args.max_parts)
    skip_small = float(args.skip_small)
    skip_huge = float(args.skip_huge)
    skip_degenerate = bool(args.skip_degenerate)

    keywords = [k.strip().lower() for k in (args.exclude_keywords or "").split(",") if k.strip()]
    use_name_filter = (not args.no_name_filter) and (len(keywords) > 0)

    if not os.path.exists(input_path):
        print(f"ERROR: input file not found: {input_path}", file=sys.stderr)
        sys.exit(2)

    os.makedirs(out_dir, exist_ok=True)

    # 1) Load STEP/IGES
    doc = App.newDocument("doc")
    try:
        Import.insert(input_path, doc.Name)
        doc.recompute()
    except Exception as e:
        print(f"ERROR: Failed to import file: {e}", file=sys.stderr)
        if not args.quiet:
            traceback.print_exc()
        sys.exit(3)

    # 2) Collect candidate objects with shape + apply early filters
    candidates = []
    for obj in doc.Objects:
        try:
            if not hasattr(obj, "Shape"):
                continue

            shape = obj.Shape
            if shape is None or shape.isNull():
                continue

            bb = shape.BoundBox
            if bb is None:
                continue

            # compute bbox-based metrics for filters
            diag = (bb.DiagonalLength if hasattr(bb, "DiagonalLength")
                    else (bb.XLength**2 + bb.YLength**2 + bb.ZLength**2) ** 0.5)

            # small-part skip
            if skip_small > 0.0 and float(diag) < skip_small:
                continue

            # name/label filter (axis/plane/datum...)
            name = getattr(obj, "Label", None) or obj.Name
            if use_name_filter and (_looks_like_reference(name, keywords) or _looks_like_reference(obj.Name, keywords)):
                continue

            # huge/degenerate bbox filter
            sx = float(bb.XLength)
            sy = float(bb.YLength)
            sz = float(bb.ZLength)
            if _is_bad_bbox(sx, sy, sz, huge=skip_huge, skip_degenerate=skip_degenerate):
                continue

            candidates.append(obj)
        except Exception:
            continue

    # 3) Parent inference (best effort)
    parent_map = {} if args.no_hierarchy else _build_parent_index(candidates)

    # 4) Export each part mesh + metadata
    parts = []
    exported = 0

    for obj in candidates:
        if max_parts > 0 and exported >= max_parts:
            break

        try:
            shape = obj.Shape
            bb = shape.BoundBox
            (cx, cy, cz), (sx, sy, sz) = _to_pos_size_from_boundbox(bb)

            # stable key: fc:<internal unique name>
            part_key = f"fc:{obj.Name}"

            # human-facing name (can be renamed later via displayName in DB)
            name = getattr(obj, "Label", None) or obj.Name
            safe_name = _safe_node_segment(name)

            # nodePath / parentKey (best effort)
            parent_name = parent_map.get(obj.Name)
            parent_key = f"fc:{parent_name}" if parent_name else None
            if not parent_name:
                node_path = f"Root/{safe_name}"
            else:
                node_path = f"Root/{_safe_node_segment(parent_name)}/{safe_name}"

            # mesh file path
            safe_obj = _safe_filename(obj.Name)
            filename = f"fc__{safe_obj}.{mesh_format}"
            mesh_path = os.path.join(out_dir, filename)

            _export_mesh_from_shape(
                shape=shape,
                out_path=mesh_path,
                mesh_format=mesh_format,
                linear_deflection=linear_deflection,
                angular_deflection=angular_deflection,
                relative=relative
            )

            exported += 1

            parts.append({
                "partKey": part_key,
                "name": name,
                "meshPath": mesh_path.replace("\\", "/"),
                "nodePath": node_path,
                "parentKey": parent_key,
                "nodeIndex": None,   # will be filled later when building GLB Scene (optional)
                "position": [cx, cy, cz],
                "size": [sx, sy, sz],
            })

        except Exception as e:
            if not args.quiet:
                print(f"WARN: mesh export failed for {getattr(obj, 'Name', '?')}: {e}", file=sys.stderr)
            continue

    payload = {"parts": parts}

    # 5) Output JSON
    if args.json_out:
        # Ensure parent dir exists
        os.makedirs(os.path.dirname(args.json_out), exist_ok=True)
        with open(args.json_out, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
    else:
        # Print JSON (UTF-8) to stdout
        sys.stdout.buffer.write(json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8"))
        sys.stdout.write("\n")

    # If no parts, treat as error.
    if len(parts) == 0:
        print("ERROR: No meshable parts found.", file=sys.stderr)
        sys.exit(4)


if __name__ == "__main__":
    main()
