<?php

declare(strict_types=1);

namespace Rodriguez\Example\Tests;

use PHPUnit\Framework\TestCase;
use Rodriguez\Example\ProductClient;
use Rodriguez\Example\ProductApiError;

class ProductClientTest extends TestCase
{
    private const PORTS = [
        'REFUSE_CONNECTION'     => 10201,
        'NOT_ACCEPT'            => 10202,
        'RST_AFTER_DELAY'       => 10203,
        'NEVER_DRAIN'           => 10204,
        'SLOW_RESPONSE'         => 10205,
        'CONTENT_TYPE_MISMATCH' => 10206,
        'RESPONSE_HEADER_ONLY'  => 10207,
        'BROKEN_JSON'           => 10208,
        'ACCEPT_BUT_SILENT'     => 10209,
        'OVERSIZED_RESPONSE'    => 10211,
        'REFUSE_AUTH'           => 10212,
    ];

    private function clientFor(int $port, float $timeoutSec = 2.0, int $maxResponseBytes = 5 * 1024 * 1024): ProductClient
    {
        return new ProductClient(
            "http://localhost:{$port}",
            timeoutSec: $timeoutSec,
            connectTimeoutSec: $timeoutSec,
            maxResponseBytes: $maxResponseBytes,
        );
    }

    private function getError(ProductClient $client): ProductApiError
    {
        try {
            $client->getProduct('1');
            $this->fail('Expected ProductApiError but call succeeded');
        } catch (ProductApiError $e) {
            return $e;
        }
    }

    // --- TCP-level faults ---

    public function testRefuseConnection(): void
    {
        $client = $this->clientFor(self::PORTS['REFUSE_CONNECTION'], timeoutSec: 1.0);
        $err = $this->getError($client);
        $this->assertSame('CONNECTION_ERROR', $err->errorCode);
    }

    public function testNotAccept(): void
    {
        $client = $this->clientFor(self::PORTS['NOT_ACCEPT'], timeoutSec: 2.0);
        $err = $this->getError($client);
        $this->assertContains($err->errorCode, ['TIMEOUT', 'CONNECTION_ERROR']);
    }

    public function testNoResponseAndSendRST(): void
    {
        $client = $this->clientFor(self::PORTS['RST_AFTER_DELAY'], timeoutSec: 5.0);
        $err = $this->getError($client);
        $this->assertSame('CONNECTION_ERROR', $err->errorCode);
    }

    public function testNeverDrain(): void
    {
        $client = $this->clientFor(self::PORTS['NEVER_DRAIN'], timeoutSec: 2.0);
        $err = $this->getError($client);
        $this->assertContains($err->errorCode, ['TIMEOUT', 'CONNECTION_ERROR']);
    }

    public function testAcceptButSilent(): void
    {
        $client = $this->clientFor(self::PORTS['ACCEPT_BUT_SILENT'], timeoutSec: 2.0);
        $err = $this->getError($client);
        $this->assertSame('TIMEOUT', $err->errorCode);
    }

    // --- HTTP-level faults ---

    public function testSlowResponse(): void
    {
        $client = $this->clientFor(self::PORTS['SLOW_RESPONSE'], timeoutSec: 2.0);
        $err = $this->getError($client);
        $this->assertContains($err->errorCode, ['TIMEOUT', 'CONNECTION_ERROR']);
    }

    public function testContentTypeMismatch(): void
    {
        $client = $this->clientFor(self::PORTS['CONTENT_TYPE_MISMATCH'], timeoutSec: 3.0);
        $err = $this->getError($client);
        $this->assertSame('HTTP_ERROR', $err->errorCode);
        $this->assertSame(400, $err->status);
    }

    public function testResponseHeaderOnly(): void
    {
        $client = $this->clientFor(self::PORTS['RESPONSE_HEADER_ONLY'], timeoutSec: 3.0);
        $err = $this->getError($client);
        $this->assertContains($err->errorCode, ['TIMEOUT', 'CONNECTION_ERROR']);
    }

    public function testBrokenJson(): void
    {
        $client = $this->clientFor(self::PORTS['BROKEN_JSON'], timeoutSec: 3.0);
        $err = $this->getError($client);
        $this->assertSame('INVALID_JSON', $err->errorCode);
    }

    public function testOversizedResponse(): void
    {
        $client = $this->clientFor(
            self::PORTS['OVERSIZED_RESPONSE'],
            timeoutSec: 10.0,
            maxResponseBytes: 1024 * 1024,
        );
        $err = $this->getError($client);
        $this->assertSame('RESPONSE_TOO_LARGE', $err->errorCode);
    }

    public function testRefuseAuthentication(): void
    {
        $client = $this->clientFor(self::PORTS['REFUSE_AUTH'], timeoutSec: 3.0);
        $err = $this->getError($client);
        $this->assertSame('UNAUTHORIZED', $err->errorCode);
        $this->assertSame(401, $err->status);
    }
}
