import numpy as np
from openai import OpenAI

client = OpenAI()

def embed(text):
    r = client.embeddings.create(model="text-embedding-3-small", input=text)
    return np.array(r.data[0].embedding, float)

def split_paragraphs(diaries):
    paras = []
    for d in diaries:
        for p in d.split("\n"):
            p = p.strip()
            if len(p) >= 8:  # 너무 짧은거 제거
                paras.append(p)
    return paras

def extract_style_examples(diaries, n=3):
    paras = split_paragraphs(diaries)

    # 임베딩 생성
    vecs = np.array([embed(p) for p in paras])

    # 평균 중심 찾기 (문체의 '대표점')
    center = np.mean(vecs, axis=0)

    # 중심과 가장 가까운 문단 상위 n개 선택
    sims = [np.dot(v, center) / (np.linalg.norm(v)*np.linalg.norm(center) + 1e-12) for v in vecs]
    idx = np.argsort(sims)[-n:]  # similarity 높은 것 추출

    chosen = [paras[i] for i in idx]

    return "\n\n[사용자 말투 예문]\n\n" + "\n\n".join(chosen)
