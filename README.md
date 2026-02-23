# SuggestHelp MVP Monorepo

## Structure
- `frontend`: Next.js App Router UI
- `backend`: Spring Boot API server
- `worker`: FastAPI parsing/AI worker
- `infra`: Docker Compose and infrastructure configs

## Planned E2E
업로드 → 분석 → 조회 → RTM수정 → 나라장터/뉴스 동기화 → XLSX/PPT export


## Local Run

### 1) Start infra/services
```bash
cd infra
docker compose up --build
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

## Quick Test Commands
```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8001/health
```

문서 업로드 테스트(사전조건: projects 테이블에 id=1 데이터 존재):
```bash
curl -X POST http://localhost:8080/api/v1/projects/1/upload \
  -F file=@/path/to/sample.pdf
```
