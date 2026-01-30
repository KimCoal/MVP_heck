# CAD 파일 뷰어 프로젝트

STL, OBJ, PLY 파일을 업로드하여 3D로 시각화하고 각 부품에 메모를 추가할 수 있는 웹 애플리케이션입니다.

## 기능

- CAD 파일 업로드 (STL, OBJ, PLY)
- Python trimesh를 사용한 GLB 변환
- Three.js를 사용한 3D 렌더링
- 부품별 메타데이터 추출 및 저장
- 부품 클릭 시 상세 정보 표시
- 부품별 메모 추가/수정/삭제

## 기술 스택

### Backend
- Spring Boot 4.0.2
- Java 17
- MyBatis
- MySQL
- Python 3.11+ (trimesh)

### Frontend
- React 19.2.0
- Three.js
- @react-three/fiber
- @react-three/drei
- Vite

## 설치 및 실행

### 1. 데이터베이스 설정

MySQL 데이터베이스를 생성하고 `backend/src/main/resources/schema.sql` 파일을 실행합니다.

```sql
CREATE DATABASE cad_project;
USE cad_project;
-- schema.sql 파일의 내용 실행
```

`backend/src/main/resources/application.properties` 파일에서 데이터베이스 연결 정보를 수정합니다.

### 2. Python 환경 설정

Python 3.11 이상이 설치되어 있어야 합니다.

```bash
cd python
pip install -r requirements.txt
```

### 3. Backend 실행

```bash
cd backend
./mvnw spring-boot:run
```

또는

```bash
cd backend
mvn spring-boot:run
```

### 4. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

## 사용 방법

1. 웹 브라우저에서 `http://localhost:5173` 접속
2. "CAD 파일 선택" 버튼을 클릭하여 STL, OBJ, PLY 파일 업로드
3. 파일이 처리되면 3D 모델이 표시됩니다
4. 모델의 부품을 클릭하면 상세 정보가 표시됩니다
5. 부품 상세 정보에서 메모를 추가/수정/삭제할 수 있습니다

## API 엔드포인트

- `POST /api/cad/upload` - CAD 파일 업로드
- `GET /api/cad/files` - 업로드된 파일 목록
- `GET /api/cad/files/{id}` - 파일 상세 정보
- `GET /api/cad/files/{id}/glb` - GLB 파일 다운로드
- `GET /api/cad/files/{id}/parts` - 부품 목록
- `GET /api/parts/{id}` - 부품 상세 정보
- `POST /api/parts/{id}/note` - 부품 메모 저장/수정
- `DELETE /api/parts/{id}/note` - 부품 메모 삭제

## 파일 구조

```
backend/
  src/main/java/com/korit/project/backend/
    controller/     # REST API 컨트롤러
    service/        # 비즈니스 로직
    mapper/         # MyBatis 매퍼
    entity/         # 엔티티 클래스
    dto/            # DTO 클래스
    config/         # 설정 클래스
  src/main/resources/
    mapper/         # MyBatis XML 매퍼
    schema.sql      # 데이터베이스 스키마

frontend/
  src/
    components/     # React 컴포넌트
    services/       # API 서비스

python/
  worker/
    cad_converter.py  # CAD 파일 변환 스크립트
  requirements.txt    # Python 의존성
```

## 주의사항

- Python 스크립트 경로는 `application.properties`의 `python.worker.script-path`에서 설정합니다
- 임시 파일은 `temp/upload/`와 `temp/converted/` 디렉토리에 저장됩니다
- 파일 크기 제한은 기본적으로 500MB입니다
