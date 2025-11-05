import os, json
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def merge_stream(memos, style_prompt, style_examples):
    if isinstance(style_prompt, dict):
        style_prompt = json.dumps(style_prompt, ensure_ascii=False, indent=2)
    if isinstance(style_examples, list):
        style_examples = "\n\n".join(f"- {s}" for s in style_examples)
    joined_memos = "\n".join(f"- {m}" for m in memos)

    prompt = f"""
    You are a diary writing assistant.

    You will be given:
    - memos: fragmented notes the user wrote today
    - style profile: JSON describing the user's writing tone, phrasing preference, pacing, common expressions
    - style examples: several representative sentences the user has written before

    Your job:
    1) Write in the same tone, sentence flow, and rhythmic pacing as the style profile and examples.
    2) Maintain the natural emotional temperature and subtlety.
    3) Be sure to create your journal in the order in which the notes are given.
    4) Respond **in the same language as the memos or the user’s input.**
    5) Avoid excessive imagination or adding information that is not clearly implied by the memos.
    6) **Do NOT copy, paraphrase, or reuse** specific events, objects, situations, or scenes from the examples.
    7) The examples are **ONLY** for tone / sentence flow / word choice patterns. Their **content must not appear** in the generated diary.
    8) For "common_phrases": 
        - These represent **habitual phrasing TENDENCIES** (e.g., softer endings, reflective pauses).
        - **Do NOT reuse the phrases exactly**. Instead, write **new sentences** that feel like they belong to the same speech rhythm.
    
    Return the diary as **continuous text**, not JSON.

    ---
    STYLE PROFILE (JSON):
    {style_prompt}

    STYLE EXAMPLES:
    {style_examples}

    MEMOS TO MERGE:
    {joined_memos}

    Now write the diary:
    """
    stream = client.chat.completions.create(
            model=os.getenv("GPT_MODEL", "gpt-4.1-nano"),
            stream=True,
            messages=[
                {"role": "system", "content": "You are a diary writing assistant."},
                {"role": "user", "content": prompt}
            ]
        )

    return stream


    # def merge(self, contents: list[str]) -> Dict[str, Any]:
    #     """ Merge two memos  """
    #     if len(contents) < 2:
    #         raise ValueError("At least two memos are required.")
    #     else: 
    #         promt_text = """
    #         You are helping an app service that completes a single diary by combining memos written during the day. 
    #         The notes below are arranged in the order that the user wants to put them together. 
    #         Please combine the notes into a diary naturally. Keep in mind that you are not just concatenating strings.

    #         Return JSON matching the MergedContentResult schema.

    #         Additional instructions:
    #         - Respond in the same language as the memos or the user’s input.
    #         - Avoid excessive imagination or adding information that is not clearly implied by the memos.
    #         - Focus on maintaining a coherent, natural, and concise diary tone without inventing new events.
    #         - Keep the tone natural and personal, suitable for a diary entry. Preserve the original tone, style, and sentence endings of the memos
    #         ---
    #         Make merged diary memo for memos: {memos}
    #         """

    #         prompt = PromptTemplate.from_template(promt_text)
    #         llm = self.model.with_structured_output(MergedContentResult)

    #         chain = prompt | llm
    #         result = chain.invoke({"memos": "\n".join(f"- {c}" for c in contents)})
    #         return {"merged_content": result.merged_content}