"""
S3 fault injection tests using boto3 against Rodriguez S3Mock.

Requires Rodriguez running on default ports (docker compose up -d).
"""

import pytest
import boto3
from botocore.config import Config
from botocore.exceptions import (
    ConnectTimeoutError,
    ReadTimeoutError,
    EndpointConnectionError,
)

S3_PORT = 10213

PORTS = {
    "REFUSE_CONNECTION": 10201,
    "NOT_ACCEPT": 10202,
    "SLOW_RESPONSE": 10205,
}


def s3_client(port=S3_PORT, connect_timeout=2, read_timeout=5, retries=0):
    return boto3.client(
        "s3",
        endpoint_url=f"http://localhost:{port}",
        aws_access_key_id="test",
        aws_secret_access_key="test",
        region_name="us-east-1",
        config=Config(
            connect_timeout=connect_timeout,
            read_timeout=read_timeout,
            retries={"max_attempts": retries},
        ),
    )


class TestS3MockBasicOperations:
    """Verify S3Mock works correctly before testing faults."""

    def test_create_and_list_buckets(self):
        client = s3_client()
        bucket_name = "pytest-test-bucket"

        client.create_bucket(Bucket=bucket_name)

        response = client.list_buckets()
        bucket_names = [b["Name"] for b in response["Buckets"]]
        assert bucket_name in bucket_names

        client.delete_bucket(Bucket=bucket_name)

    def test_put_and_get_object(self):
        client = s3_client()
        bucket_name = "pytest-object-test"
        client.create_bucket(Bucket=bucket_name)

        client.put_object(Bucket=bucket_name, Key="hello.txt", Body=b"Hello, Rodriguez!")

        response = client.get_object(Bucket=bucket_name, Key="hello.txt")
        body = response["Body"].read()
        assert body == b"Hello, Rodriguez!"

        # Cleanup
        client.delete_object(Bucket=bucket_name, Key="hello.txt")
        client.delete_bucket(Bucket=bucket_name)


class TestS3ConnectionRefused:
    """boto3 raises EndpointConnectionError when connection is refused."""

    def test_raises_endpoint_connection_error(self):
        client = s3_client(port=PORTS["REFUSE_CONNECTION"], retries=0)
        with pytest.raises(EndpointConnectionError):
            client.list_buckets()


class TestS3ConnectionTimeout:
    """
    boto3 raises ConnectTimeoutError when server never accepts.

    Key pitfall: boto3's default retry count is 3 (standard mode) or 5 (adaptive).
    Without setting retries=0, a ConnectTimeout test with a 2s connect timeout
    could wait up to 8s (2s * 4 attempts).
    """

    def test_raises_connect_timeout(self):
        client = s3_client(port=PORTS["NOT_ACCEPT"], connect_timeout=1, retries=0)
        with pytest.raises(ConnectTimeoutError):
            client.list_buckets()


class TestS3SlowResponse:
    """
    boto3 raises ReadTimeoutError when response body is too slow.

    Key pitfall: boto3's read_timeout applies per socket recv() call,
    not total transfer time. SlowResponse dripping 1 byte/sec may not
    trigger ReadTimeoutError if each byte arrives within the timeout.
    """

    def test_raises_read_timeout_with_short_timeout(self):
        client = s3_client(port=PORTS["SLOW_RESPONSE"], read_timeout=1, retries=0)
        with pytest.raises(ReadTimeoutError):
            client.list_buckets()
