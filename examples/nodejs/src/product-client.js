export class ProductApiError extends Error {
  constructor(message, { status, code, cause } = {}) {
    super(message, { cause });
    this.name = 'ProductApiError';
    this.status = status;
    this.code = code;
  }
}

export class ProductClient {
  constructor(baseUrl, options = {}) {
    this.baseUrl = baseUrl;
    this.timeoutMs = options.timeoutMs ?? 5000;
    this.maxResponseBytes = options.maxResponseBytes ?? 1024 * 1024;
  }

  async getProduct(id) {
    const response = await this._request(`${this.baseUrl}/products/${id}`);
    return this._parseJson(response);
  }

  async listProducts() {
    const response = await this._request(`${this.baseUrl}/products`);
    return this._parseJson(response);
  }

  async _request(url) {
    let response;
    try {
      response = await fetch(url, {
        signal: AbortSignal.timeout(this.timeoutMs),
        headers: { 'Accept': 'application/json' },
      });
    } catch (err) {
      if (err.name === 'TimeoutError') {
        throw new ProductApiError('Request timed out', {
          code: 'TIMEOUT',
          cause: err,
        });
      }
      throw new ProductApiError(`Connection failed: ${err.message}`, {
        code: 'CONNECTION_ERROR',
        cause: err,
      });
    }

    if (response.status === 401) {
      throw new ProductApiError('Authentication failed', {
        status: 401,
        code: 'UNAUTHORIZED',
      });
    }

    if (!response.ok) {
      throw new ProductApiError(`HTTP ${response.status}`, {
        status: response.status,
        code: 'HTTP_ERROR',
      });
    }

    return response;
  }

  async _parseJson(response) {
    const contentType = response.headers.get('content-type') ?? '';
    const body = await this._readBodyWithLimit(response);

    if (!contentType.includes('application/json')) {
      throw new ProductApiError(
        `Unexpected content type: ${contentType}`,
        { code: 'INVALID_CONTENT_TYPE' }
      );
    }

    try {
      return JSON.parse(body);
    } catch (err) {
      throw new ProductApiError('Invalid JSON in response body', {
        code: 'INVALID_JSON',
        cause: err,
      });
    }
  }

  async _readBodyWithLimit(response) {
    const reader = response.body.getReader();
    const chunks = [];
    let totalBytes = 0;

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        totalBytes += value.byteLength;
        if (totalBytes > this.maxResponseBytes) {
          reader.cancel();
          throw new ProductApiError(
            `Response body exceeds maximum size of ${this.maxResponseBytes} bytes`,
            { code: 'RESPONSE_TOO_LARGE' }
          );
        }
        chunks.push(value);
      }
    } catch (err) {
      if (err instanceof ProductApiError) throw err;
      throw new ProductApiError(`Failed to read response body: ${err.message}`, {
        code: 'BODY_READ_ERROR',
        cause: err,
      });
    }

    const decoder = new TextDecoder();
    return chunks.map(c => decoder.decode(c, { stream: true })).join('') + decoder.decode();
  }
}
