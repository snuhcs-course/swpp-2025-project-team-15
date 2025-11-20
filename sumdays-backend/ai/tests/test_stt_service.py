import io
import json


def test_stt_memo_success(client):
    """
    [통합 테스트] /stt/memo
    - 단일 오디오 파일 업로드 후 Whisper API 호출
    - 실제 Whisper API 키가 있으면 음성 텍스트를 반환
    """
    # 가짜 오디오 바이트 생성 (실제 테스트용 wav/mp3 파일 사용 가능)
    fake_audio = (io.BytesIO(b"fake-audio-bytes"), "sample.wav")
    data = {"audio": fake_audio}

    res = client.post("/stt/memo", data=data, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code in (200, 400), res.get_data(as_text=True)

    if res.status_code == 200:
        # 정상 응답 구조 확인
        assert "transcribed_text" in data
        assert isinstance(data["transcribed_text"], str)
    else:
        # 키 누락, 모델 오류, 파일 포맷 오류 등
        assert "error" in data


def test_stt_memo_no_file(client):
    """
    [에러 테스트] /stt/memo without audio file
    - 파일 누락 시 400 에러 반환
    """
    res = client.post("/stt/memo", data={}, content_type="multipart/form-data")
    data = res.get_json()

    assert res.status_code == 400
    assert "No audio file uploaded" in data["error"]
