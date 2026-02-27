package main

import (
	"context"
	"net"
	"net/http"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

const sqsPort = 10214

func createSQSClient(t *testing.T, port int, httpClient *http.Client) *sqs.Client {
	t.Helper()
	endpoint := endpointURL(port)
	cfg, err := config.LoadDefaultConfig(context.TODO(),
		config.WithRegion("ap-northeast-1"),
		config.WithCredentialsProvider(
			credentials.NewStaticCredentialsProvider("dummy", "dummy", ""),
		),
	)
	if err != nil {
		t.Fatalf("failed to load config: %v", err)
	}
	opts := []func(*sqs.Options){
		func(o *sqs.Options) {
			o.BaseEndpoint = aws.String(endpoint)
			o.RetryMaxAttempts = 1
		},
	}
	if httpClient != nil {
		opts = append(opts, func(o *sqs.Options) {
			o.HTTPClient = httpClient
		})
	}
	return sqs.NewFromConfig(cfg, opts...)
}

// --- Normal operation ---

func TestSQS_CreateQueue(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	result, err := client.CreateQueue(context.TODO(), &sqs.CreateQueueInput{
		QueueName: aws.String("go-test-queue"),
	})
	if err != nil {
		t.Fatalf("CreateQueue failed: %v", err)
	}
	if result.QueueUrl == nil {
		t.Fatal("QueueUrl is nil")
	}
	t.Logf("QueueUrl: %s", *result.QueueUrl)
}

func TestSQS_GetQueueUrl(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	result, err := client.GetQueueUrl(context.TODO(), &sqs.GetQueueUrlInput{
		QueueName: aws.String("go-test-queue"),
	})
	if err != nil {
		t.Fatalf("GetQueueUrl failed: %v", err)
	}
	if result.QueueUrl == nil {
		t.Fatal("QueueUrl is nil")
	}
}

func TestSQS_SendMessage(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	queueURL := endpointURL(sqsPort) + "/go-test-queue"
	result, err := client.SendMessage(context.TODO(), &sqs.SendMessageInput{
		QueueUrl:    aws.String(queueURL),
		MessageBody: aws.String("Hello from Go"),
	})
	if err != nil {
		t.Fatalf("SendMessage failed: %v", err)
	}
	if result.MessageId == nil {
		t.Fatal("MessageId is nil")
	}
	if result.MD5OfMessageBody == nil {
		t.Fatal("MD5OfMessageBody is nil")
	}
}

func TestSQS_ReceiveMessage(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	queueURL := endpointURL(sqsPort) + "/go-test-queue"
	result, err := client.ReceiveMessage(context.TODO(), &sqs.ReceiveMessageInput{
		QueueUrl: aws.String(queueURL),
	})
	if err != nil {
		t.Fatalf("ReceiveMessage failed: %v", err)
	}
	if len(result.Messages) == 0 {
		t.Fatal("no messages received")
	}
	msg := result.Messages[0]
	if msg.Body == nil {
		t.Fatal("message Body is nil")
	}
	if msg.MessageId == nil {
		t.Fatal("message MessageId is nil")
	}
	if msg.ReceiptHandle == nil {
		t.Fatal("message ReceiptHandle is nil")
	}
}

func TestSQS_DeleteMessage(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	queueURL := endpointURL(sqsPort) + "/go-test-queue"
	_, err := client.DeleteMessage(context.TODO(), &sqs.DeleteMessageInput{
		QueueUrl:      aws.String(queueURL),
		ReceiptHandle: aws.String("dummy-receipt-handle"),
	})
	if err != nil {
		t.Fatalf("DeleteMessage failed: %v", err)
	}
}

func TestSQS_DeleteQueue(t *testing.T) {
	client := createSQSClient(t, sqsPort, nil)
	queueURL := endpointURL(sqsPort) + "/go-test-queue"
	_, err := client.DeleteQueue(context.TODO(), &sqs.DeleteQueueInput{
		QueueUrl: aws.String(queueURL),
	})
	if err != nil {
		t.Fatalf("DeleteQueue failed: %v", err)
	}
}

// --- Fault injection: demonstrating the default timeout problem ---
//
// Go's http.DefaultClient has Timeout = 0 (no timeout).
// The AWS SDK for Go v2 also does not set a default timeout.
// This means requests to misbehaving servers will hang indefinitely.

func TestSQS_AcceptButSilent_DefaultHangs(t *testing.T) {
	// PITFALL: default client has no timeout — request hangs forever.
	// We use context.WithTimeout as a safety net to prove the SDK won't timeout on its own.
	client := createSQSClient(t, portAcceptButSilent, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	_, err := client.CreateQueue(ctx, &sqs.CreateQueueInput{
		QueueName: aws.String("hang-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected to hang for ~3s, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: default client hung for %v until context cancelled", elapsed)
}

func TestSQS_AcceptButSilent_FixWithContextTimeout(t *testing.T) {
	// FIX: context.WithTimeout provides a request-scoped deadline.
	client := createSQSClient(t, portAcceptButSilent, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Second)
	defer cancel()
	_, err := client.CreateQueue(ctx, &sqs.CreateQueueInput{
		QueueName: aws.String("timeout-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	if elapsed >= 2500*time.Millisecond {
		t.Fatalf("expected timeout within ~1s, but took %v", elapsed)
	}
	t.Logf("FIX confirmed: context.WithTimeout cancelled in %v", elapsed)
}

func TestSQS_AcceptButSilent_FixWithHttpClientTimeout(t *testing.T) {
	// FIX: http.Client.Timeout covers the full request lifecycle.
	httpClient := &http.Client{Timeout: 1 * time.Second}
	client := createSQSClient(t, portAcceptButSilent, httpClient)
	start := time.Now()
	_, err := client.CreateQueue(context.TODO(), &sqs.CreateQueueInput{
		QueueName: aws.String("timeout-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	if elapsed >= 2500*time.Millisecond {
		t.Fatalf("expected timeout within ~1s, but took %v", elapsed)
	}
	t.Logf("FIX confirmed: http.Client.Timeout cancelled in %v", elapsed)
}

func TestSQS_SlowResponse_DefaultHangs(t *testing.T) {
	// PITFALL: default client hangs on slow response — no timeout at all.
	client := createSQSClient(t, portSlowResponse, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	_, err := client.CreateQueue(ctx, &sqs.CreateQueueInput{
		QueueName: aws.String("hang-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected to hang for ~3s, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: default client hung for %v on slow response", elapsed)
}

func TestSQS_SlowResponse_ResponseHeaderTimeoutDoesNotCoverBody(t *testing.T) {
	// PITFALL: ResponseHeaderTimeout only covers time to response headers.
	// SlowResponse returns HTTP 200 + headers immediately, then trickles body at 1 byte/sec.
	// ResponseHeaderTimeout is satisfied and the body read (during SDK deserialization) hangs.
	//
	// Unlike S3 GetObject where the body is returned as a stream to the caller,
	// SQS responses are fully deserialized by the SDK. So the hang occurs inside send().
	httpClient := &http.Client{
		Transport: &http.Transport{
			ResponseHeaderTimeout: 1 * time.Second,
			DialContext: (&net.Dialer{
				Timeout: 5 * time.Second,
			}).DialContext,
		},
	}
	client := createSQSClient(t, portSlowResponse, httpClient)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 4*time.Second)
	defer cancel()
	_, err := client.CreateQueue(ctx, &sqs.CreateQueueInput{
		QueueName: aws.String("slow-body-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	// ResponseHeaderTimeout didn't help — headers arrived in time.
	// Only the context deadline (4s) or another mechanism can save us.
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected to hang beyond ResponseHeaderTimeout, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: ResponseHeaderTimeout did not cover body read, hung for %v", elapsed)
}

func TestSQS_SlowResponse_FixWithHttpClientTimeout(t *testing.T) {
	// FIX: http.Client.Timeout covers the FULL lifecycle including body reads.
	// Unlike Node.js requestTimeout, Go's http.Client.Timeout also covers body consumption.
	httpClient := &http.Client{Timeout: 2 * time.Second}
	client := createSQSClient(t, portSlowResponse, httpClient)
	start := time.Now()
	_, err := client.CreateQueue(context.TODO(), &sqs.CreateQueueInput{
		QueueName: aws.String("timeout-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	if elapsed >= 3*time.Second {
		t.Fatalf("expected timeout within ~2s, but took %v", elapsed)
	}
	t.Logf("FIX confirmed: http.Client.Timeout cancelled slow body read in %v", elapsed)
}
