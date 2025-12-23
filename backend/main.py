from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import openai
import os

app = FastAPI()

# Mock relationship contexts for MVP
CONTEXTS = {
    "sarah_tinder": "Relationship: Flirty conversations on Tinder, playful banter, mutual attraction.",
    "boss_work": "Relationship: Professional colleague, respectful communication, focus on career advancement.",
    "friend_casual": "Relationship: Close friend, casual and relaxed, inside jokes and light-hearted.",
}

class GenerateReplyRequest(BaseModel):
    clipboard_text: str
    context_id: str
    vibe_type: str  # e.g., "roast", "professional", "flirty", "defuse", "ghost", "custom"
    custom_prompt: str = None
    modifiers: list[str] = []  # e.g., ["shorter", "add_emoji"]

class ReplyOption(BaseModel):
    text: str
    index: int

class GenerateReplyResponse(BaseModel):
    replies: list[ReplyOption]

@app.post("/generate_reply", response_model=GenerateReplyResponse)
async def generate_reply(request: GenerateReplyRequest):
    # Retrieve context
    context = CONTEXTS.get(request.context_id, "Relationship: General context, neutral tone.")

    # Construct system prompt
    vibe_instruction = ""
    if request.vibe_type == "custom" and request.custom_prompt:
        vibe_instruction = f"Respond in the style: {request.custom_prompt}"
    elif request.vibe_type == "roast":
        vibe_instruction = "Craft a witty, teasing roast reply."
    elif request.vibe_type == "professional":
        vibe_instruction = "Respond professionally and politely."
    elif request.vibe_type == "flirty":
        vibe_instruction = "Respond flirtatiously and playfully."
    elif request.vibe_type == "defuse":
        vibe_instruction = "Respond calmly to de-escalate the situation."
    elif request.vibe_type == "ghost":
        vibe_instruction = "Provide a subtle way to end the conversation."
    else:
        vibe_instruction = "Provide a neutral reply."

    modifiers_str = ", ".join(request.modifiers) if request.modifiers else "none"
    system_prompt = f"""
You are a communication assistant. Based on the relationship context: {context}
Craft a reply to: "{request.clipboard_text}"
Style: {vibe_instruction}
Modifiers: {modifiers_str}
Keep replies under 15 words. Generate 3 distinct reply options.
Return as a JSON array of objects with 'text' and 'index' fields.
"""

    try:
        client = openai.OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": "Generate replies."}
            ],
            max_tokens=200
        )
        # Parse the response (assuming GPT returns JSON)
        import json
        replies_data = json.loads(response.choices[0].message.content.strip())
        replies = [ReplyOption(text=rep["text"], index=rep["index"]) for rep in replies_data]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI generation failed: {str(e)}")

    return GenerateReplyResponse(replies=replies)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
