# Backend 기술 스택

Python 워커를 통한 CAD 파일 변환 작업이 필요 없는 경우의 백엔드 기술 스택입니다.

## 핵심 프레임워크

### Spring Boot
- **버전**: 4.0.2
- **용도**: 메인 애플리케이션 프레임워크
- **주요 기능**:
  - RESTful API 서버
  - 의존성 주입 (DI)
  - 트랜잭션 관리
  - 비동기 처리

### Java
- **버전**: 17 (LTS)
- **용도**: 백엔드 개발 언어

## 데이터베이스

### MySQL
- **용도**: 관계형 데이터베이스
- **저장 데이터**:
  - CAD 파일 메타데이터
  - 부품 정보
  - 부품 메모
- **연결**: MySQL Connector/J

### MyBatis
- **버전**: 4.0.1
- **용도**: SQL 매퍼 프레임워크
- **기능**:
  - XML 기반 SQL 매핑
  - 동적 SQL 지원
  - 자동 매핑 (underscore ↔ camelCase)

## 보안

### Spring Security
- **용도**: 인증 및 인가
- **기능**:
  - CORS 설정
  - CSRF 보호
  - 요청 인가 관리

## 데이터 처리

### Jackson
- **용도**: JSON 직렬화/역직렬화
- **기능**:
  - 객체 ↔ JSON 변환
  - API 요청/응답 처리

### Commons IO
- **버전**: 2.15.1
- **용도**: 파일 입출력 처리
- **기능**:
  - 파일 업로드/다운로드
  - 파일 시스템 작업

## API 문서화

### SpringDoc OpenAPI
- **버전**: 3.0.1
- **용도**: API 문서 자동 생성
- **기능**:
  - Swagger UI 제공
  - API 스펙 문서화

## 개발 도구

### Lombok
- **용도**: 보일러플레이트 코드 제거
- **기능**:
  - @Data, @RequiredArgsConstructor 등
  - Getter/Setter 자동 생성

### Spring Boot DevTools
- **용도**: 개발 편의성 향상
- **기능**:
  - 자동 재시작
  - 라이브 리로드

## 빌드 도구

### Maven
- **용도**: 프로젝트 빌드 및 의존성 관리
- **주요 플러그인**:
  - Spring Boot Maven Plugin
  - Maven Compiler Plugin

## 주요 기능 모듈

### 파일 관리
- GLB 파일 업로드/다운로드
- 파일 메타데이터 관리
- 파일 저장 경로 관리

### 부품 관리
- 부품 정보 조회
- 부품 메모 CRUD
- 부품 이름 변경

### API 응답 표준화
- ApiRespDto를 통한 일관된 응답 형식
- 에러 처리 및 메시지 관리

## 제거된 기술 (Python 워커 불필요 시)

- ❌ Python 실행 환경
- ❌ Python 스크립트 실행
- ❌ 파일 변환 프로세스
- ❌ 비동기 파일 처리 워커

## 필수 설정

### application.properties
```properties
# 데이터베이스 설정
spring.datasource.url=jdbc:mysql://localhost:3306/mvp_404f
spring.datasource.username=root
spring.datasource.password=root

# 파일 업로드 설정
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# 파일 저장 경로
file.upload.temp-dir=temp/upload
file.upload.converted-dir=temp/converted
```

## 아키텍처 패턴

- **Controller**: REST API 엔드포인트, 서비스 호출만 담당
- **Service**: 비즈니스 로직 처리, ApiRespDto 반환
- **Mapper**: 데이터베이스 접근 (MyBatis)
- **DTO**: 데이터 전송 객체
- **Entity**: 데이터베이스 엔티티
