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
    private val webSocketHandlerHolder: WebSocketHandlerHolder<T>,
    private val wsPaths: Array<String>
) : ChannelInboundHandlerAdapter() {
    private val logger = logger(this)

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is FullHttpRequest) {
            ctx.fireChannelRead(msg)
            return
        }
        val path = getCleanPath(msg.uri())

        if (!isConfiguredWebSocketPath(path)) {
            ctx.fireChannelRead(msg)
            return
        }

        try {
            if (msg.method() != GET) {
                sendHttpResponse(ctx, msg, DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST))
                msg.release()
                return
            }
            // made it this far, valid websocket path
            ctx.pipeline().addAfter(
                ctx.name(),
                "wsProtoName-$path",
                WebSocketServerProtocolHandler(path, null, true)
            )
            val messageHandler = webSocketHandlerHolder.getHandler(path)
            ctx.pipeline().addBefore(
                "exceptionHandler",
                "messageHandler-${path}",
                messageHandler
            )
            logger.info("WebSocketServerProtocolHandler added for websocket path: {}", path)
            messageHandler.channelActive(ctx)
            ctx.pipeline().remove(this)
            ctx.fireChannelRead(msg)

        } catch (e: Exception) {
            logger.error("Failed to setup WebSocket pipeline for path: {}", path, e)
            msg.release()
            ctx.close()
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

    private fun getCleanPath(uri: String): String {
        val queryIndex = uri.indexOf('?')
        return if (queryIndex >= 0) uri.substring(0, queryIndex) else uri
    }

    private fun isConfiguredWebSocketPath(path: String): Boolean =
        path in wsPaths

}
