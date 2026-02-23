CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    client_org VARCHAR(255),
    budget BIGINT,
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    object_key VARCHAR(500) NOT NULL,
    sha256 CHAR(64) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE analysis_runs (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    model_name VARCHAR(100),
    prompt_version VARCHAR(50) NOT NULL,
    analysis_schema_version VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE eval_items (
    id BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    item_name TEXT NOT NULL,
    score NUMERIC(5,2),
    criteria TEXT,
    source_page INTEGER NOT NULL,
    source_snippet TEXT NOT NULL,
    confidence NUMERIC(5,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE requirements (
    id BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    requirement_text TEXT NOT NULL,
    category VARCHAR(100),
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    source_page INTEGER NOT NULL,
    source_snippet TEXT NOT NULL,
    confidence NUMERIC(5,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rtm_mapping (
    id BIGSERIAL PRIMARY KEY,
    requirement_id BIGINT NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    response_strategy TEXT,
    evidence TEXT,
    owner VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bid_notices (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    notice_no VARCHAR(100) NOT NULL,
    notice_type VARCHAR(50),
    payload_json JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE external_news (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    source VARCHAR(100),
    summary TEXT,
    relevance_score NUMERIC(5,4),
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE artifacts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id BIGINT NULL,
    artifact_type VARCHAR(100) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_eval_items_analysis_run ON eval_items (analysis_run_id);
CREATE INDEX idx_requirements_analysis_run ON requirements (analysis_run_id);
