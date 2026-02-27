package main

import (
	"context"
	"io"
	"net"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

const (
	s3Port = 10213
	bucket = "go-test-bucket"
	key    = "hello.txt"
	body   = "Hello from Go S3 test"
)

func createS3Client(t *testing.T, port int, httpClient *http.Client) *s3.Client {
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
	opts := []func(*s3.Options){
		func(o *s3.Options) {
			o.BaseEndpoint = aws.String(endpoint)
			o.UsePathStyle = true
			o.RetryMaxAttempts = 1
		},
	}
	if httpClient != nil {
		opts = append(opts, func(o *s3.Options) {
			o.HTTPClient = httpClient
		})
	}
	return s3.NewFromConfig(cfg, opts...)
}

// --- Normal operation ---

func TestS3_CreateBucket(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	_, err := client.CreateBucket(context.TODO(), &s3.CreateBucketInput{
		Bucket: aws.String(bucket),
	})
	if err != nil {
		t.Fatalf("CreateBucket failed: %v", err)
	}
}

func TestS3_PutObject(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	_, err := client.PutObject(context.TODO(), &s3.PutObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
		Body:   strings.NewReader(body),
	})
	if err != nil {
		t.Fatalf("PutObject failed: %v", err)
	}
}

func TestS3_ListBuckets(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	result, err := client.ListBuckets(context.TODO(), &s3.ListBucketsInput{})
	if err != nil {
		t.Fatalf("ListBuckets failed: %v", err)
	}
	found := false
	for _, b := range result.Buckets {
		if aws.ToString(b.Name) == bucket {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("bucket %q not found in ListBuckets", bucket)
	}
}

func TestS3_ListObjects(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	result, err := client.ListObjects(context.TODO(), &s3.ListObjectsInput{
		Bucket: aws.String(bucket),
	})
	if err != nil {
		t.Fatalf("ListObjects failed: %v", err)
	}
	found := false
	for _, obj := range result.Contents {
		if aws.ToString(obj.Key) == key {
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("key %q not found in ListObjects", key)
	}
}

func TestS3_GetObject(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	result, err := client.GetObject(context.TODO(), &s3.GetObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
	})
	if err != nil {
		t.Fatalf("GetObject failed: %v", err)
	}
	defer result.Body.Close()
	data, err := io.ReadAll(result.Body)
	if err != nil {
		t.Fatalf("reading body failed: %v", err)
	}
	if string(data) != body {
		t.Fatalf("expected %q, got %q", body, string(data))
	}
}

func TestS3_DeleteObject(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	_, err := client.DeleteObject(context.TODO(), &s3.DeleteObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(key),
	})
	if err != nil {
		t.Fatalf("DeleteObject failed: %v", err)
	}
}

func TestS3_DeleteBucket(t *testing.T) {
	client := createS3Client(t, s3Port, nil)
	_, err := client.DeleteBucket(context.TODO(), &s3.DeleteBucketInput{
		Bucket: aws.String(bucket),
	})
	if err != nil {
		t.Fatalf("DeleteBucket failed: %v", err)
	}
}

// --- Fault injection: demonstrating the default timeout problem ---
//
// Go's http.DefaultClient has Timeout = 0 (no timeout).
// The AWS SDK for Go v2 also does not set a default timeout.
// This means requests to misbehaving servers will hang indefinitely.
//
// Unlike Node.js SDK v3 where requestTimeout only covers response headers,
// Go's http.Client.Timeout and context.WithTimeout cover the FULL lifecycle
// including response body reads. This makes Go more robust once timeouts
// are configured, but the default "no timeout" pitfall remains.

func TestS3_AcceptButSilent_DefaultHangs(t *testing.T) {
	// PITFALL: default client has no timeout — request hangs forever.
	// We use context.WithTimeout as a safety net to prove the SDK won't timeout on its own.
	client := createS3Client(t, portAcceptButSilent, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	_, err := client.CreateBucket(ctx, &s3.CreateBucketInput{
		Bucket: aws.String("hang-test"),
	})
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected an error but call succeeded")
	}
	// The external context cancelled at ~3s, proving the SDK had no internal timeout.
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected to hang for ~3s, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: default client hung for %v until context cancelled", elapsed)
}

