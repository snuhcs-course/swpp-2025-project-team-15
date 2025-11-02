import os
import io
from openai import OpenAI

class SpeechToTextService:
    """ Service for converting audio to text using Whisper API. """
    def __init__(self):
        self.client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.model = os.getenv("AUDIO_MODEL", "whisper-1")

    def transcribe(self, audio_file, language: str = "ko") -> str:
        """ Transcribe an audio file using OpenAI Whisper API. """
        audio_data = audio_file.read()
        buffer = io.BytesIO(audio_data)
        buffer.name = "file.mp3"

        result = self.client.audio.transcriptions.create(
            model=self.model,
            file=buffer,
            language=language,
        )
        return result.text
