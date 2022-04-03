package konem.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import konem.logger
import java.net.SocketAddress

interface HandlerListener<I> {
    fun registerActiveHandler(handler: Handler<I>, channelPort: Int, remoteConnection: SocketAddress)
    fun registerInActiveHandler(handler: Handler<I>, channelPort: Int, remoteConnection: SocketAddress)
}

abstract class Handler<I>() :
    SimpleChannelInboundHandler<I>() {

    internal val logger = logger(javaClass)

    private lateinit var context: ChannelHandlerContext
    internal lateinit var remoteAddress: SocketAddress

    internal var handlerId: Long = -1
    internal lateinit var transceiver: Transceiver<I>

   open fun sendMessage(message: I) {
        if (isActive()) {
            logger.trace("[write2Wire] dest: {} msg: {} ", remoteAddress, message.toString())
            context.writeAndFlush(message)
        } else {
            logger.warn("called when channel not active or writable")
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} connected", ctx.channel().remoteAddress())
        context = ctx
        remoteAddress = ctx.channel().remoteAddress()
        transceiver.handlerActive(remoteAddress, this)
        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("remote peer: {} disconnected", ctx.channel().remoteAddress())
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
}