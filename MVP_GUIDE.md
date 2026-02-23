# 제안 특화 플랫폼: 개인용 MVP 통합 개발 명세서 (개발/유지보수 중심)

## 0. 문서 목적
본 문서는 **공공 RFP 분석 및 제안 전략 자동화 MVP**를 실제로 구현·운영 가능한 수준으로 정의합니다.  
핵심 원칙은 다음 3가지입니다.
- **빠른 출시**: 개인용 MVP를 8주 내 완성 가능한 범위로 제한
- **유지보수성**: 서비스 경계, 데이터 계약, 운영 기준을 명확히 문서화
- **확장 준비**: 추후 팀 기능(RBAC/협업/승인)으로 무리 없이 확장

---

## 1. MVP 범위 정의

### 1.1 포함 범위 (Must)
1. 문서 업로드: PDF/DOCX/HWP
2. 자동 분석: 평가 항목, 배점, 제출 조건, 필수 인증/리스크 추출
3. RTM 자동 초안 생성 + 사용자 수정
4. 나라장터 공고 데이터 연동(목록/상세)
5. 뉴스·정책 근거 수집 및 요약
6. 산출물 다운로드: XLSX(RTM/배점대응/WBS), PPT 아웃라인(JSON/MD)

### 1.2 제외 범위 (Not in MVP)
- 조직 단위 사용자/권한 체계(RBAC)
- 실시간 협업(댓글/동시편집), 승인 워크플로우
- 완성형 PPT 렌더링(템플릿 자동 디자인 반영)

### 1.3 성공 기준
- 200p 내외 문서 기준 분석 파이프라인 완료
- 핵심 추출 결과를 UI에서 검토/수정 가능
- 산출물(XLSX, PPT 아웃라인) 즉시 다운로드 가능

---

## 2. 시스템 아키텍처 (Hybrid Tech Stack)

### 2.1 구성
- **Frontend**: Next.js(App Router) + Tailwind + shadcn/ui
- **Main API**: Spring Boot 3.x (도메인 로직, 트랜잭션, API)
- **AI Worker**: FastAPI (파싱/OCR/LLM 오케스트레이션)
- **DB**: PostgreSQL + pgvector
- **Storage**: S3(운영) / MinIO(개발)
- **배포**: Docker Compose(개발/MVP 운영)

### 2.2 서비스 경계(유지보수 핵심)
- Spring Boot: 프로젝트/문서/결과 조회/산출물 메타 관리의 **단일 진입점**
- FastAPI: CPU/GPU 작업(파싱/OCR/LLM)의 **비동기 워커 전담**
- 두 서비스 간 계약은 **버전 있는 JSON 스키마**로 고정 (`analysis_schema_version`)

### 2.3 권장 처리 흐름
1. 사용자가 문서 업로드
2. Spring이 파일 저장(S3/MinIO), `documents` 레코드 생성
3. Spring이 Worker 분석 작업 요청
4. Worker가 파싱/추출/검증 후 결과 저장(또는 Spring에 콜백)
5. 사용자가 상태 조회(Polling, 필요 시 SSE)

---

## 3. 데이터 모델 (운영 가능한 최소 스키마)

## 3.1 테이블
- `projects`: 사업명, 발주처, 예산, 마감일, 상태
- `documents`: 프로젝트ID, 파일경로, 파일타입, SHA-256, 파싱상태
- `analysis_runs`: 문서ID, 모델명, 프롬프트버전, 실행상태, 실행시간
- `eval_items`: 평가항목, 배점, 기준, 원문근거(page/snippet), confidence
- `requirements`: 요구사항 본문, 카테고리, 필수여부, 원문근거
- `rtm_mapping`: requirement_id, 대응전략, 증빙자료, 담당, 상태
- `bid_notices`: 나라장터 공고번호, 유형, 상세 JSON, 동기화시각
- `external_news`: 제목, URL, 출처, 수집시각, 요약, 관련도 점수
- `artifacts`: 산출물 유형, 파일경로, 생성버전, 생성시각

### 3.2 인덱스/제약
- `documents.sha256` 유니크 (중복 업로드 방지)
- `eval_items(project_id, score)` 인덱스
- `requirements(project_id, category)` 인덱스
- 모든 추출 엔터티에 `source_page`, `source_snippet`, `confidence` 필수

### 3.3 확장 대비 컬럼
- 모든 주요 테이블에 `user_id` nullable 컬럼 선반영
- `created_at/updated_at`, `created_by` 공통 메타 필수

---

## 4. 핵심 파이프라인 상세

### 4.1 문서 파싱 (FastAPI)
- PDF: `pdfplumber`
- DOCX: `python-docx`
- HWP: `hwp5-html` 또는 사내 표준 변환 모듈
- 스캔 PDF: `Tesseract OCR` + 후처리
- 표 인식: 룰 기반 우선, 복잡 표는 LlamaParse 옵션 처리

### 4.2 전처리 규칙
- 헤더/푸터 제거
- 장/절/항 번호 정규화
- 깨진 줄바꿈/특수문자 정리
- 중복 문단 제거

### 4.3 추출 전략 (Rule + LLM)
1. 규칙 기반 후보 탐색: `배점`, `평가항목`, `제출서류`, `필수`, `가점`
2. LLM 구조화 추출: JSON 스키마 강제
3. 검증 단계:
   - 배점 총합 검증(100점 여부)
   - 필수 제출항목 누락 탐지
   - confidence 낮은 항목 재질의

