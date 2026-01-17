package konem.json_websocket

import konem.data.json.KonemMessage
import konem.netty.client.WebSocketClientFactory
import konem.netty.server.WebSocketServer

var server: WebSocketServer<KonemMessage>? = null
var clientFactory: WebSocketClientFactory<KonemMessage>? = null