func TestS3_AcceptButSilent_FixWithContextTimeout(t *testing.T) {
	// FIX: context.WithTimeout provides a request-scoped deadline.
	client := createS3Client(t, portAcceptButSilent, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Second)
	defer cancel()
	_, err := client.CreateBucket(ctx, &s3.CreateBucketInput{
		Bucket: aws.String("timeout-test"),
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

func TestS3_AcceptButSilent_FixWithHttpClientTimeout(t *testing.T) {
	// FIX: http.Client.Timeout covers the full request lifecycle.
	httpClient := &http.Client{Timeout: 1 * time.Second}
	client := createS3Client(t, portAcceptButSilent, httpClient)
	start := time.Now()
	_, err := client.CreateBucket(context.TODO(), &s3.CreateBucketInput{
		Bucket: aws.String("timeout-test"),
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

func TestS3_SlowResponse_DefaultHangs(t *testing.T) {
	// PITFALL: default client hangs on slow body — no timeout at all.
	// GetObject returns immediately (headers arrive quickly), but reading the body hangs.
	// We use context.WithTimeout as a safety net to prove the SDK won't timeout on its own.
	client := createS3Client(t, portSlowResponse, nil)
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	result, err := client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String("any"),
		Key:    aws.String("any"),
	})
	if err != nil {
		t.Fatalf("GetObject unexpectedly failed: %v", err)
	}
	// GetObject succeeded — headers arrived in time.
	// Now try to read the body — this will hang until the context deadline.
	defer result.Body.Close()
	start := time.Now()
	_, err = io.ReadAll(result.Body)
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected body read to fail, but succeeded")
	}
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected body read to hang for ~3s, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: default client hung on body read for %v until context cancelled", elapsed)
}

func TestS3_SlowResponse_ResponseHeaderTimeoutDoesNotCoverBody(t *testing.T) {
	// PITFALL: ResponseHeaderTimeout only covers time to response headers.
	// SlowResponse returns HTTP 200 + headers immediately, then trickles body at 1 byte/sec.
	// ResponseHeaderTimeout is satisfied and the body read hangs.
	//
	// This is analogous to Node.js SDK v3's requestTimeout pitfall.
	httpClient := &http.Client{
		Transport: &http.Transport{
			ResponseHeaderTimeout: 1 * time.Second,
			DialContext: (&net.Dialer{
				Timeout: 5 * time.Second,
			}).DialContext,
		},
	}
	client := createS3Client(t, portSlowResponse, httpClient)

	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 4*time.Second)
	defer cancel()

	result, err := client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String("any"),
		Key:    aws.String("any"),
	})
	if err != nil {
		// If the SDK itself consumed the body (deserialization), it may timeout here.
		elapsed := time.Since(start)
		t.Logf("PITFALL confirmed: GetObject failed after %v — %v", elapsed, err)
		return
	}

	// GetObject succeeded — headers arrived within ResponseHeaderTimeout.
	// But reading the body will hang until the context deadline.
	defer result.Body.Close()
	_, err = io.ReadAll(result.Body)
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected body read to fail, but succeeded")
	}
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected body read to hang, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: ResponseHeaderTimeout did not cover body read, hung for %v", elapsed)
}

func TestS3_SlowResponse_FixWithHttpClientTimeout(t *testing.T) {
	// FIX: http.Client.Timeout covers the FULL lifecycle including body reads.
	// This is a key advantage of Go over Node.js SDK v3.
	httpClient := &http.Client{Timeout: 2 * time.Second}
	client := createS3Client(t, portSlowResponse, httpClient)
	start := time.Now()
	result, err := client.GetObject(context.TODO(), &s3.GetObjectInput{
		Bucket: aws.String("any"),
		Key:    aws.String("any"),
	})
	if err != nil {
		elapsed := time.Since(start)
		if elapsed >= 3*time.Second {
			t.Fatalf("expected timeout within ~2s, but took %v", elapsed)
		}
		t.Logf("FIX confirmed: http.Client.Timeout cancelled in %v", elapsed)
		return
	}
	// If GetObject succeeded, the body read should still timeout.
	defer result.Body.Close()
	_, err = io.ReadAll(result.Body)
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected body read to fail, but succeeded")
	}
	if elapsed >= 3*time.Second {
		t.Fatalf("expected timeout within ~2s, but took %v", elapsed)
	}
	t.Logf("FIX confirmed: http.Client.Timeout cancelled body read in %v", elapsed)
}

func TestS3_ResponseHeaderOnly_DefaultHangs(t *testing.T) {
	// PITFALL: ResponseHeaderOnly sends HTTP 200 + headers + 1 byte, then hangs.
	// Default client has no timeout — body read hangs forever.
	client := createS3Client(t, portResponseHeaderOnly, nil)
	start := time.Now()
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	result, err := client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String("any"),
		Key:    aws.String("any"),
	})
	if err != nil {
		elapsed := time.Since(start)
		t.Logf("PITFALL confirmed: GetObject failed after %v — %v", elapsed, err)
		if elapsed < 2500*time.Millisecond {
			t.Fatalf("expected to hang, but returned early in %v", elapsed)
		}
		return
	}
	defer result.Body.Close()
	_, err = io.ReadAll(result.Body)
	elapsed := time.Since(start)
	if err == nil {
		t.Fatal("expected body read to fail, but succeeded")
	}
	if elapsed < 2500*time.Millisecond {
		t.Fatalf("expected body read to hang for ~3s, but returned in %v", elapsed)
	}
	t.Logf("PITFALL confirmed: default client hung on incomplete body for %v", elapsed)
}
