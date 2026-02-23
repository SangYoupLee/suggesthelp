# SuggestHelp MVP Monorepo

## Structure
- `frontend`: Next.js App Router UI
- `backend`: Spring Boot API server
- `worker`: FastAPI parsing/AI worker
- `infra`: Docker Compose and infrastructure configs

## Planned E2E
업로드 → 분석 → 조회 → RTM수정 → 나라장터/뉴스 동기화 → XLSX/PPT export

## Environment
1) `.env.example`를 복사해 `.env`로 사용하거나, 기본값으로 실행합니다.
```bash
cp .env.example .env
```

## Local Run

### 1) Start infra/services
```bash
cd infra
docker compose --env-file ../.env up --build
```

### 2) Backend only (local)
```bash
cd backend
mvn spring-boot:run
```

### 3) Worker only (local)
```bash
cd worker
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

### 4) Frontend only (local)
```bash
cd frontend
npm install
npm run dev
```

## Flyway / DDL 확인
- backend 기동 시 Flyway가 `V1__init.sql`을 자동 적용합니다.
- 확인 SQL:
```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

## Quick Test Commands
```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8001/health
```

### 1) 프로젝트 생성
```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"조달 제안 프로젝트","clientOrg":"조달청","budget":1000000000,"dueDate":"2026-12-31"}'
```

### 2) 문서 업로드 (project id=1 예시)
```bash
curl -X POST http://localhost:8080/api/v1/projects/1/upload \
  -F file=@/path/to/sample.pdf
```

### 3) 프로젝트 상태 조회
```bash
curl http://localhost:8080/api/v1/projects/1/status
```
