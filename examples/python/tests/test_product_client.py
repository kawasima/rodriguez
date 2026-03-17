"""
Fault injection tests for ProductClient using Rodriguez.

Requires Rodriguez running on default ports (docker compose up -d).
"""

import sys
import os
import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from src.product_client import ProductClient, ProductApiError

PORTS = {
    "REFUSE_CONNECTION": 10201,
    "NOT_ACCEPT": 10202,
    "RST_AFTER_DELAY": 10203,
    "NEVER_DRAIN": 10204,
    "SLOW_RESPONSE": 10205,
    "CONTENT_TYPE_MISMATCH": 10206,
    "RESPONSE_HEADER_ONLY": 10207,
    "BROKEN_JSON": 10208,
    "ACCEPT_BUT_SILENT": 10209,
    "OVERSIZED_RESPONSE": 10211,
    "REFUSE_AUTH": 10212,
}


def client_for(port, **kwargs):
    timeout = kwargs.pop("timeout", (1, 2))
    return ProductClient(f"http://localhost:{port}", timeout=timeout, **kwargs)


# --- TCP-level faults ---


class TestRefuseConnection:
    def test_throws_connection_error(self):
        client = client_for(PORTS["REFUSE_CONNECTION"], timeout=(1, 1))
        with pytest.raises(ProductApiError, match="Connection failed") as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "CONNECTION_ERROR"


class TestNotAccept:
    def test_throws_connect_timeout(self):
        client = client_for(PORTS["NOT_ACCEPT"], timeout=(1, 1))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code in ("CONNECT_TIMEOUT", "CONNECTION_ERROR")


class TestNoResponseAndSendRST:
    def test_throws_connection_error(self):
        client = client_for(PORTS["RST_AFTER_DELAY"], timeout=(5, 5))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "CONNECTION_ERROR"


class TestNeverDrain:
    def test_throws_timeout_or_connection_error(self):
        client = client_for(PORTS["NEVER_DRAIN"], timeout=(1, 2))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code in ("CONNECT_TIMEOUT", "READ_TIMEOUT", "CONNECTION_ERROR")


class TestAcceptButSilent:
    def test_throws_read_timeout(self):
        client = client_for(PORTS["ACCEPT_BUT_SILENT"], timeout=(1, 2))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "READ_TIMEOUT"


# --- HTTP-level faults ---


class TestSlowResponse:
    """
    Key pitfall: requests' timeout=(connect, read) applies the read timeout
    per chunk, not for the total transfer. A SlowResponse that drips data
    1 byte/second may never trigger ReadTimeout if each byte arrives within
    the read timeout window.
    """

    def test_slow_body_may_not_timeout_with_per_chunk_read_timeout(self):
        # read timeout of 2s per chunk — SlowResponse sends 1 byte/sec,
        # which is within the per-chunk deadline
        client = client_for(PORTS["SLOW_RESPONSE"], timeout=(1, 2))
        # This may NOT raise ReadTimeout because each individual chunk
        # arrives within 2s. This is a key difference from Node.js where
        # AbortSignal.timeout covers the entire request.
        # We just verify the request eventually completes or errors.
        with pytest.raises(ProductApiError):
            client.get_product("1")


class TestResponseHeaderOnly:
    def test_throws_read_timeout(self):
        client = client_for(PORTS["RESPONSE_HEADER_ONLY"], timeout=(1, 2))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "READ_TIMEOUT"


class TestBrokenJson:
    def test_throws_invalid_json(self):
        client = client_for(PORTS["BROKEN_JSON"], timeout=(1, 5))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "INVALID_JSON"


class TestContentTypeMismatch:
    def test_throws_invalid_content_type(self):
        client = client_for(PORTS["CONTENT_TYPE_MISMATCH"], timeout=(1, 5))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "INVALID_CONTENT_TYPE"


class TestOversizedResponse:
    def test_throws_response_too_large(self):
        client = client_for(
            PORTS["OVERSIZED_RESPONSE"],
            timeout=(1, 10),
            max_response_bytes=1024,
        )
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "RESPONSE_TOO_LARGE"


class TestRefuseAuthentication:
    def test_throws_unauthorized(self):
        client = client_for(PORTS["REFUSE_AUTH"], timeout=(1, 5))
        with pytest.raises(ProductApiError) as exc_info:
            client.get_product("1")
        assert exc_info.value.code == "UNAUTHORIZED"
        assert exc_info.value.status == 401
