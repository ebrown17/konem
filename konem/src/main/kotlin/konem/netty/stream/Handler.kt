package konem.netty.stream

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.net.SocketAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Handler<H,T>(val handlerId: Long, val abstractTransceiver: Transceiver<T,H>) :
  SimpleChannelInboundHandler<H>() {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  private lateinit var context: ChannelHandlerContext
  internal lateinit var remoteAddress: SocketAddress

  fun sendMessage(message: H) {
    if (isActive()) {
      logger.trace("{} to {} written to wire", message.toString(), remoteAddress)
      context.writeAndFlush(message)
    } else {
      logger.warn("called when channel not active or writable")
    }
  }

  @Throws(Exception::class)
  override fun channelActive(ctx: ChannelHandlerContext) {
    logger.info("remote peer: {} connected", ctx.channel().remoteAddress())
    context = ctx
    remoteAddress = ctx.channel().remoteAddress()
    abstractTransceiver.handlerActive(remoteAddress, this)
    ctx.fireChannelActive()
  }

  @Throws(Exception::class)
  override fun channelInactive(ctx: ChannelHandlerContext) {
    logger.info("remote peer: {} disconnected", ctx.channel().remoteAddress())
    abstractTransceiver.handlerInActive(remoteAddress)
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
