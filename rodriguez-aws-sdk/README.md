# rodriguez-aws-sdk

Rodriguez supports mock AWS services (S3 and SQS) as test harnesses.

## Default ports

| Port | Service |
| --- | --- |
| 10213 | S3Mock |
| 10214 | SQSMock |

## SQSMock

Mock SQS service that supports both AWS JSON protocol (`X-Amz-Target` header) and Query protocol (`Action` parameter).

### Supported operations

| Operation | Description |
| --- | --- |
| CreateQueue | Returns a QueueUrl |
| GetQueueUrl | Returns a QueueUrl |
| SendMessage | Accepts MessageBody, returns MD5, MessageId, SequenceNumber |
| ReceiveMessage | Returns a hardcoded test message |
| DeleteMessage | Returns 200 OK |
| DeleteQueue | Returns 200 OK |

### Usage (Node.js / AWS SDK v3)

```javascript
const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');

const sqs = new SQSClient({
  region: 'us-east-1',
  endpoint: 'http://localhost:10214',
  credentials: {
    accessKeyId: 'dummy',
    secretAccessKey: 'dummy',
  },
});

const command = new SendMessageCommand({
  MessageBody: 'hello',
  QueueUrl: 'http://localhost:10214',
});

sqs.send(command)
  .then((data) => console.log('Success', data.MessageId))
  .catch((err) => console.log('Error', err));
```

### Usage (Java / AWS SDK v1)

```java
AmazonSQS sqs = AmazonSQSClientBuilder.standard()
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:10214", "us-east-1"))
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
    .build();

SendMessageResult result = sqs.sendMessage("http://localhost:10214", "hello");
System.out.println(result.getMessageId());
```

## S3Mock

Mock S3 service that stores objects on the local filesystem.

### Supported operations

| Operation | HTTP | Description |
| --- | --- | --- |
| CreateBucket | PUT /bucket | Creates a bucket directory |
| DeleteBucket | DELETE /bucket | Deletes a bucket directory recursively |
| ListBuckets | GET / | Lists all bucket directories |
| ListObjects | GET /bucket | Lists objects in a bucket |
| PutObject | PUT /bucket/key | Writes object to filesystem |
| GetObject | GET /bucket/key | Reads object from filesystem |
| DeleteObject | DELETE /bucket/key | Deletes object from filesystem |

### Configuration

| property | description | default |
| --- | --- | --- |
| s3Directory | Root directory for bucket storage | (required) |
| endpointHost | Hostname for virtual-hosted-style bucket extraction | localhost |

Bucket and object data are stored as directories and files under `s3Directory`:

```
s3Directory/
├── my-bucket/
│   ├── file1.txt
│   └── dir/file2.txt
└── another-bucket/
    └── data.csv
```

### Programmatic setup

```java
HarnessConfig config = new HarnessConfig();
S3Mock s3Mock = new S3Mock();
s3Mock.setS3Directory(new File("/tmp/s3-data"));
config.setPorts(Map.of(10213, s3Mock));

HarnessServer server = new HarnessServer(config);
server.start();
```

## Configuration file

```json
{
  "ports": {
    "10213": {
      "type": "S3Mock",
      "s3Directory": "/tmp/s3-data"
    },
    "10214": {
      "type": "SQSMock"
    }
  }
}
```
