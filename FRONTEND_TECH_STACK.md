# Frontend 기술 스택

Python 워커를 통한 CAD 파일 변환 작업이 필요 없는 경우의 프론트엔드 기술 스택입니다.

## 핵심 프레임워크

### React
- **버전**: 18.3.1
- **용도**: UI 라이브러리
- **주요 기능**:
  - 컴포넌트 기반 개발
  - 상태 관리 (useState, useEffect)
  - 가상 DOM

### React DOM
- **버전**: 18.3.1
- **용도**: React를 DOM에 렌더링

## 3D 렌더링

### Three.js
- **버전**: 0.169.0
- **용도**: 3D 그래픽 라이브러리
- **기능**:
  - WebGL 기반 3D 렌더링
  - GLB/GLTF 파일 로드
  - 3D 모델 표시 및 조작

### @react-three/fiber
- **버전**: 8.17.10
- **용도**: Three.js의 React 래퍼
- **기능**:
  - React 컴포넌트로 3D 씬 구성
  - 선언적 3D 렌더링
  - 자동 리소스 관리

### @react-three/drei
- **버전**: 9.114.3
- **용도**: Three.js 유틸리티 라이브러리
- **주요 기능**:
  - OrbitControls (카메라 제어)
  - Grid (격자 표시)
  - useGLTF (GLB 파일 로드)
  - 기타 헬퍼 컴포넌트

## HTTP 클라이언트

### Axios
- **버전**: 1.7.7
- **용도**: HTTP 요청 처리
- **기능**:
  - RESTful API 호출
  - 요청/응답 인터셉터
  - 에러 처리

## 빌드 도구

### Vite
- **버전**: 7.2.4
- **용도**: 프론트엔드 빌드 도구
- **기능**:
  - 빠른 개발 서버
  - HMR (Hot Module Replacement)
  - 프로덕션 빌드 최적화

### @vitejs/plugin-react
- **버전**: 5.1.1
- **용도**: Vite의 React 플러그인

## 코드 품질

### ESLint
- **버전**: 9.39.1
- **용도**: 코드 린팅
- **플러그인**:
  - eslint-plugin-react-hooks
  - eslint-plugin-react-refresh

### TypeScript 타입 정의
- **@types/react**: 18.3.12
- **@types/react-dom**: 18.3.1
- **용도**: TypeScript 타입 지원 (선택사항)

## 프로젝트 구조

```
frontend/
├── src/
│   ├── api/              # API 호출 모듈
│   │   ├── axiosInstance.js  # Axios 인스턴스 설정
│   │   ├── resp.js           # 공통 응답 처리
│   │   ├── cadApi.js         # CAD 파일 API
│   │   └── partApi.js        # 부품 API
│   ├── components/       # React 컴포넌트
│   │   ├── FileUpload.jsx    # 파일 업로드
│   │   ├── ModelViewer.jsx   # 3D 모델 뷰어
│   │   ├── PartList.jsx      # 부품 목록
│   │   └── PartDetail.jsx    # 부품 상세
│   ├── App.jsx           # 메인 앱 컴포넌트
│   └── main.jsx          # 진입점
```

## 주요 기능

### 파일 관리
- GLB 파일 업로드
- 업로드된 파일 목록 표시
- 파일 선택 및 로드

### 3D 모델 뷰어
- GLB 파일 3D 렌더링
- 카메라 제어 (OrbitControls)
- 부품 클릭 이벤트 처리
- 부품 하이라이트

### 부품 관리
- 부품 목록 표시
- 부품 상세 정보 표시
- 부품 메모 CRUD
- 부품 이름 변경

## API 통신 패턴

### 공통 응답 처리
- `resp.js`를 통한 ApiRespDto 처리
- 자동 에러 처리
- 성공 시 data 필드만 반환

### API 모듈화
- 도메인별 API 파일 분리
- 재사용 가능한 API 함수
- 타입 안정성 (선택사항)

## 상태 관리

### React Hooks
- `useState`: 컴포넌트 상태 관리
- `useEffect`: 사이드 이펙트 처리
- `useRef`: DOM 참조 및 값 저장

## 스타일링

- CSS 모듈 또는 일반 CSS
- 인라인 스타일 (조건부 스타일링)

## 개발 환경

### 개발 서버
```bash
npm run dev
```
- Vite 개발 서버 실행
- HMR 지원
- 기본 포트: 5173

### 프로덕션 빌드
```bash
npm run build
```
- 최적화된 빌드 생성
- 정적 파일 생성

## 제거된 기술 (Python 워커 불필요 시)

- ❌ 파일 변환 대기 상태 관리
- ❌ 변환 진행률 표시
- ❌ 변환 실패 처리
- ❌ Python 스크립트 호출

## 필수 설정

### 환경 변수 (.env)
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### Vite 설정 (vite.config.js)
```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
})
```

## 성능 최적화

- React Suspense를 통한 코드 스플리팅
- Three.js 리소스 자동 관리
- 이미지/모델 지연 로딩
- 불필요한 리렌더링 방지

## 브라우저 지원

- Chrome/Edge (최신 버전)
- Firefox (최신 버전)
- Safari (최신 버전)
- WebGL 지원 필수
