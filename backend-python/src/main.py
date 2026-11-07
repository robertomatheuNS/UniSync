from fastapi import FastAPI

app = FastAPI(
    title="UniSync",
    description="API responsável pelo gerenciamento de oportunidades acadêmicas.",
    version="0.1.0"
)


@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "ok"}