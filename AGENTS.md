# Repository Guidelines

## Project Structure & Module Organization
- `konem/src/main/kotlin/konem/`: core library code (Netty abstractions, protocols, and data models).
- `konem/src/test/kotlin/konem/`: Kotest-based tests.
- `konem/config/`: build and tooling config (Detekt, protobuf, keystore).
- `examples/`: runnable sample apps (`tcp-sockets`, `websocket-server`).
- Gradle build files live at the repo root and in module directories.

## Build, Test, and Development Commands
- `./gradlew build`: compiles and packages all modules.
- `./gradlew test`: runs the full test suite.
- `./gradlew test --tests "ClassName.testMethodName"`: run a single test.
- `./gradlew ktlintCheck`: Kotlin formatting checks.
- `./gradlew detekt`: static analysis.
- `./gradlew generateMainProtos`: generates Wire/Protobuf classes (not run in default build).

## Coding Style & Naming Conventions
- Kotlin code follows standard Kotlin style; `ktlint` enforces formatting.
- Indentation: 4 spaces, no tabs.
- File/class naming: `PascalCase` for classes and file names; `camelCase` for functions/variables.
- Prefer clear protocol-specific names (e.g., `TcpClient`, `WebSocketServer`).

## Testing Guidelines
- Primary framework: Kotest (JUnit5 runner). Some Groovy/Spock tests exist.
- Prefer descriptive test names that reflect behavior.
- If you add Netty or TLS tests, mirror the existing JVM args used in Gradle:
  `-Dio.netty.leakDetection.level=PARANOID` and keystore settings under `konem/config/keystore/`.

## Security & Configuration Tips
- TLS material for local testing lives in `konem/config/keystore/`.
- Protobuf definitions are in `konem/config/proto/` and require `generateMainProtos` when changed.
