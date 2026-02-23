from fastapi import FastAPI

app = FastAPI(title="suggesthelp-worker")


@app.get("/health")
def health():
    return {"status": "ok"}
