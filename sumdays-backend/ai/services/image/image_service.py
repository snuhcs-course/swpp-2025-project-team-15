from langchain_openai import ChatOpenAI
from langchain.schema import HumanMessage
import os
import base64

class ImageService:
    """ Service for image memos or diaries """
    def __init__(self):
        self.model = ChatOpenAI(
            model=os.getenv("IMAGE_MODEL", "gpt-4o-mini"),
            temperature=0.3
        )

    def analyze(self, image_file, analysis_type: str):
        """ Extract image descriptions or text depending on the analysis type """
        if analysis_type == "extract":
            prompt = """
            Extract all readable text from this image accurately.
            Do not add any explanations, introductions, or quotes.
            Output only the plain extracted text exactly as it appears in the image.
            """
        elif analysis_type == "describe":
            prompt = """
            You are helping an app that analyzes a single diary image input.
            Your task is to describe what kind of moment or situation the image represents,
            as if it were a short diary note.

            Guidelines:
            - Write in a sinle sentence.
            - Focus on the *context or emotion* implied by the image rather than listing visible objects.
            (e.g., “listening to music” instead of “an audio player on a desk”)
            - Avoid excessive imagination or details not clearly implied by the image.
            """
        else:
            raise ValueError(f"Unsupported analysis type: {analysis_type}")

        image_bytes = image_file.read()
        image_b64 = base64.b64encode(image_bytes).decode("utf-8")
        data_uri = f"data:image/jpeg;base64,{image_b64}"

        response = self.model.invoke([
            HumanMessage(content=[
                {"type": "text", "text": prompt},
                {"type": "image_url", "image_url": {'url': data_uri}}
            ])
        ])

        result_text = response.content.strip()

        return {
            "type": analysis_type,
            "text": result_text
        }
