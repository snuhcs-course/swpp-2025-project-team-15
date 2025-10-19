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
        self.model = os.getenv("GPT_MODEL", "gpt-5-nano")

    def merge(self, contents: list[str]) -> Dict[str, Any]:
        """ Merge two memos  """
        if len(contents) < 2:
            raise ValueError("At least two memos are required.")
        else: 
            promt_text = """
            You are helping an app service that completes a single diary by combining memos written during the day. 
            The notes below are arranged in the order that the user wants to put them together. 
            Please combine the notes into a diary naturally. Keep in mind that you are not just concating strings.

            Return JSON matching the MergedContentResult schema.
            ---
            Make merged diary memo for memos: {memos}
            """

            prompt = PromptTemplate.from_template(promt_text)
            llm = ChatOpenAI(model=self.model).with_structured_output(MergedContentResult)

            chain = prompt | llm
            result = chain.invoke({"memos": "\n".join(f"- {c}" for c in contents)})
            return {"merged_content": result.merged_content}

