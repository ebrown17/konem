# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Konem (Kotlin Netty Medium) is a Kotlin library that provides high-level abstractions for building TCP socket and WebSocket servers and clients using Netty. The library supports multiple serialization protocols (Wire/Protobuf, JSON, and String) with automatic heartbeat management and reconnection logic.

## Build and Test Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run Single Test
```bash
./gradlew test --tests "ClassName.testMethodName"
```

### Code Quality
```bash
# Run ktlint formatting check
./gradlew ktlintCheck

# Run detekt static analysis
./gradlew detekt
```

### Generate Protocol Buffers
```bash
./gradlew generateMainProtos
```

Note: The proto generation task is excluded from default builds (see settings.gradle). Wire proto files are located in `konem/config/proto/`.

## Architecture

### Core Components

**Konem Entry Point** (`konem/src/main/kotlin/konem/Konem.kt`)
- Factory methods for creating servers and clients
- Main API: `createTcpSocketServer()`, `createTcpSocketClientFactoryOfDefaults()`, `createWebSocketServer()`, `createWebSocketClientFactoryOfDefaults()`

**Netty Abstractions** (`konem/src/main/kotlin/konem/netty/`)
- Core interfaces and protocols: `Handler`, `Transceiver`, `MessageReceiver`, `ConnectionStatusListener`
- `HeartbeatProtocol`: Configures automatic heartbeat behavior for clients and servers
- `ProtocolPipeline<T>`: Abstraction for configuring Netty codec pipelines
- `client/`: Client abstractions with connection management and reconnection logic
- `server/`: Server abstractions supporting multi-port operation

**Protocol Implementations** (`konem/src/main/kotlin/konem/protocol/`)
- `tcp/`: TCP socket implementations (TcpSocketServerImp, TcpClientFactory)
- `websocket/`: WebSocket implementations (WebSocketServerImp, WebSocketClientFactoryImp)
  - WebSocket servers support path-based routing (e.g., `/tester`)
- `konem/`: Protocol pipeline configurations (KonemProtocolPipeline)
  - `wire/`: Protobuf/Wire encoders and decoders
  - `json/`: JSON encoders and decoders
  - `string/`: String encoders and decoders

### Protocol Pipeline Pattern

The library uses a `ProtocolPipeline<T>` abstraction to configure Netty's ChannelPipeline:
- `protoPipelineCodecs`: Configures codecs for TCP sockets
- `wsPipelineFrameCodec`: Configures codecs for WebSocket frames

Available pipelines (via `KonemProtocolPipeline`):
- `getKonemWirePipeline()`: Protobuf with varint32 length prefixing
- `getKonemJsonPipeline()`: JSON over String encoding
- `getKonemStringPipeline()`: Raw string messaging

### Message Flow

1. **Server Setup**: Create server with config (ports/paths) and protocol pipeline
2. **Message Receivers**: Register `MessageReceiver` callbacks per channel or globally
3. **Client Setup**: Use factory to create clients with connection listeners
4. **Communication**: Send messages via `sendMessage()` or `broadcastOnAllChannels()`
5. **Heartbeat**: Automatic heartbeat handling via `ServerHeartbeatProtocol`/`ClientHeartbeatProtocol`

### Data Models

**Protobuf Messages** (`konem/src/main/kotlin/konem/data/protobuf/`)
- Generated from `konem/config/proto/konemwiremessage.proto`
- `KonemMessage`: Union type with `MessageType` enum (UNKNOWN, STATUS, HEARTBEAT, DATA)
- Wire protocol uses Protobuf for efficient binary serialization

**JSON Messages** (`konem/src/main/kotlin/konem/data/json/`)
- Kotlin serialization-based equivalent of protobuf messages

## Project Structure

```
konem/
├── src/main/kotlin/konem/
│   ├── Konem.kt                    # Main API entry point
│   ├── data/                       # Message data classes
│   │   ├── json/                   # JSON message models
│   │   └── protobuf/               # Wire/Protobuf generated models
│   ├── netty/                      # Core Netty abstractions
│   │   ├── client/                 # Client abstractions and connection management
│   │   └── server/                 # Server abstractions and configs
│   └── protocol/                   # Protocol implementations
│       ├── tcp/                    # TCP socket protocol
│       ├── websocket/              # WebSocket protocol
│       └── konem/                  # Codec pipelines
│           ├── wire/               # Protobuf codecs
│           ├── json/               # JSON codecs
│           └── string/             # String codecs
├── src/test/kotlin/konem/          # Kotest-based tests
└── config/
    ├── proto/                      # Protocol buffer definitions
    ├── keystore/                   # TLS keystores for secure connections
    └── detekt/                     # Static analysis config

examples/
├── tcp-sockets/                    # TCP client/server examples
└── websocket-server/               # WebSocket examples
```

## Technology Stack

- **Language**: Kotlin 1.9.20, JVM target 21
- **Framework**: Netty 4.2.9.Final for async I/O
- **Serialization**:
  - Wire 4.9.1 (Protobuf)
  - kotlinx-serialization 1.6.0 (JSON)
- **Testing**: Kotest 5.5.5 (JUnit5 runner), Spock/Groovy
- **Security**: Bouncy Castle 1.76 for TLS/SSL
- **Build**: Gradle 8.6 with Kotlin DSL

## Test Configuration

Tests use JVM args for Netty leak detection and TLS configuration:
```
-Dio.netty.leakDetection.level=PARANOID
-Dkonem.secure.keyStoreLocation=config/keystore/konem.jks
-Dkonem.secure.keyStoreType=JKS
-Dkonem.secure.keyStorePassword=test123
```

## Examples

See `examples/tcp-sockets/src/main/kotlin/konem/example/` for:
- `wire-examples.kt`: TCP server/client with Protobuf messaging
- `json-examples.kt`: TCP server/client with JSON messaging

See `examples/websocket-server/src/main/kotlin/konem/example/` for:
- `wire-examples.kt`: WebSocket server/client with path-based routing

Key patterns demonstrated:
- Multi-port server configuration
- Channel-specific vs. global message receivers
- Client factory pattern for creating multiple clients
- Connection/disconnection listeners
- Heartbeat protocol configuration