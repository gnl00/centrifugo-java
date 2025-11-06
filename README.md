# Centrifugo

---



## Why Centrifugo

why not use Kafka, RocketMQ or other existing MQs?

our requirements

- Mobile/Web/Desktop clients connect to message server and exchange messages.
- Support one-to-one messaging
- Support one-to-many messaging
- Support WebSocket protocol
- Http based Message API

---



## Launch

launch Centrifugo with Docker

create config file

```shell
docker run --rm -v$PWD:/centrifugo centrifugo/centrifugo:v6 centrifugo genconfig
```

edit config file to enable admin web page

```config.json
{
  ...
  "admin": {
    "enabled": true,
    ...
  }
}
```

```shell
docker run --rm --ulimit nofile=262144:262144 -v config/file/path:/centrifugo -p 8000:8000 centrifugo/centrifugo centrifugo -c config.json
```

open browser enter `http://localhost:8000` and you will enter to the admin web page

---



## Connect to Centrifugo

first, you need to create a JWT token for your Centrifugo client

create JWT token by `centrifugo` subcommand

```shell
# means create jwt token for user 123722
centrifugo gentoken -u 123722
```

```html
<html>

<head>
  <title>Centrifugo quick start</title>
</head>

<body>
  <div id="counter">-</div>
  <script src="https://unpkg.com/centrifuge@5.4.0/dist/centrifuge.js"></script>
  <script type="text/javascript">
    const container = document.getElementById('counter');

    const centrifuge = new Centrifuge("ws://localhost:8000/connection/websocket", {
      token: "<PUT-YOUR-TOKEN-HERE>"
    });

    centrifuge.on('connecting', function (ctx) {
      console.log(`connecting: ${ctx.code}, ${ctx.reason}`);
    }).on('connected', function (ctx) {
      console.log(`connected over ${ctx.transport}`);
    }).on('disconnected', function (ctx) {
      console.log(`disconnected: ${ctx.code}, ${ctx.reason}`);
    }).connect();

    const sub = centrifuge.newSubscription("channel");

    sub.on('publication', function (ctx) {
      container.innerHTML = ctx.data.value;
      document.title = ctx.data.value;
    }).on('subscribing', function (ctx) {
      console.log(`subscribing: ${ctx.code}, ${ctx.reason}`);
    }).on('subscribed', function (ctx) {
      console.log('subscribed', ctx);
    }).on('unsubscribed', function (ctx) {
      console.log(`unsubscribed: ${ctx.code}, ${ctx.reason}`);
    }).subscribe();
  </script>
</body>

</html>
```

and now start the page, open brower console you will see something likes bellow:

```shell
permission denied
```

edit `config.json`, and `channel` part:

```json
{
  ...
  "channel": {
    "without_namespace": {
      "allow_subscribe_for_client": true
    }
  }
}
```

restart centrifugo, refresh the page and you can see, everything is good now

```console
connecting: 0, connect called
subscribing: 0, subscribe called
connected over websocket
subscribed {channel: 'channel', positioned: false, recoverable: false, wasRecovering: false, recovered: false, …}
```

---



## Enable Message History

enable history feature in Centrifugo

```json
{
  ...
  "channel": {
    "without_namespace": {
      "history_size": 10,
      "history_ttl": "60s"
    }
  }
}
```

allow subscriber to fetch history messages

```json
{
  ...
  "channel": {
    "without_namespace": {
      "allow_history_for_subscriber": true,
      "allow_history_for_client": true // or...
  }
}
```

restart centrifugo service, and now you can fetch history from subscirber or client.

---



## Centrifugo QA

- How many connections can one Centrifugo instance handle?

This depends on many factors. Real-time transport choice, hardware, message rate, size of messages, Centrifugo features enabled, client distribution over channels, compression on/off, etc.

Generally, we suggest not putting more than 50-100k clients on one node - but you should measure for your use case.

[Million connections with Centrifugo](https://centrifugal.dev/blog/2020/02/10/million-connections-with-centrifugo)

---

- Memory usage per connection?

Depending on transport used and features enabled the amount of RAM required per each connection can vary.

For example, you can expect that each WebSocket connection will cost about 30-50 KB of RAM, thus a server with 1 GB of RAM can handle about 20-30k connections.

---

- How can I know a message is delivered to a client?

You can, but Centrifugo does not have such an API. What you have to do to ensure your client has received a message is sending confirmation ack from your client to your application backend as soon as the client processed the message coming from a Centrifugo channel.

> It means Centrifugo has no embedded ACK support, you need to implement it yourself in your backend service. Centrifugo 未内置 ACK 支持，需要在自己的服务中实现。

---



## Reference

- https://centrifugal.dev/docs/getting-started/quickstart
- https://centrifugal.dev/docs/server/history_and_recovery
- https://centrifugal.dev/docs/faq
