<?php

declare(strict_types=1);

namespace Rodriguez\Example\Tests;

use Aws\S3\S3Client;
use PHPUnit\Framework\TestCase;

class S3Test extends TestCase
{
    private const S3_PORT = 10213;
    private const BUCKET = 'phpunit-bucket';
    private const KEY = 'hello.txt';
    private const BODY = 'Hello from PHP S3 test';

    private const PORT_SLOW_RESPONSE = 10205;
    private const PORT_RESPONSE_HEADER_ONLY = 10207;
    private const PORT_ACCEPT_BUT_SILENT = 10209;

    private static function createS3Client(int $port = self::S3_PORT, array $httpOptions = []): S3Client
    {
        return new S3Client([
            'region' => 'ap-northeast-1',
            'endpoint' => "http://localhost:{$port}",
            'credentials' => [
                'key' => 'dummy',
                'secret' => 'dummy',
            ],
            'use_path_style_endpoint' => true,
            'retries' => 0,
            'http' => $httpOptions,
        ]);
    }

    // --- Normal operation ---

    public function testCreateBucket(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->createBucket(['Bucket' => self::BUCKET]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    public function testPutObject(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->putObject([
            'Bucket' => self::BUCKET,
            'Key' => self::KEY,
            'Body' => self::BODY,
        ]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    public function testListBuckets(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->listBuckets();
        $names = array_map(fn($b) => $b['Name'], $result['Buckets']);
        $this->assertContains(self::BUCKET, $names);
    }

    public function testListObjects(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->listObjects(['Bucket' => self::BUCKET]);
        $keys = array_map(fn($o) => $o['Key'], $result['Contents']);
        $this->assertContains(self::KEY, $keys);
    }

    public function testGetObject(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->getObject([
            'Bucket' => self::BUCKET,
            'Key' => self::KEY,
        ]);
        $body = (string) $result['Body'];
        $this->assertSame(self::BODY, $body);
    }

    public function testDeleteObject(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->deleteObject([
            'Bucket' => self::BUCKET,
            'Key' => self::KEY,
        ]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    public function testDeleteBucket(): void
    {
        $s3 = self::createS3Client();
        $result = $s3->deleteBucket(['Bucket' => self::BUCKET]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    // --- Fault injection: demonstrating the default timeout problem ---
    //
    // Guzzle's default `timeout` is 0 (no timeout).
    // The AWS SDK for PHP does not set a default timeout.
    // This means requests to misbehaving servers will hang indefinitely.
    //
    // Unlike Node.js SDK v3 where `requestTimeout` only covers response headers,
    // Guzzle's `timeout` maps to CURLOPT_TIMEOUT which covers the FULL transfer
    // lifecycle including response body reads. This is a key advantage of PHP (and Go)
    // over Node.js.

    public function testAcceptButSilentDefaultHangs(): void
    {
        // PITFALL: default client has no timeout — request hangs forever.
        // We set a 3s timeout as a safety net to prove the SDK has no internal timeout.
        $s3 = self::createS3Client(self::PORT_ACCEPT_BUT_SILENT, [
            'timeout' => 3.0,
        ]);
        $start = microtime(true);
        try {
            $s3->createBucket(['Bucket' => 'hang-test']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            // The safety-net timeout fired at ~3s, proving the SDK had no shorter internal timeout.
            $this->assertGreaterThanOrEqual(2.5, $elapsed,
                "Expected to hang for ~3s, but returned in {$elapsed}s");
            $this->addToAssertionCount(1);
        }
    }

    public function testAcceptButSilentFixWithTimeout(): void
    {
        // FIX: Guzzle timeout covers the entire transfer.
        $s3 = self::createS3Client(self::PORT_ACCEPT_BUT_SILENT, [
            'timeout' => 1.0,
        ]);
        $start = microtime(true);
        try {
            $s3->createBucket(['Bucket' => 'timeout-test']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertLessThan(2.5, $elapsed,
                "Expected timeout within ~1s, but took {$elapsed}s");
            $this->assertMatchesRegularExpression('/timed?\s*out/i', $e->getMessage());
        }
    }

    public function testSlowResponseDefaultHangs(): void
    {
        // PITFALL: default client (no timeout) hangs on slow body.
        // We use a 3s timeout as safety net.
        $s3 = self::createS3Client(self::PORT_SLOW_RESPONSE, [
            'timeout' => 3.0,
        ]);
        $start = microtime(true);
        try {
            $s3->getObject(['Bucket' => 'any', 'Key' => 'any']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertGreaterThanOrEqual(2.5, $elapsed,
                "Expected to hang for ~3s, but returned in {$elapsed}s");
            $this->addToAssertionCount(1);
        }
    }

    public function testSlowResponseFixWithTimeout(): void
    {
        // FIX: Guzzle timeout (CURLOPT_TIMEOUT) covers the FULL lifecycle including body reads.
        // Unlike Node.js requestTimeout, this works for slow body reads.
        $s3 = self::createS3Client(self::PORT_SLOW_RESPONSE, [
            'timeout' => 2.0,
        ]);
        $start = microtime(true);
        try {
            $s3->getObject(['Bucket' => 'any', 'Key' => 'any']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertLessThan(3.0, $elapsed,
                "Expected timeout within ~2s, but took {$elapsed}s");
        }
    }

    public function testResponseHeaderOnlyDefaultHangs(): void
    {
        // PITFALL: ResponseHeaderOnly sends HTTP 200 + headers + 1 byte, then hangs.
        // Default client has no timeout — body read hangs forever.
        $s3 = self::createS3Client(self::PORT_RESPONSE_HEADER_ONLY, [
            'timeout' => 3.0,
        ]);
        $start = microtime(true);
        try {
            $s3->getObject(['Bucket' => 'any', 'Key' => 'any']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertGreaterThanOrEqual(2.5, $elapsed,
                "Expected to hang for ~3s, but returned in {$elapsed}s");
            $this->addToAssertionCount(1);
        }
    }
}
