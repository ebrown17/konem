package konem.protocol.websocket

import konem.netty.stream.ExceptionHandler
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class WebSocketExceptionHandler : ExceptionHandler() {
    private val logger = LoggerFactory.getLogger(WebSocketExceptionHandler::class.java)

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        val req = (message as FullHttpRequest).copy()
        val path = req.uri()
        val addr = ctx.channel().localAddress() as InetSocketAddress
        logger.warn("channelRead: end of pipeline reached without handling: {} on port {}; closing connection", path, addr.port)
        sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN))
        ctx.close()
    }

    private fun sendHttpResponse(ctx: ChannelHandlerContext, req: HttpRequest, res: HttpResponse) {
        val future = ctx.channel().writeAndFlush(res)
        if (!isKeepAlive(req) || res.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE)
        }
    }
}
