# Konem

A Kotlin library for building TCP and WebSocket servers and clients using Netty.

## Features

- TCP socket servers and clients with multi-port support
- WebSocket servers and clients with path-based routing
- Built-in protocol support for Protocol Buffers (Wire) and JSON
- Automatic heartbeat and reconnection handling
- Custom protocol pipeline support

## Usage

### TCP Server and Client (JSON)

```kotlin
// Create TCP server
val server = Konem.createTcpSocketServer(
    config = {
        addChannel(6160)
        addChannel(6161)
    },
    protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
    heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) }
)

// Register message handler for specific port
server.registerChannelMessageReceiver(6160, MessageReceiver { from, msg ->
    println("Port 6160 received: $msg")
    server.sendMessage(from, KonemMessage(message = Data("Response from 6160")))
})

// Register message handler for all ports
server.registerChannelMessageReceiver(MessageReceiver { from, msg ->
    println("Received: $msg")
})

server.startServer()

// Create TCP client
val clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
    heartbeatProtocol = ClientHeartbeatProtocol(
        isHeartbeat = { message -> message is Heartbeat }
    ),
    protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
)

val client = clientFactory.createClient("localhost", 6160)

client.registerConnectionListener(ConnectionListener {
    println("Connected")
    client.sendMessage(KonemMessage(message = Data("Hello server")))
})

client.registerChannelMessageReceiver(MessageReceiver { from, msg ->
    println("Client received: $msg")
})

client.connect()
```

### WebSocket Server and Client (JSON)

```kotlin
// Create WebSocket server
val server = Konem.createWebSocketServer(
    config = {
        addChannel(8080, "/chat")
    },
    protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
)

// Register message handler
server.registerChannelMessageReceiver(MessageReceiver { from, message ->
    println("Server received: $message")
    server.broadcastOnAllChannels(KonemMessage(message = Data("Broadcast message")))
}, "/chat")

// Track connections
server.registerPathConnectionStatusListener(
    WebSocketConnectionStatusListener(
        connected = { remoteAddr, wsPath ->
            println("Client connected: $remoteAddr to $wsPath")
        },
        disconnected = { remoteAddr, wsPath ->
            println("Client disconnected: $remoteAddr")
        }
    )
)

server.startServer()

// Create WebSocket client
val clientFactory = Konem.createWebSocketClientFactoryOfDefaults(
    KonemProtocolPipeline.getKonemJsonPipeline()
)

val client = clientFactory.createClient("localhost", 8080, "/chat")

client.registerConnectionListener(ConnectionListener { remoteAddr ->
    println("Connected to $remoteAddr")
    client.sendMessage(KonemMessage(message = Data("Hello from client")))
})

client.registerChannelMessageReceiver(MessageReceiver { from, msg ->
    println("Client received: $msg")
})

client.connect()
```

## Creating a Custom Protocol

Implement custom encoders and decoders, then create a `ProtocolPipeline`:

```kotlin
data class MyCustomMessage(val content: String)

class MyCustomEncoder : MessageToMessageEncoder<MyCustomMessage>() {
    override fun encode(ctx: ChannelHandlerContext, msg: MyCustomMessage, out: MutableList<Any>) {
        val bytes = msg.content.toByteArray()
        val buf = ctx.alloc().buffer(bytes.size)
        buf.writeBytes(bytes)
        out.add(buf)
    }
}

class MyCustomDecoder : SimpleChannelInboundHandler<ByteBuf>() {
    override fun channelRead0(ctx: ChannelHandlerContext, buf: ByteBuf) {
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        ctx.fireChannelRead(MyCustomMessage(String(bytes)))
    }
}

val customPipeline = ProtocolPipeline<MyCustomMessage>(
    protoPipelineCodecs = { pipeline ->
        pipeline["frameDecoder"] = LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4)
        pipeline["frameEncoder"] = LengthFieldPrepender(4)
        pipeline["customDecoder"] = MyCustomDecoder()
        pipeline["customEncoder"] = MyCustomEncoder()
    },
    wsPipelineFrameCodec = { pipeline ->
        // WebSocket codec configuration
    }
)

// Use custom pipeline
val server = Konem.createTcpSocketServer(
    config = { addChannel(9000) },
    protocolPipeline = customPipeline,
    heartbeatProtocol = ServerHeartbeatProtocol { MyCustomMessage("heartbeat") }
)
```

## Building from Source

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Code quality checks
./gradlew ktlintCheck
./gradlew detekt
```

**Note:** Test framework is currently being updated following the `new-layout` branch refactor.

## Requirements

- Kotlin 1.9.20+
- JVM 21+

## Examples

See the `examples/` directory for complete working examples:
- `examples/tcp-sockets/` - TCP client/server examples
- `examples/websocket-server/` - WebSocket examples

## License

Apache License 2.0 - see [License.txt](License.txt)
