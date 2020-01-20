package konem.protocol.websocket.json

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod.GET
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory

class WebSocketPathHandler(
  val transceiver: WebSocketTransceiver,
  private val channelIds: AtomicLong,
  private vararg val webSocketPaths: String
) : ChannelInboundHandlerAdapter() {
  private val logger = LoggerFactory.getLogger(WebSocketPathHandler::class.java)

  @Throws(Exception::class)
  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    val req = (msg as FullHttpRequest).copy()
    val path = req.uri()
    try {
      if (isConfiguredWebSocketPath(path)) {
        if (req.method() != GET) {
          sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN))
          return
        }
        // made it this far, valid websocket path
        ctx.pipeline().addAfter(
          ctx.name(), WebSocketServerProtocolHandler::class.java.name,
          WebSocketServerProtocolHandler(path, null, true)
        )
        ctx.pipeline().addAfter(
          WebSocketServerProtocolHandler::class.java.name, "frameHandler",
          WebSocketFrameHandler(
            channelIds.incrementAndGet(),
            transceiver,
            path
          )
        )
        ctx.pipeline().remove(WebSocketPathHandler::class.java.name)
        logger.info(
          "WebSocketServerProtocolHandler and WebSocketFrameHandler  added for websocket path: {}",
          path
        )
        ctx.fireChannelActive()
        ctx.fireChannelRead(msg)
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
      if (!isKeepAlive(req) || res.status().code() != OK) {
        future.addListener(ChannelFutureListener.CLOSE)
      }
    } else {
      logger.warn("sendHttpResponse called when channel not active or writable")
    }
  }

  private fun isConfiguredWebSocketPath(path: String): Boolean {
    var valid = false
    for (configuredPath in webSocketPaths) {
      if (path == configuredPath) {
        valid = true
      }
    }
    return valid
  }

  companion object {
    private const val OK = 200
  }
}
