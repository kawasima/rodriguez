# Rodriguez Go Example

Go アプリケーションに対する rodriguez の障害注入テスト例。
`go test` を使い、AWS SDK for Go v2 の各障害パターンに対するタイムアウト挙動を検証する。

## 前提条件

- Go 1.21+
- rodriguez が起動済み（Docker Compose 推奨）

```bash
# リポジトリルートで
docker compose up -d
```

## セットアップ

```bash
cd examples/go
go mod download
```

## テスト実行

```bash
go test -v -count=1 -timeout 120s ./...
```

## テスト構成

### 1. S3 テスト (`s3_test.go`)

AWS SDK for Go v2 (`github.com/aws/aws-sdk-go-v2/service/s3`) を使った S3 Mock の正常系テストと、タイムアウト問題のデモ。

**正常系（port 10213）:** CreateBucket, PutObject, ListBuckets, ListObjects, GetObject, DeleteObject, DeleteBucket

**障害注入テスト:**

| テスト名 | ポート | 内容 |
| --------- | -------- | ------ |
| DefaultHangs | 10209 (AcceptButSilent) | デフォルト設定はタイムアウトなし。3s の context で検出 |
| FixWithContextTimeout | 10209 (AcceptButSilent) | `context.WithTimeout` で ~1s でタイムアウト |
| FixWithHttpClientTimeout | 10209 (AcceptButSilent) | `http.Client.Timeout` で ~1s でタイムアウト |
| DefaultHangs | 10205 (SlowResponse) | GetObject 成功後、ボディ読み取りでハング |
| ResponseHeaderTimeoutDoesNotCoverBody | 10205 (SlowResponse) | `ResponseHeaderTimeout` はヘッダ到着で満たされ、ボディ読み取りをカバーしない |
| FixWithHttpClientTimeout | 10205 (SlowResponse) | `http.Client.Timeout` はボディ読み取りもカバー |
| DefaultHangs | 10207 (ResponseHeaderOnly) | 不完全ボディでハング |

### 2. SQS テスト (`sqs_test.go`)

AWS SDK for Go v2 (`github.com/aws/aws-sdk-go-v2/service/sqs`) を使った SQS Mock の正常系テストと、タイムアウト問題のデモ。

**正常系（port 10214）:** CreateQueue, GetQueueUrl, SendMessage, ReceiveMessage, DeleteMessage, DeleteQueue

**障害注入テスト:**

| テスト名 | ポート | 内容 |
| --------- | -------- | ------ |
| DefaultHangs | 10209 (AcceptButSilent) | デフォルト設定はタイムアウトなし |
| FixWithContextTimeout | 10209 (AcceptButSilent) | `context.WithTimeout` で ~1s でタイムアウト |
| FixWithHttpClientTimeout | 10209 (AcceptButSilent) | `http.Client.Timeout` で ~1s でタイムアウト |
| DefaultHangs | 10205 (SlowResponse) | デフォルト設定はタイムアウトなし |
| ResponseHeaderTimeoutDoesNotCoverBody | 10205 (SlowResponse) | `ResponseHeaderTimeout` はボディ読み取りをカバーしない |
| FixWithHttpClientTimeout | 10205 (SlowResponse) | `http.Client.Timeout` はボディ読み取りもカバー |

### 3. FUSE テスト (`main.go`)

ファイル I/O 障害注入の例（別途 FUSE 設定が必要）。

AWS SDK タイムアウトの落とし穴の詳細と言語間比較は [examples/README.md](../README.md) を参照。
