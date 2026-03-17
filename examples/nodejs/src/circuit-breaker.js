/**
 * Minimal circuit breaker for demonstration purposes.
 *
 * States:
 *   CLOSED   — requests pass through normally
 *   OPEN     — requests are immediately rejected (fail fast)
 *   HALF_OPEN — one probe request is allowed through to test recovery
 *
 * Transitions:
 *   CLOSED → OPEN:      after `failureThreshold` consecutive failures
 *   OPEN → HALF_OPEN:   after `resetTimeoutMs` elapses
 *   HALF_OPEN → CLOSED: if the probe request succeeds
 *   HALF_OPEN → OPEN:   if the probe request fails
 */
export class CircuitBreaker {
  constructor(fn, { failureThreshold = 3, resetTimeoutMs = 2000 } = {}) {
    this.fn = fn;
    this.failureThreshold = failureThreshold;
    this.resetTimeoutMs = resetTimeoutMs;
    this.state = 'CLOSED';
    this.failureCount = 0;
    this.lastFailureTime = null;
  }

  async call(...args) {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime >= this.resetTimeoutMs) {
        this.state = 'HALF_OPEN';
      } else {
        throw new CircuitOpenError('Circuit breaker is OPEN');
      }
    }

    try {
      const result = await this.fn(...args);
      this._onSuccess();
      return result;
    } catch (err) {
      this._onFailure();
      throw err;
    }
  }

  _onSuccess() {
    this.failureCount = 0;
    this.state = 'CLOSED';
  }

  _onFailure() {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    if (this.failureCount >= this.failureThreshold) {
      this.state = 'OPEN';
    }
  }
}

export class CircuitOpenError extends Error {
  constructor(message) {
    super(message);
    this.name = 'CircuitOpenError';
  }
}
