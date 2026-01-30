#!/usr/bin/env python3
"""
CAD 파일 변환 및 메타데이터 추출 Worker
STL, OBJ, PLY 파일을 GLB로 변환하고 부품 정보를 추출합니다.
"""

import sys
import json
import os
import trimesh
from pathlib import Path

def convert_to_glb(input_path, output_path):
    """
    CAD 파일을 GLB 형식으로 변환
    
    Args:
        input_path: 입력 파일 경로 (STL, OBJ, PLY)
        output_path: 출력 GLB 파일 경로
    
    Returns:
        bool: 변환 성공 여부
    """
    try:
        # trimesh로 메시 로드
        mesh = trimesh.load(input_path)
        
        # 단일 메시인 경우
        if isinstance(mesh, trimesh.Trimesh):
            mesh.export(output_path, file_type='glb')
            return True
        # 여러 메시인 경우 (Scene)
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
    """
    CAD 파일에서 부품 메타데이터 추출
    
    Args:
        input_path: 입력 파일 경로
    
    Returns:
        dict: 부품 정보 리스트를 포함한 딕셔너리
    """
    parts = []
    
    try:
        # trimesh로 메시 로드
        mesh = trimesh.load(input_path)
        
        # 단일 메시인 경우
        if isinstance(mesh, trimesh.Trimesh):
            bounds = mesh.bounds
            center = mesh.centroid
            
            parts.append({
                "name": Path(input_path).stem,
                "position": [float(center[0]), float(center[1]), float(center[2])],
                "size": [
                    float(bounds[1][0] - bounds[0][0]),  # size_x
                    float(bounds[1][1] - bounds[0][1]),  # size_y
                    float(bounds[1][2] - bounds[0][2])   # size_z
                ]
            })
        
        # 여러 메시인 경우 (Scene)
        elif isinstance(mesh, trimesh.Scene):
            for i, (name, geometry) in enumerate(mesh.geometry.items()):
                if isinstance(geometry, trimesh.Trimesh):
                    bounds = geometry.bounds
                    center = geometry.centroid
                    
                    part_name = name if name else f"Part_{i+1}"
                    
                    parts.append({
                        "name": part_name,
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
    """
    메인 함수
    명령줄 인자:
        sys.argv[1]: 입력 파일 경로
        sys.argv[2]: 출력 GLB 파일 경로
    
    출력:
        JSON 형식의 부품 메타데이터 (stdout)
    """
    if len(sys.argv) != 3:
        print("ERROR: Usage: python cad_converter.py <input_path> <output_path>", file=sys.stderr)
        sys.exit(1)
    
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    
    # 입력 파일 존재 확인
    if not os.path.exists(input_path):
        print(f"ERROR: Input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)
    
    # 출력 디렉토리 생성
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # 파일 변환
    if not convert_to_glb(input_path, output_path):
        sys.exit(1)
    
    # 메타데이터 추출
    metadata = extract_metadata(input_path)
    
    # JSON 출력
    print(json.dumps(metadata, indent=2))

if __name__ == "__main__":
    main()
