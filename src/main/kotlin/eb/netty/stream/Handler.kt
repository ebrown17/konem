package eb.netty.stream

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.InetSocketAddress

abstract class Handler<I>(val handlerId: Long, val transceiver: Transceiver<I>) : SimpleChannelInboundHandler<I>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private lateinit var context: ChannelHandlerContext
    private lateinit var remoteAddress: InetSocketAddress


    fun sendMessage(message: I) {
        if (isActive()) {
            logger.trace("sendMessage {} to {} written to wire", message.toString(), remoteAddress)
            context.writeAndFlush(message)
        } else {
            logger.warn("sendMessage called when channel not active or writable")
        }
    }

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("channelActive remote peer: {} connected", ctx.channel().remoteAddress())
        context = ctx
        remoteAddress = ctx.channel().remoteAddress() as InetSocketAddress
        transceiver.handlerActive(remoteAddress, this)
        ctx.fireChannelActive()
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("channelInactive remote peer: {} disconnected", ctx.channel().remoteAddress())
        ctx.fireChannelInactive()
        transceiver.handlerInActive(remoteAddress)
    }

    fun isActive(): Boolean {
        if(this::context.isInitialized){
            val channel = context.channel()
            return channel != null && (channel.isOpen || channel.isActive)
        }
        return false
    }
}