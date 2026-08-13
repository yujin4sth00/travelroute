# travelroute

도보 중심 여행 동선 최적화 웹 서비스. 사용자가 저장한 장소들을 출발지/도착지 기준으로
효율적인 방문 순서로 배치해준다.

설계 배경과 전체 로드맵은 [docs/design.md](docs/design.md) 참고.

## 기술 스택

- **Backend**: Java 17, Spring Boot 4, Spring Data JPA, Gradle
- **Frontend**: React, Vite
- **DB**: MySQL 8 (Docker)
- **외부 API**: Kakao Local API, Kakao Map SDK, Kakao Walking Route API

## 프로젝트 구조

```
travelroute/
├── backend/     # Spring Boot (Gradle)
├── frontend/    # React + Vite
├── docs/        # 설계 문서
├── docker-compose.yml
└── .env.example
```

## 로컬 개발 환경 설정

### 1. 환경변수 준비

```bash
cp .env.example .env              # docker-compose(mysql, backend)용
cp backend/.env.example backend/.env      # 백엔드를 docker 없이 IDE에서 직접 실행할 때만
cp frontend/.env.example frontend/.env.local
```

각 `.env` 파일을 열어 `KAKAO_REST_API_KEY`, `VITE_KAKAO_JS_KEY` 등 실제 값을 채운다.
`.env*` 파일은 모두 git에 커밋되지 않는다 (`.gitignore` 참고). 저장소에는 `.env.example`만 존재한다.

### 2. MySQL + Backend 실행 (Docker Compose)

```bash
docker compose up --build
```

- MySQL: `localhost:3306`
- Backend: `localhost:8080`

### 3. Frontend 실행 (로컬 dev 서버)

```bash
cd frontend
npm install
npm run dev
```

- Frontend: `localhost:5173`

## API 키 관리 원칙

- **Kakao REST API 키** (Local 검색, 도보 경로): 백엔드만 보유하고 프록시 형태로만 사용. 프론트에 절대 전달하지 않음.
- **Kakao JavaScript 키** (Map SDK): 프론트 `.env.local`의 `VITE_KAKAO_JS_KEY`로 주입. 카카오 개발자 콘솔에서 사용 도메인을 등록해 도용을 방지.
- 모든 키는 `.env` 계열 파일로만 관리하며 코드에 하드코딩하지 않는다.

## 개발 로드맵

Phase 0(현재)부터 Phase 8까지의 단계별 계획은 [docs/design.md](docs/design.md#6-단계별-개발-로드맵) 참고.
