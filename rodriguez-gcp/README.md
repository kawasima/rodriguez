# rodriguez-gcp

Mock implementation of the Google Cloud Storage (GCS) JSON API v1 for testing GCS client interactions without connecting to real Google Cloud infrastructure.

## Default Port

| Port  | Behavior |
| ----- | -------- |
| 10215 | GCSMock  |

## Supported Operations

| Operation        | Method   | Endpoint                                              |
| ---------------- | -------- | ----------------------------------------------------- |
| Create Bucket    | `POST`   | `/storage/v1/b?project={project}`                     |
| List Buckets     | `GET`    | `/storage/v1/b?project={project}`                     |
| Delete Bucket    | `DELETE` | `/storage/v1/b/{bucket}`                              |
| Upload Object    | `POST`   | `/upload/storage/v1/b/{bucket}/o?uploadType=media`    |
| List Objects     | `GET`    | `/storage/v1/b/{bucket}/o`                            |
| Get Metadata     | `GET`    | `/storage/v1/b/{bucket}/o/{object}`                   |
| Download Object  | `GET`    | `/storage/v1/b/{bucket}/o/{object}?alt=media`         |
| Delete Object    | `DELETE` | `/storage/v1/b/{bucket}/o/{object}`                   |

Both `uploadType=media` (raw body) and `uploadType=multipart` (JSON metadata + binary) are supported for uploads.

## Storage

Objects are stored on the local filesystem in a temporary directory created on first request.
Each bucket maps to a subdirectory, and objects are stored as files within it.
The directory is automatically cleaned up on JVM exit.

MD5 and CRC32C checksums are computed per object during upload for client-side validation.

## Configuration

```json
{
  "ports": {
    "10215": {
      "type": "GCSMock"
    }
  }
}
```

## Client Configuration

Point the GCS client to `http://localhost:10215` (or your configured port).

### Java

```java
Storage storage = StorageOptions.newBuilder()
    .setHost("http://localhost:10215")
    .setProjectId("test-project")
    .build()
    .getService();
```

### Node.js

```javascript
const { Storage } = require('@google-cloud/storage');
const storage = new Storage({
  apiEndpoint: 'http://localhost:10215',
  projectId: 'test-project',
});
```

### Python

```python
from google.cloud import storage

client = storage.Client(
    project="test-project",
    client_options={"api_endpoint": "http://localhost:10215"},
)
```

The mock ignores authentication credentials, so any auth configuration will be accepted.
