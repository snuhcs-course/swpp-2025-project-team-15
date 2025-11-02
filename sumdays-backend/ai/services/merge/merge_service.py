from typing import Dict, Any
from pydantic import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate
import os

class MergedContentResult(BaseModel):
    """ Represents the result of the memo-mergence """
    merged_content  : str   = Field(description="result of the memo-mergence")


class MemoMerger:
    """ Service that analyzes a diary """
    def __init__(self):
        """ Initialize the Diary analyzing service. """
        self.model = ChatOpenAI(
            model=os.getenv("GPT_MODEL", "gpt-4.1-nano"),
            temperature=0.5
        )

    def merge(self, contents: list[str]) -> Dict[str, Any]:
        """ Merge two memos  """
        if len(contents) < 2:
            raise ValueError("At least two memos are required.")
        else: 
            promt_text = """
            You are helping an app service that completes a single diary by combining memos written during the day. 
            The notes below are arranged in the order that the user wants to put them together. 
            Please combine the notes into a diary naturally. Keep in mind that you are not just concatenating strings.

            Return JSON matching the MergedContentResult schema.

            Additional instructions:
            - Respond in the same language as the memos or the user’s input.
            - Avoid excessive imagination or adding information that is not clearly implied by the memos.
            - Focus on maintaining a coherent, natural, and concise diary tone without inventing new events.
            - Keep the tone natural and personal, suitable for a diary entry. Preserve the original tone, style, and sentence endings of the memos
            ---
            Make merged diary memo for memos: {memos}
            """

            prompt = PromptTemplate.from_template(promt_text)
            llm = self.model.with_structured_output(MergedContentResult)

            chain = prompt | llm
            result = chain.invoke({"memos": "\n".join(f"- {c}" for c in contents)})
            return {"merged_content": result.merged_content}

