# Rodriguez PHP Example

PHP アプリケーションに対する rodriguez の障害注入テスト例。
[PHPUnit](https://phpunit.de/) を使い、各ポートの障害パターンがクライアントコードにどう影響するかを検証する。

## 前提条件

- PHP 8.1+（cURL 拡張必須）
- Composer
- rodriguez が起動済み（Docker Compose 推奨）

```bash
# リポジトリルートで
docker compose up -d
```

## セットアップ

```bash
cd examples/php
composer install
```

## テスト実行

```bash
vendor/bin/phpunit --testdox
```

## テスト構成

### 1. ProductClient テスト (`tests/ProductClientTest.php`)

Guzzle HTTP クライアントを使った REST API クライアントの耐障害テスト。
rodriguez の各ポートに接続し、クライアントが障害を適切にハンドリングできることを検証する。

| ポート | Behavior | 障害内容 | 期待するエラー |
| -------- | ---------- | --------- | ----------------- |
| 10201 | RefuseConnection | TCP 接続拒否 | `CONNECTION_ERROR` |
| 10202 | NotAccept | listen queue が溢れて accept しない | `TIMEOUT` or `CONNECTION_ERROR` |
| 10203 | NoResponseAndSendRST | 3 秒後に RST 送信 | `CONNECTION_ERROR` |
| 10204 | NeverDrain | 受信バッファを読まない | `TIMEOUT` or `CONNECTION_ERROR` |
| 10205 | SlowResponse | 1 byte/秒でレスポンス | `TIMEOUT` or `CONNECTION_ERROR` |
| 10206 | ContentTypeMismatch | HTTP 400 を返す | `HTTP_ERROR` (status 400) |
| 10207 | ResponseHeaderOnly | ヘッダのみ、ボディ未完了 | `TIMEOUT` or `CONNECTION_ERROR` |
| 10208 | BrokenJson | 不完全な JSON `{` | `INVALID_JSON` |
| 10209 | AcceptButSilent | 接続後に無応答 | `TIMEOUT` |
| 10211 | OversizedResponse | 10MB のレスポンス | `RESPONSE_TOO_LARGE` |
| 10212 | RefuseAuthentication | 401 Unauthorized | `UNAUTHORIZED` (status 401) |

### 2. S3 テスト (`tests/S3Test.php`)

AWS SDK for PHP (`aws/aws-sdk-php`) を使った S3 Mock の正常系テストと、タイムアウト問題のデモ。

**正常系（port 10213）:** CreateBucket, PutObject, ListBuckets, ListObjects, GetObject, DeleteObject, DeleteBucket

**障害注入テスト:**

| テスト名 | ポート | 内容 |
| --------- | -------- | ------ |
| DefaultHangs | 10209 (AcceptButSilent) | デフォルト設定はタイムアウトなし。3s の timeout で検出 |
| FixWithTimeout | 10209 (AcceptButSilent) | Guzzle `timeout=1s` で ~1s でタイムアウト |
| DefaultHangs | 10205 (SlowResponse) | デフォルト設定はタイムアウトなし |
| FixWithTimeout | 10205 (SlowResponse) | Guzzle `timeout=2s` でボディ読み取りも含めてタイムアウト |
| DefaultHangs | 10207 (ResponseHeaderOnly) | 不完全ボディでハング |

### 3. SQS テスト (`tests/SqsTest.php`)

AWS SDK for PHP (`aws/aws-sdk-php`) を使った SQS Mock の正常系テストと、タイムアウト問題のデモ。

**正常系（port 10214）:** CreateQueue, GetQueueUrl, SendMessage, ReceiveMessage, DeleteMessage, DeleteQueue

**障害注入テスト:**

| テスト名 | ポート | 内容 |
| --------- | -------- | ------ |
| DefaultHangs | 10209 (AcceptButSilent) | デフォルト設定はタイムアウトなし |
| FixWithTimeout | 10209 (AcceptButSilent) | Guzzle `timeout=1s` で ~1s でタイムアウト |
| DefaultHangs | 10205 (SlowResponse) | デフォルト設定はタイムアウトなし |
| FixWithTimeout | 10205 (SlowResponse) | Guzzle `timeout=2s` でボディ読み取りも含めてタイムアウト |

AWS SDK タイムアウトの落とし穴の詳細と言語間比較は [examples/README.md](../README.md) を参照。
