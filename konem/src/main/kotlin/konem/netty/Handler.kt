package konem.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import konem.logger
import java.net.SocketAddress

interface HandlerListener<T> {
    fun registerActiveHandler(handler: Handler<T>, channelPort: Int, remoteConnection: SocketAddress)
    fun registerInActiveHandler(handler: Handler<T>, channelPort: Int, remoteConnection: SocketAddress)
}

abstract class Handler<T> :
    SimpleChannelInboundHandler<T>() {

    internal val logger = logger(this)

    private lateinit var context: ChannelHandlerContext
    internal lateinit var remoteAddress: SocketAddress
    private var isHandlerActive: Boolean = false

    internal var handlerId: Long = -1
    internal lateinit var transceiver: Transceiver<T>

    open fun sendMessage(message: T) {
        if (isActive()) {
            logger.trace("[write2Wire] dest: {} msg: {} ", remoteAddress, message.toString())
            context.writeAndFlush(message)
        } else {
            logger.warn("called when channel not active or writable")
        }
    }

    internal fun initializeContext(ctx: ChannelHandlerContext) {
        context = ctx
        remoteAddress = ctx.channel().remoteAddress()
    }

    internal fun activateHandler() {
        if (!isHandlerActive) {
            isHandlerActive = true
            transceiver.handlerActive(remoteAddress, this)
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} connected", ctx.channel().remoteAddress())
        initializeContext(ctx)
        activateHandler()
        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} disconnected", remoteAddress)
        isHandlerActive = false
        transceiver.handlerInActive(remoteAddress)
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
        logger.debug("Id=$handlerId from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message, extra)
    }

    override fun toString(): String {
        return "Handler(Id=$handlerId,transceiver=$transceiver)"
    }
}
