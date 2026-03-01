# Java Spring Boot Proxy Demo

`caller-app` から `receiver-app` への HTTP 通信を、`rodriguez proxy` (`:10220`) 経由にするデモです。

## 構成

- `receiver-app` (`:8080`): `/api/greet` を提供
- `caller-app` (`:8081`): `/api/call` で `receiver-app` を呼び出し
- `rodriguez proxy` (`:10220`): `caller-app` と `receiver-app` の間に挟まる

通信経路:

`caller-app (:8081)` -> `rodriguez proxy (:10220)` -> `receiver-app (:8080)`

## 事前準備

- JDK 21+
- Maven 3.9+
- Docker

## 起動手順

1. 2つのアプリをビルド

```bash
cd examples/java
mvn package
```

2. `receiver-app` を起動 (`:8080`)

```bash
java -jar receiver-app/target/receiver-app-0.1.0-SNAPSHOT.jar
```

3. `rodriguez proxy` を起動 (`:10220`)

```bash
docker run --rm \
  -p 10200-10220:10200-10220 \
  -v "$(pwd)/rodriguez-proxy.json:/config/rodriguez.json:ro" \
  kawasima/rodriguez \
  -c /config/rodriguez.json
```

4. `caller-app` を起動 (`:8081`)

```bash
java -jar caller-app/target/caller-app-0.1.0-SNAPSHOT.jar
```

5. 動作確認

```bash
curl "http://localhost:8081/api/call?name=proxy-demo"
```

期待されるレスポンス例:

```json
{
  "app": "caller-app",
  "viaBaseUrl": "http://localhost:10220",
  "receiverResponse": {
    "message": "hello, proxy-demo!",
    "app": "receiver-app",
    "timestamp": "2026-01-01T00:00:00Z"
  }
}
```

## Proxy デモ

- ダッシュボード: `http://localhost:10220/_proxy/ui/`
- API: `http://localhost:10220/_proxy/api/behaviors`

ダッシュボードで `/api/greet` にフォルトを設定し、`/api/call` を再実行すると、
`caller-app` -> `proxy` -> `fault port` の流れを確認できます。