### 4.4 RTM 생성
- 요구사항 원자 분해
- 카테고리 분류(기능/비기능/컴플라이언스/행정)
- 대응전략·증빙·담당·우선순위 자동 초안
- UI에서 사용자 확정(자동값은 `draft=true`로 저장)

---

## 5. API 명세 (MVP 최소)

### 5.1 프로젝트/문서
- `POST /api/v1/projects`
- `GET /api/v1/projects/{id}`
- `POST /api/v1/projects/{id}/upload`
- `GET /api/v1/projects/{id}/status`

### 5.2 분석/전략
- `GET /api/v1/projects/{id}/analysis`
- `POST /api/v1/projects/{id}/strategy`
- `PATCH /api/v1/projects/{id}/rtm/{mappingId}` (사용자 수정)

### 5.3 외부연동/산출물
- `POST /api/v1/projects/{id}/sync/bid-notices`
- `POST /api/v1/projects/{id}/sync/news`
- `GET /api/v1/projects/{id}/export/xlsx`
- `GET /api/v1/projects/{id}/export/ppt-outline`

---

## 6. 외부 데이터 연동 운영 기준

### 6.1 나라장터 API
- 수집 대상: 공고목록/상세, 기초금액, 면허제한, 지역제한
- 동기화 정책: **6시간 캐시 + 변경일시 기반 증분 수집**
- 실패 대응: 재시도(지수 백오프), 실패 로그 저장

### 6.2 뉴스/정책
- 쿼리: `(사업명 + 기관명 + 정책키워드)`
- 상위 5건만 요약(관련도 기준)
- 출처 URL, 수집시각, 요약모델 버전 필수 저장

---

## 7. 산출물 사양

### 7.1 XLSX (필수 시트 3종)
1. RTM
2. 배점 대응표
3. WBS

### 7.2 PPT 아웃라인
- 포맷: JSON 또는 Markdown
- 필드: `slide_no`, `title`, `key_message`, `evidence_refs`, `visual_hint`

### 7.3 인포그래픽 스펙 (선택)
- MVP에서는 JSON 사양만 제공(렌더링 엔진 미포함)

---

## 8. 유지보수 관점의 필수 운영 설계

### 8.1 관측성(Observability)
- 구조화 로그(JSON): `project_id`, `document_id`, `analysis_run_id` 포함
- 메트릭:
  - 분석 소요시간
  - 추출 실패율
  - 재시도 횟수
  - 외부 API 오류율
- 에러 추적: Sentry 등 1개 도구 표준화

### 8.2 장애 대응
- 분석 작업 타임아웃 명시(예: 문서당 15분)
- Dead-letter 성격의 실패 큐(또는 실패 테이블) 운영
- 산출물 생성 실패 시 부분 성공 상태 제공

### 8.3 버전/호환성
- 프롬프트 버전 관리(`prompt_version`)
- 응답 스키마 버전 관리(`analysis_schema_version`)
- 모델 변경 시 A/B 비교 로그 1주 이상 보관

### 8.4 테스트 전략
- 단위 테스트: 파서, 스키마 검증, 배점합 로직
- 통합 테스트: 업로드→분석→조회→엑셀 생성 E2E
- 회귀 테스트셋: 대표 RFP 샘플 5종 이상 고정

---

## 9. 보안/컴플라이언스 체크리스트

- [ ] 저장 데이터 암호화(S3 SSE, DB at-rest)
- [ ] 전송 TLS 적용
- [ ] 외부 LLM 전송 전 민감정보 마스킹
- [ ] 접근/수정 감사로그 보관
- [ ] 나라장터/뉴스 API 약관 준수 문서화

---

## 10. 8주 구현 로드맵 (집중형)

- **1~2주**: 인프라, DB 스키마, 업로드 API, Worker 골격
- **3~4주**: 파싱/OCR/규칙추출 + LLM JSON 추출
- **5~6주**: 분석 UI 탭, RTM 편집, 나라장터/뉴스 연동
- **7주**: XLSX/PPT 아웃라인 생성, 오류처리/재시도
- **8주**: 정확도 개선, 회귀 테스트, 운영 문서화

---

## 11. 최종 MVP 수용 기준 (Definition of Done)
- [ ] PDF/DOCX/HWP 업로드 및 분석 완료
- [ ] 평가항목/배점/요구사항 추출 및 근거 페이지 표시
- [ ] RTM 사용자 수정 및 저장 가능
- [ ] 나라장터 동기화 성공(캐시 포함)
- [ ] 뉴스 5건 요약 및 출처 표시
- [ ] XLSX/PPT 아웃라인 다운로드 가능
- [ ] 실패 재시도/로그 추적 가능
- [ ] 핵심 E2E 테스트 통과

---

## 12. 팀 확장 시 우선순위
1. 인증/권한(RBAC)
2. 협업 코멘트/할당/상태관리
3. 승인 워크플로우
4. 버전 비교 및 변경 이력 시각화

이 명세는 “개발 속도”와 “운영 유지보수성” 사이의 균형을 목표로 하며, 개인용 MVP를 안정적으로 출시한 뒤 팀용 제품으로 확장하기 위한 기준 문서입니다.
