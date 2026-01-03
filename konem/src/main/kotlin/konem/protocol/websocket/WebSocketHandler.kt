package konem.protocol.websocket

import konem.netty.Handler

abstract class WebSocketHandler<T>(val webSocketPath: String) : Handler<T>()

