from fastapi import FastAPI
from models.Gemini import init_genimi
from datetime import datetime


app = FastAPI()
gemini_bot = init_genimi()


@app.post("/api/ai/{room_number}/")
def create_message(room_number: int, message: dict):
    print(message)
    # AI_message = gemini_bot.generate_content(message)
    AI_message = "hello!!!"
    precise_timestamp = datetime.now().isoformat()
    event_log = "AI 대답 생성 완료"
    return {
        "timeStamp": f"{precise_timestamp} - {event_log}",
        "AI_code" : 1,
        "status": 200,
        "room_number": room_number,
        "message": AI_message,
    }
