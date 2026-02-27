<?php

declare(strict_types=1);

namespace Rodriguez\Example\Tests;

use Aws\Sqs\SqsClient;
use PHPUnit\Framework\TestCase;

class SqsTest extends TestCase
{
    private const SQS_PORT = 10214;
    private const PORT_SLOW_RESPONSE = 10205;
    private const PORT_ACCEPT_BUT_SILENT = 10209;

    private static function createSqsClient(int $port = self::SQS_PORT, array $httpOptions = []): SqsClient
    {
        return new SqsClient([
            'region' => 'ap-northeast-1',
            'endpoint' => "http://localhost:{$port}",
            'credentials' => [
                'key' => 'dummy',
                'secret' => 'dummy',
            ],
            'retries' => 0,
            'http' => $httpOptions,
        ]);
    }

    // --- Normal operation ---

    public function testCreateQueue(): void
    {
        $sqs = self::createSqsClient();
        $result = $sqs->createQueue(['QueueName' => 'php-test-queue']);
        $this->assertNotEmpty($result['QueueUrl']);
    }

    public function testGetQueueUrl(): void
    {
        $sqs = self::createSqsClient();
        $result = $sqs->getQueueUrl(['QueueName' => 'php-test-queue']);
        $this->assertNotEmpty($result['QueueUrl']);
    }

    public function testSendMessage(): void
    {
        $sqs = self::createSqsClient();
        $queueUrl = "http://localhost:" . self::SQS_PORT . "/php-test-queue";
        $result = $sqs->sendMessage([
            'QueueUrl' => $queueUrl,
            'MessageBody' => 'Hello from PHP',
        ]);
        $this->assertNotEmpty($result['MessageId']);
        $this->assertNotEmpty($result['MD5OfMessageBody']);
    }

    public function testReceiveMessage(): void
    {
        $sqs = self::createSqsClient();
        $queueUrl = "http://localhost:" . self::SQS_PORT . "/php-test-queue";
        $result = $sqs->receiveMessage(['QueueUrl' => $queueUrl]);
        $this->assertNotEmpty($result['Messages']);
        $msg = $result['Messages'][0];
        $this->assertNotEmpty($msg['Body']);
        $this->assertNotEmpty($msg['MessageId']);
        $this->assertNotEmpty($msg['ReceiptHandle']);
    }

    public function testDeleteMessage(): void
    {
        $sqs = self::createSqsClient();
        $queueUrl = "http://localhost:" . self::SQS_PORT . "/php-test-queue";
        $result = $sqs->deleteMessage([
            'QueueUrl' => $queueUrl,
            'ReceiptHandle' => 'dummy-receipt-handle',
        ]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    public function testDeleteQueue(): void
    {
        $sqs = self::createSqsClient();
        $queueUrl = "http://localhost:" . self::SQS_PORT . "/php-test-queue";
        $result = $sqs->deleteQueue(['QueueUrl' => $queueUrl]);
        $this->assertLessThan(300, $result['@metadata']['statusCode']);
    }

    // --- Fault injection: demonstrating the default timeout problem ---
    //
    // Guzzle's default `timeout` is 0 (no timeout).
    // The AWS SDK for PHP does not set a default timeout.
    // This means requests to misbehaving servers will hang indefinitely.
    //
    // Unlike Node.js SDK v3, Guzzle's `timeout` maps to CURLOPT_TIMEOUT which
    // covers the FULL transfer lifecycle including response body reads.
    // This means once `timeout` is configured, it protects against both
    // no-response and slow-body scenarios — a key advantage over Node.js.

    public function testAcceptButSilentDefaultHangs(): void
    {
        // PITFALL: default client has no timeout — request hangs forever.
        // We set a 3s timeout as safety net.
        $sqs = self::createSqsClient(self::PORT_ACCEPT_BUT_SILENT, [
            'timeout' => 3.0,
        ]);
        $start = microtime(true);
        try {
            $sqs->createQueue(['QueueName' => 'hang-test']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertGreaterThanOrEqual(2.5, $elapsed,
                "Expected to hang for ~3s, but returned in {$elapsed}s");
            $this->addToAssertionCount(1);
        }
    }

    public function testAcceptButSilentFixWithTimeout(): void
    {
        // FIX: Guzzle timeout covers the entire transfer.
        $sqs = self::createSqsClient(self::PORT_ACCEPT_BUT_SILENT, [
            'timeout' => 1.0,
        ]);
        $start = microtime(true);
        try {
            $sqs->createQueue(['QueueName' => 'timeout-test']);
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
        // PITFALL: default client hangs on slow response — no timeout.
        $sqs = self::createSqsClient(self::PORT_SLOW_RESPONSE, [
            'timeout' => 3.0,
        ]);
        $start = microtime(true);
        try {
            $sqs->createQueue(['QueueName' => 'hang-test']);
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
        // Unlike Node.js requestTimeout, this works for slow body reads too.
        $sqs = self::createSqsClient(self::PORT_SLOW_RESPONSE, [
            'timeout' => 2.0,
        ]);
        $start = microtime(true);
        try {
            $sqs->createQueue(['QueueName' => 'timeout-test']);
            $this->fail('Expected exception');
        } catch (\Exception $e) {
            $elapsed = microtime(true) - $start;
            $this->assertLessThan(3.0, $elapsed,
                "Expected timeout within ~2s, but took {$elapsed}s");
        }
    }
}
