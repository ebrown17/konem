package konem.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import konem.logger
import java.net.SocketAddress

interface HandlerListener<T> {
    fun registerActiveHandler(handler: Handler<T>, channelPort: Int)
    fun registerInActiveHandler(handler: Handler<T>, channelPort: Int)
}

data class ConnectionKey(val channelId: String, val remoteAddress: SocketAddress)

abstract class Handler<T>(val transceiver: Transceiver<T>) :
    SimpleChannelInboundHandler<T>() {

    internal val logger = logger(this)

    private lateinit var context: ChannelHandlerContext
    internal lateinit var connectionKey: ConnectionKey
        private set
    private var isHandlerActive: Boolean = false

    open fun sendMessage(message: T) {
        if (isActive()) {
            logger.trace("[write2Wire] dest: {} msg: {} ", connectionKey.remoteAddress, message.toString())
            context.writeAndFlush(message)
        } else {
            logger.warn("called when channel not active or writable")
        }
    }

    internal fun initializeContext(ctx: ChannelHandlerContext) {
        context = ctx
        connectionKey = ConnectionKey(context.channel().id().asLongText(), ctx.channel().remoteAddress())
    }

    internal fun activateHandler() {
        if (!isHandlerActive) {
            logger.debug("Handler active")
            isHandlerActive = true
            transceiver.handlerActive(this)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} connected", ctx.channel().remoteAddress())
        initializeContext(ctx)
        activateHandler()
        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} disconnected", connectionKey.remoteAddress)
        isHandlerActive = false
        transceiver.handlerInActive(this)
        ctx.fireChannelInactive()
    }

    fun isActive(): Boolean {
        if (this::context.isInitialized) {
            val channel = context.channel()
            return channel != null && (channel.isOpen || channel.isActive)
        }
        return false
    }

    fun transceiverReceive(message: T,  extra: String="") {
        logger.debug("Id={} from: {} received: {}",connectionKey.channelId, connectionKey.remoteAddress, message)
        transceiver.receive(connectionKey, message, extra)
    }

    override fun toString(): String {
        if(this::connectionKey.isInitialized) {
            return "Handler($connectionKey,transceiver=$transceiver)"
        }
        return "Handler(transceiver=$transceiver)"
    }
}
