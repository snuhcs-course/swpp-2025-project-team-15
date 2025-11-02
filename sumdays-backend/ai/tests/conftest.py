import os
from dotenv import load_dotenv
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "..", "..", ".env"))

import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

import pytest
from unittest.mock import MagicMock
from ai.app import app



@pytest.fixture(scope="session", autouse=True)
def load_env():
    """
    .env 파일을 명시적으로 로드 (테스트 실행 시 환경 변수 누락 방지)
    """
    env_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
    if os.path.exists(env_path):
        load_dotenv(dotenv_path=env_path)
    else:
        print("[WARN] .env 파일을 찾을 수 없습니다:", env_path)
    # 기본 포트 설정 (테스트 환경에서 누락되더라도 안전하게)
    os.environ.setdefault("AI_SERVER_PORT", "5001")


@pytest.fixture
def client():
    """
    Flask 애플리케이션의 테스트 클라이언트를 반환합니다.
    - 실제 app.py를 import하여 모든 blueprint를 등록한 상태로 실행됩니다.
    - 내부적으로 요청 시 실제 OpenAI/Google Vision API가 호출됩니다.
    """
    app.config["TESTING"] = True
    app.config["WTF_CSRF_ENABLED"] = False
    return app.test_client()
