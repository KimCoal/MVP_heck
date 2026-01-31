#!/usr/bin/env python3
"""
CAD 파일 변환 및 메타데이터 추출 Worker
STL, OBJ, PLY 파일을 GLB로 변환하고 부품 정보를 추출합니다.

stdout(JSON):
{
  "parts": [
    {
      "partKey": "...",
      "name": "...",
      "nodeIndex": null,
      "nodePath": "Root/...",
      "parentKey": null,
      "position": [x,y,z],
      "size": [sx,sy,sz]
    }
  ]
}
"""

import sys
import json
import os
import trimesh
from pathlib import Path


def convert_to_glb(input_path, output_path):
    try:
        mesh = trimesh.load(input_path)

        if isinstance(mesh, trimesh.Trimesh):
            mesh.export(output_path, file_type='glb')
            return True
        elif isinstance(mesh, trimesh.Scene):
            mesh.export(output_path, file_type='glb')
            return True
        else:
            print(f"ERROR: Unsupported mesh type: {type(mesh)}", file=sys.stderr)
            return False

    except Exception as e:
        print(f"ERROR: Failed to convert file: {str(e)}", file=sys.stderr)
        return False


def extract_metadata(input_path):
    parts = []

    try:
        mesh = trimesh.load(input_path)
        used_keys = set()

        # 단일 메시
        if isinstance(mesh, trimesh.Trimesh):
            bounds = mesh.bounds
            center = mesh.centroid

            name = Path(input_path).stem
            node_path = f"Root/{name}"

            part_key = f"m:{name}"
            if part_key in used_keys:
                suffix = 1
                while f"{part_key}:{suffix}" in used_keys:
                    suffix += 1
                part_key = f"{part_key}:{suffix}"
            used_keys.add(part_key)

            parts.append({
                "partKey": part_key,
                "name": name,
                "nodeIndex": None,
                "nodePath": node_path,
                "parentKey": None,
                "position": [float(center[0]), float(center[1]), float(center[2])],
                "size": [
                    float(bounds[1][0] - bounds[0][0]),
                    float(bounds[1][1] - bounds[0][1]),
                    float(bounds[1][2] - bounds[0][2])
                ]
            })

        # Scene(여러 geometry)
        elif isinstance(mesh, trimesh.Scene):
            for i, (geom_name, geometry) in enumerate(mesh.geometry.items()):
                if not isinstance(geometry, trimesh.Trimesh):
                    continue

                bounds = geometry.bounds
                center = geometry.centroid

                name = geom_name if geom_name else f"Part_{i+1}"
                node_path = f"Root/{name}"

                base = geom_name if geom_name else f"Part_{i+1}"
                part_key = f"g:{base}"

                if part_key in used_keys:
                    suffix = 1
                    while f"{part_key}:{suffix}" in used_keys:
                        suffix += 1
                    part_key = f"{part_key}:{suffix}"
                used_keys.add(part_key)

                parts.append({
                    "partKey": part_key,
                    "name": name,
                    "nodeIndex": None,
                    "nodePath": node_path,
                    "parentKey": None,
                    "position": [float(center[0]), float(center[1]), float(center[2])],
                    "size": [
                        float(bounds[1][0] - bounds[0][0]),
                        float(bounds[1][1] - bounds[0][1]),
                        float(bounds[1][2] - bounds[0][2])
                    ]
                })

        return {"parts": parts}

    except Exception as e:
        print(f"ERROR: Failed to extract metadata: {str(e)}", file=sys.stderr)
        return {"parts": []}


def main():
    if len(sys.argv) != 3:
        print("ERROR: Usage: python cad_converter.py <input_path> <output_path>", file=sys.stderr)
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    if not os.path.exists(input_path):
        print(f"ERROR: Input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    if not convert_to_glb(input_path, output_path):
        sys.exit(1)

    metadata = extract_metadata(input_path)
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
