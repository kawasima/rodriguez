# Rodriguez Examples

rodriguez を使った障害注入テストの各言語サンプル集。
各言語の AWS SDK がデフォルトでどのようなタイムアウト挙動をするかを、実際のテストで実証する。

## 言語別サンプル

| ディレクトリ | 言語 | テストフレームワーク | AWS SDK | テスト数 |
| --- | --- | --- | --- | --- |
| [nodejs/](nodejs/) | Node.js 18+ | vitest | AWS SDK v3 (`@aws-sdk/client-s3`, `@aws-sdk/client-sqs`) | 32 |
| [go/](go/) | Go 1.21+ | `go test` | AWS SDK for Go v2 (`aws-sdk-go-v2`) | 27 |
| [php/](php/) | PHP 8.1+ | PHPUnit | AWS SDK for PHP (`aws/aws-sdk-php` + Guzzle/cURL) | 33 |

## 起動方法

```bash
# リポジトリルートで rodriguez を起動
docker compose up -d

# 各言語のテストを実行
cd examples/nodejs && npm install && npm test
cd examples/go && go test -v -count=1 -timeout 120s ./...
cd examples/php && composer install && vendor/bin/phpunit --testdox
```

## AWS SDK タイムアウトの落とし穴: 言語間比較

3 つの AWS SDK で共通の障害パターン（AcceptButSilent, SlowResponse, ResponseHeaderOnly）に対するタイムアウト挙動を検証した結果をまとめる。

### 共通の落とし穴: デフォルトタイムアウトが無限

**全 3 言語の AWS SDK で、デフォルトのタイムアウトは「なし」（0 または未設定）。**

サーバーが応答しない場合、リクエストは永遠にハングする。これは AWS SDK の問題というよりも、各言語の HTTP クライアントライブラリのデフォルト値に起因する。

| 言語 | HTTP クライアント | デフォルトタイムアウト |
| ------ | ------------------ | --------------------- |
| Node.js | `NodeHttpHandler` (node:http) | `DEFAULT_REQUEST_TIMEOUT = 0` |
| Go | `http.DefaultClient` | `Timeout = 0` |
| PHP | Guzzle (cURL) | `timeout = 0` |

### Node.js 固有の落とし穴

Node.js SDK v3 には、他の言語にはない **2 つの追加の落とし穴** がある。

#### 落とし穴 A: `requestTimeout` を設定しても throw しない

`NodeHttpHandler` に `requestTimeout` を渡しても、デフォルトでは **Warning ログを出すだけ**でリクエストは中断されない。

```
@smithy/node-http-handler - [WARN] a request has exceeded the configured 1000 ms
requestTimeout. Init client requestHandler with throwOnRequestTimeout=true to turn
this into an error.
```

**`throwOnRequestTimeout: true` を明示的に設定しないとエラーにならない。**

```js
// Bad: タイムアウトしても Warning だけ出て処理が続行
new NodeHttpHandler({ requestTimeout: 5000 })

// Good: タイムアウト時にエラーを throw
new NodeHttpHandler({ requestTimeout: 5000, throwOnRequestTimeout: true })
```

Go と PHP にはこの問題は存在しない。タイムアウトを設定すれば必ずエラーになる。

#### 落とし穴 B: `requestTimeout` はレスポンスボディの読み取りをカバーしない

`requestTimeout` は「レスポンスヘッダが届くまでの時間」を制御する。
ヘッダが即座に返り、ボディが遅い（または来ない）場合、タイマーは満たされてしまう。

- **SlowResponse**: HTTP 200 + ヘッダを即座に返し、ボディを 1 byte/秒で送信
- **ResponseHeaderOnly**: HTTP 200 + ヘッダ + 1 byte を返し、以降は無応答

```
SQS + SlowResponse: requestTimeout + throwOnRequestTimeout still hangs on slow body
S3  + SlowResponse: send() succeeds but body read hangs — requestTimeout does NOT cover this
```

**対策: `AbortSignal.timeout()` や `Promise.race` によるアプリケーションレベルのタイムアウトが必要。**

```js
// send() に AbortSignal を渡す
await client.send(command, { abortSignal: AbortSignal.timeout(5000) });

// S3 GetObject のボディ読み取りに Promise.race を使う
const result = await client.send(new GetObjectCommand({ Bucket, Key }));
const body = await Promise.race([
  result.Body.transformToString(),
  new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Body read timeout')), 5000)),
]);
```

### Go 固有の特性

Go の `http.Transport.ResponseHeaderTimeout` は Node.js の `requestTimeout` と同様に「ヘッダが届くまでの時間」しかカバーしない。ただし、Go には **`http.Client.Timeout`** と **`context.WithTimeout`** という 2 つの全ライフサイクルタイムアウトがあり、どちらもボディ読み取りまでカバーする。

```go
// 方法 A: リクエスト単位のタイムアウト（推奨）
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()
result, err := client.GetObject(ctx, input)

// 方法 B: クライアント全体のタイムアウト
httpClient := &http.Client{Timeout: 30 * time.Second}
```

### PHP 固有の特性

