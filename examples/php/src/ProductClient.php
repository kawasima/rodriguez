<?php

declare(strict_types=1);

namespace Rodriguez\Example;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\ConnectException;
use GuzzleHttp\Exception\RequestException;
use GuzzleHttp\Exception\TransferException;

class ProductApiError extends \RuntimeException
{
    public readonly string $errorCode;
    public readonly ?int $status;

    public function __construct(
        string $errorCode,
        string $message,
        ?int $status = null,
        ?\Throwable $previous = null,
    ) {
        parent::__construct($message, 0, $previous);
        $this->errorCode = $errorCode;
        $this->status = $status;
    }
}

class ProductClient
{
    private Client $client;
    private float $timeoutSec;
    private int $maxResponseBytes;

    public function __construct(
        string $baseUrl,
        float $timeoutSec = 5.0,
        float $connectTimeoutSec = 3.0,
        int $maxResponseBytes = 5 * 1024 * 1024,
    ) {
        $this->timeoutSec = $timeoutSec;
        $this->maxResponseBytes = $maxResponseBytes;
        $this->client = new Client([
            'base_uri' => $baseUrl,
            'timeout' => $timeoutSec,
            'connect_timeout' => $connectTimeoutSec,
            'http_errors' => false,
        ]);
    }

    /**
     * @return array<string, mixed>
     * @throws ProductApiError
     */
    public function getProduct(string $id): array
    {
        try {
            $response = $this->client->get("/products/{$id}", [
                'headers' => ['Accept' => 'application/json'],
            ]);
        } catch (ConnectException $e) {
            $message = $e->getMessage();
            if (str_contains($message, 'timed out') || str_contains($message, 'Connection timed out')) {
                throw new ProductApiError('TIMEOUT', $message, previous: $e);
            }
            throw new ProductApiError('CONNECTION_ERROR', $message, previous: $e);
        } catch (TransferException $e) {
            $message = $e->getMessage();
            if (str_contains($message, 'timed out') || str_contains($message, 'Timeout')) {
                throw new ProductApiError('TIMEOUT', $message, previous: $e);
            }
            throw new ProductApiError('CONNECTION_ERROR', $message, previous: $e);
        }

        $statusCode = $response->getStatusCode();

        if ($statusCode === 401) {
            throw new ProductApiError('UNAUTHORIZED', 'Unauthorized', status: 401);
        }

        if ($statusCode >= 400) {
            throw new ProductApiError(
                'HTTP_ERROR',
                "HTTP {$statusCode}",
                status: $statusCode,
            );
        }

        // Read body with size limit — stream to avoid loading entire oversized response into memory.
        $stream = $response->getBody();
        $body = '';
        while (!$stream->eof()) {
            $chunk = $stream->read(8192);
            $body .= $chunk;
            if (strlen($body) > $this->maxResponseBytes) {
                $stream->close();
                throw new ProductApiError(
                    'RESPONSE_TOO_LARGE',
                    "Response body exceeds limit {$this->maxResponseBytes} bytes",
                );
            }
        }

        $data = json_decode($body, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new ProductApiError(
                'INVALID_JSON',
                'Invalid JSON: ' . json_last_error_msg(),
            );
        }

        return $data;
    }
}
