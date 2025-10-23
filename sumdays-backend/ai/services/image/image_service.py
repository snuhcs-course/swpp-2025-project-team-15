from langchain_openai import ChatOpenAI
from langchain.schema import HumanMessage
from google.cloud import vision
import os
import base64

os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")

class ImageService:
    """ Service for image memos or diaries """
    def __init__(self):
        self.model = ChatOpenAI(
            model=os.getenv("IMAGE_MODEL", "gpt-4o-mini"),
            temperature=0.3
        )
        self.ocr_client = vision.ImageAnnotatorClient()

    def analyze(self, image_file, analysis_type: str):
        """ Extract image descriptions or text depending on the analysis type """
        if analysis_type == "extract":
            image_bytes = image_file.read()
            image = vision.Image(content=image_bytes)

            response = self.ocr_client.document_text_detection(image=image)
            texts = response.text_annotations

            if not texts:
                return {
                    "type": "extract",
                    "text": ""
                }

            if response.error.message:
                raise Exception(f"Vision API Error: {response.error.message}")
            
            return {
                    "type": analysis_type,
                    "text": texts[0].description.strip()
                }
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
        
        else:
            raise ValueError(f"Unsupported analysis type: {analysis_type}")

