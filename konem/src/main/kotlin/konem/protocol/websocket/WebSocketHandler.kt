package konem.protocol.websocket

import konem.netty.Handler

abstract class WebSocketHandler<T>(internal val webSocketPath: String) : Handler<T>()
