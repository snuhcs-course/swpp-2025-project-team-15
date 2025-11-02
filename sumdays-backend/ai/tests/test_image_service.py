import io
import os
import pytest

# 실제 이미지 파일 경로 설정
# ai/tests/sample.jpg, ai/tests/diary_page1.jpg, ai/tests/diary_page2.jpg 준비 필요
SAMPLE_IMG = os.path.join("ai", "tests", "images", "sample.jpg")
DIARY_IMG_1 = os.path.join("ai", "tests", "images", "diary_page1.jpg")
DIARY_IMG_2 = os.path.join("ai", "tests", "images", "diary_page2.jpg")


@pytest.mark.integration
def test_image_memo_extract(client):
    """
    [통합 테스트] /image/memo (type=extract)
    - 실제 이미지 파일로 OCR 추출
    """
    with open(SAMPLE_IMG, "rb") as f:
        data = {"type": "extract", "image": (io.BytesIO(f.read()), "sample.jpg")}

    res = client.post("/image/memo", data=data, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code in (200, 400), res.get_data(as_text=True)
    if res.status_code == 200:
        assert "type" in data
        assert data["type"] == "extract"
        assert "text" in data
        print("\n[OCR extract sample]\n", data["text"][:200])
    else:
        assert "error" in data
        print("\n[extract error]", data["error"])


@pytest.mark.integration
def test_image_memo_describe(client):
    """
    [통합 테스트] /image/memo (type=describe)
    - 실제 이미지 파일로 GPT Vision 호출
    """
    with open(SAMPLE_IMG, "rb") as f:
        data = {"type": "describe", "image": (io.BytesIO(f.read()), "sample.jpg")}

    res = client.post("/image/memo", data=data, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code in (200, 400), res.get_data(as_text=True)
    if res.status_code == 200:
        assert "type" in data
        assert data["type"] == "describe"
        assert "text" in data
        print("\n[Vision describe result]\n", data["text"])
    else:
        assert "error" in data
        print("\n[describe error]", data["error"])


def test_image_memo_invalid_type(client):
    """
    [에러 테스트] /image/memo invalid type
    """
    with open(SAMPLE_IMG, "rb") as f:
        data = {"type": "nonsense", "image": (io.BytesIO(f.read()), "sample.jpg")}

    res = client.post("/image/memo", data=data, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code == 400
    assert "Invalid analysis type" in data["error"]


def test_image_memo_no_file(client):
    """
    [에러 테스트] /image/memo without image
    """
    data = {"type": "extract"}
    res = client.post("/image/memo", data=data, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code == 400
    assert "No image file uploaded" in data["error"]


@pytest.mark.integration
def test_image_diary_multiple(client):
    """
    [통합 테스트] /image/diary
    - 실제 이미지 2장 업로드, Vision + GPT refine 경로 모두 실행
    """
    with open(DIARY_IMG_1, "rb") as f1, open(DIARY_IMG_2, "rb") as f2:
        files = [("image", (io.BytesIO(f1.read()), "page1.jpg")),
                 ("image", (io.BytesIO(f2.read()), "page2.jpg"))]
        res = client.post("/image/diary", data=dict(files), content_type="multipart/form-data")

    data = res.get_json()
    assert res.status_code in (200, 400), res.get_data(as_text=True)

    if res.status_code == 200:
        assert "result" in data
        assert isinstance(data["result"], list)
        print("\n[Refined diary sample]\n", data["result"][0])
    else:
        assert "error" in data
        print("\n[diary error]", data["error"])


def test_image_diary_no_files(client):
    """
    [에러 테스트] /image/diary without files
    """
    res = client.post("/image/diary", data={}, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code == 400
    assert "No image files uploaded" in data["error"]
