import requests


class ProductApiError(Exception):
    def __init__(self, message, *, code=None, status=None):
        super().__init__(message)
        self.code = code
        self.status = status


class ProductClient:
    def __init__(self, base_url, *, timeout=(5, 5), max_response_bytes=1024 * 1024):
        """
        Args:
            base_url: Base URL of the product API.
            timeout: (connect_timeout, read_timeout) in seconds.
            max_response_bytes: Maximum response body size in bytes.
        """
        self.base_url = base_url
        self.timeout = timeout
        self.max_response_bytes = max_response_bytes

    def get_product(self, product_id):
        return self._request(f"{self.base_url}/products/{product_id}")

    def list_products(self):
        return self._request(f"{self.base_url}/products")

    def _request(self, url):
        try:
            response = requests.get(
                url,
                timeout=self.timeout,
                headers={"Accept": "application/json"},
                stream=True,
            )
        except requests.exceptions.ConnectTimeout:
            raise ProductApiError("Connection timed out", code="CONNECT_TIMEOUT")
        except requests.exceptions.ReadTimeout:
            raise ProductApiError("Read timed out", code="READ_TIMEOUT")
        except requests.exceptions.ConnectionError as e:
            raise ProductApiError(f"Connection failed: {e}", code="CONNECTION_ERROR")

        if response.status_code == 401:
            raise ProductApiError("Authentication failed", code="UNAUTHORIZED", status=401)

        if not response.ok:
            raise ProductApiError(f"HTTP {response.status_code}", code="HTTP_ERROR", status=response.status_code)

        content_type = response.headers.get("content-type", "")
        body = self._read_body_with_limit(response)

        if "application/json" not in content_type:
            raise ProductApiError(
                f"Unexpected content type: {content_type}", code="INVALID_CONTENT_TYPE"
            )

        try:
            return response.json()
        except ValueError:
            raise ProductApiError("Invalid JSON in response body", code="INVALID_JSON")

    def _read_body_with_limit(self, response):
        total = 0
        chunks = []
        for chunk in response.iter_content(chunk_size=4096):
            total += len(chunk)
            if total > self.max_response_bytes:
                response.close()
                raise ProductApiError(
                    f"Response body exceeds {self.max_response_bytes} bytes",
                    code="RESPONSE_TOO_LARGE",
                )
            chunks.append(chunk)
        return b"".join(chunks)
