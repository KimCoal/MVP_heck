import sys
import os

import FreeCAD as App
import Mesh
import Import
import Part


def main():
    if len(sys.argv) < 3:
        print("Usage: FreeCADCmd.exe step_iges_to_stl.py <input.step/iges> <output.stl>", file=sys.stderr)
        sys.exit(2)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    if not os.path.exists(input_path):
        print(f"Input not found: {input_path}", file=sys.stderr)
        sys.exit(2)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    doc = App.newDocument("doc")

    # 1) Import (GUI 없이)
    Import.insert(input_path, doc.Name)
    doc.recompute()

    # 2) 모든 Shape를 "결합(fuse)"하지 말고, Compound로만 묶기 (대형 어셈블리 안전)
    shapes = []
    for obj in doc.Objects:
        if hasattr(obj, "Shape") and obj.Shape and not obj.Shape.isNull():
            shapes.append(obj.Shape)

    if not shapes:
        print("No shapes found in document.", file=sys.stderr)
        sys.exit(4)

    compound = Part.makeCompound(shapes)

    # 3) MVP용: 매우 거친 테셀레이션(빠르게 성공 우선)
    linear_deflection = 10.0   # 더 크면 더 거칠고 빠름
    angular_deflection = 0.9  # 라디안, 더 크면 더 거칠 수 있음

    # Shape.tessellate -> (points, triangles) 형태 반환
    pts, tris = compound.tessellate(linear_deflection)

    mesh = Mesh.Mesh((pts, tris))

    # 4) STL 저장
    mesh.write(output_path)

    print("OK")


if __name__ == "__main__":
    main()
