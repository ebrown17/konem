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
import io.netty.handler.timeout.IdleStateHandler
import konem.logger
import konem.netty.ExceptionHandler
import konem.netty.HeartbeatProducer
import konem.netty.ServerTransceiver
import konem.netty.server.ServerChannelInfo

class WebSocketPathHandler<T>(
    private val transceiver: ServerTransceiver<T>,
    private val serverChannelInfo: ServerChannelInfo<T>,
    private val wsPaths: Array<String>
): ChannelInboundHandlerAdapter() {
    private val logger = logger(this)

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val req = (msg as FullHttpRequest).copy()
        val path = req.uri()
        logger.info("XXXXX {}",path)
        try {
            if (isConfiguredWebSocketPath(path)) {
                if (req.method() != GET) {
                    sendHttpResponse(ctx, req, DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST))
                    return
                }
                // made it this far, valid websocket path
                val protocolPipeline = serverChannelInfo.protocol_pipeline.getProtocolPipelineCodecs()
                val wsFrameHandlers  = serverChannelInfo.protocol_pipeline.getProtocolWebSocketPipelineFrameHandlers()
                val heartbeatProtocol = serverChannelInfo.heartbeatProtocol
                val handlerPair = serverChannelInfo.protocol_pipeline.getProtocolMessageHandler(path)

                val handlerName = "messageHandler"
                val messageHandler = object: WebSocketHandler<T>(path){
                    override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                        transceiverReceive(message)
                    }

                }

                messageHandler.handlerId = serverChannelInfo.channel_id
                messageHandler.transceiver = transceiver
                logger.info("XXXXXXXXXXXXXXXXXXXXXX")


                val wsProtoName = WebSocketServerProtocolHandler::class.java.name
                ctx.pipeline().addAfter(
                    ctx.name(),wsProtoName ,
                    WebSocketServerProtocolHandler(path, null, true)
                )
                wsFrameHandlers.forEach {(handlerName,handler) ->
                    ctx.pipeline().addAfter(wsProtoName, handlerName, handler)
                }


                protocolPipeline.forEach { entry ->
                    ctx.pipeline().addLast(entry.key, entry.value)
                }

                if (heartbeatProtocol.enabled) {
                   ctx.pipeline().addLast("idleStateHandler", IdleStateHandler(0, heartbeatProtocol.write_idle_time, 0))
                   ctx.pipeline().addLast("heartBeatHandler", HeartbeatProducer(transceiver, heartbeatProtocol.generateHeartbeat))
                }

                ctx.pipeline().addLast(handlerName, messageHandler)
                ctx.pipeline().addLast("exceptionHandler", ExceptionHandler())
                logger.info(
                    "WebSocketServerProtocolHandler and WebSocketFrameHandler  added for websocket path: {}",
                    path
                )
                ctx.pipeline().remove(WebSocketPathHandler::class.java.name)
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
