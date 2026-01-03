package konem.protocol.websocket

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod.GET
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import konem.logger
import konem.netty.ServerTransceiver

class WebSocketPathHandler<T>(
    private val handlerId: Long,
    private val transceiver: ServerTransceiver<T>,
    private val wsPaths: Array<String>
): ChannelInboundHandlerAdapter() {
    private val logger = logger(this)

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val req = (msg as FullHttpRequest).copy()
        val path = req.uri()
        try {
            if (isConfiguredWebSocketPath(path)) {
                if (req.method() != GET) {
                    sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST))
                    return
                }
                // made it this far, valid websocket path
                val messageHandler = object: WebSocketHandler<T>(path){
                    override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                        transceiverReceive(message,webSocketPath)
                    }
                }
                messageHandler.handlerId = handlerId
                messageHandler.transceiver = transceiver
                val wsProtoName = WebSocketServerProtocolHandler::class.java.name
                ctx.pipeline().addAfter(
                    ctx.name(),wsProtoName ,
                    WebSocketServerProtocolHandler(path, null, true)
                )
                ctx.pipeline().addBefore("exceptionHandler","messageHandler",messageHandler)
                logger.info(
                    "WebSocketServerProtocolHandler added for websocket path: {}",
                    path
                )
                ctx.fireChannelActive()
                ctx.pipeline().remove(WebSocketPathHandler::class.java.name)
            } else {
                ctx.fireChannelRead(msg)
            }
        } finally {
            req.release()
        }
    }

    private fun sendHttpResponse(ctx: ChannelHandlerContext?, req: HttpRequest, res: HttpResponse) {
        if (ctx != null && (ctx.channel().isOpen || ctx.channel().isActive)) {
            val future = ctx.channel().writeAndFlush(res)
            if (!isKeepAlive(req) || res.status().code() != OK.code()) {
                future.addListener(ChannelFutureListener.CLOSE)
            }
        } else {
            logger.warn("called when channel not active or writable")
        }
    }

    private fun isConfiguredWebSocketPath(path: String): Boolean {
        var valid = false
        print("XXX $wsPaths ==== $path")
        for (configuredPath in wsPaths) {
            if (path == configuredPath) {
                valid = true
            }
        }
        return valid
    }

}
