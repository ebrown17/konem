package konem.netty

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import konem.logger

class ExceptionHandler : ChannelDuplexHandler() {
    private val logger = logger(javaClass)
    private var lastExceptionTime: Long = 0
    private var exceptionCount = 0

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        logger.warn("end of pipeline reached without handling: {}", message.toString())
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("Connection from {} cause {}", ctx.channel().remoteAddress(), cause.toString())
        val now = System.currentTimeMillis()
        if (now - lastExceptionTime >= EXCEPTION_TIME) {
            exceptionCount++
            lastExceptionTime = now
        } else {
            exceptionCount = 0
        }
        if (exceptionCount >= MAX_EXCEPTIONS) {
            logger.error("To many exceptions closing connection from {}", ctx.channel().remoteAddress())
            ctx.close()
        }
    }

    companion object {
        private const val EXCEPTION_TIME = 5000
        private const val MAX_EXCEPTIONS = 3
    }
}