PHP (Guzzle) の `timeout` は cURL の `CURLOPT_TIMEOUT` にマッピングされ、**転送全体**をカバーする。`timeout` の一つの設定だけで全てのケースに対応できる。Node.js のような「ヘッダ到着後のボディ読み取りはカバーしない」という落とし穴は存在しない。

```php
$client = new S3Client([
    'http' => [
        'timeout'         => 30,   // 転送全体のタイムアウト（秒）
        'connect_timeout' => 5,    // 接続確立のタイムアウト（秒）
    ],
]);
```

### 障害パターン別の挙動比較

#### 無応答（AcceptButSilent）— サーバーが接続を受け入れるが一切応答しない

| 設定 | Node.js | Go | PHP |
| ------ | --------- | ----- | ----- |
| デフォルト | ハング | ハング | ハング |
| `requestTimeout` / `ResponseHeaderTimeout` のみ | Warning のみ | ハング | — |
| `requestTimeout` + `throwOnRequestTimeout` | **タイムアウト** | — | — |
| `http.Client.Timeout` / Guzzle `timeout` | — | **タイムアウト** | **タイムアウト** |
| `AbortSignal` / `context.WithTimeout` | **タイムアウト** | **タイムアウト** | — |

#### 遅いボディ（SlowResponse）— ヘッダは即座に返し、ボディを 1 byte/秒で送信

| 設定 | Node.js | Go | PHP |
| ------ | --------- | ----- | ----- |
| デフォルト | ハング | ハング | ハング |
| `requestTimeout` / `ResponseHeaderTimeout` のみ | Warning のみ | ハング（ヘッダは届く） | — |
| `requestTimeout` + `throwOnRequestTimeout` | **ハング（ヘッダは届く）** | — | — |
| `http.Client.Timeout` / Guzzle `timeout` | — | **タイムアウト** | **タイムアウト** |
| `AbortSignal` / `Promise.race` / `context.WithTimeout` | **タイムアウト** | **タイムアウト** | — |

#### 不完全ボディ（ResponseHeaderOnly）— ヘッダ + 1 byte を返し、以降は無応答

| 設定 | Node.js | Go | PHP |
| ------ | --------- | ----- | ----- |
| デフォルト | ハング | ハング | ハング |
| `requestTimeout` + `throwOnRequestTimeout` | **ハング（ヘッダは届く）** | — | — |
| `http.Client.Timeout` / Guzzle `timeout` | — | **タイムアウト** | **タイムアウト** |
| `AbortSignal` / `Promise.race` / `context.WithTimeout` | **タイムアウト** | **タイムアウト** | — |

### 落とし穴の数の比較

| 落とし穴 | Node.js | Go | PHP |
| --------- | --------- | ----- | ----- |
| デフォルトタイムアウトが無限 | あり | あり | あり |
| タイムアウト設定しても throw しない | **あり** | なし | なし |
| ヘッダ用タイムアウトがボディをカバーしない | **あり** | あり | なし |
| メインの timeout がボディをカバー | **しない** | する | する |
| **落とし穴の合計** | **3** | **2** | **1** |

### 各言語の推奨設定

#### Node.js

```js
import { NodeHttpHandler } from '@smithy/node-http-handler';

const client = new S3Client({
  requestHandler: new NodeHttpHandler({
    requestTimeout: 5000,
    connectionTimeout: 3000,
    throwOnRequestTimeout: true,  // これを忘れない
  }),
  maxAttempts: 3,
});

// さらに、ボディ読み取りには必ずアプリケーションレベルのタイムアウトを設定する
await client.send(command, { abortSignal: AbortSignal.timeout(5000) });
```

#### Go

```go
// 方法 A: リクエスト単位のタイムアウト（推奨）
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()
result, err := client.GetObject(ctx, input)

// 方法 B: クライアント全体のタイムアウト
httpClient := &http.Client{
    Timeout: 30 * time.Second,
    Transport: &http.Transport{
        DialContext: (&net.Dialer{Timeout: 3 * time.Second}).DialContext,
        TLSHandshakeTimeout:   3 * time.Second,
        ResponseHeaderTimeout: 10 * time.Second,
    },
}
client := s3.NewFromConfig(cfg, func(o *s3.Options) {
    o.HTTPClient = httpClient
})
```

#### PHP

```php
$client = new S3Client([
    'region' => 'ap-northeast-1',
    'http' => [
        'timeout'         => 30,
        'connect_timeout' => 5,
    ],
    'retries' => 3,
]);
```

## rodriguez のポート一覧

| ポート | 用途 |
| -------- | ------ |
| 10200 | Control port |
| 10201 | RefuseConnection |
| 10202 | NotAccept |
| 10203 | NoResponseAndSendRST |
| 10204 | NeverDrain |
| 10205 | SlowResponse |
| 10206 | ContentTypeMismatch |
| 10207 | ResponseHeaderOnly |
| 10208 | BrokenJson |
| 10209 | AcceptButSilent |
| 10210 | MockDatabase (JDBC) |
| 10211 | OversizedResponse |
| 10212 | RefuseAuthentication |
| 10213 | S3 Mock |
| 10214 | SQS Mock |
