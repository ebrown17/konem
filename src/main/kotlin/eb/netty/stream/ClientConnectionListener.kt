package eb.netty.stream


import eb.netty.stream.client.Client
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener

import java.util.concurrent.TimeUnit

class ClientConnectionListener internal constructor(private val client: Client) : ChannelFutureListener {
    internal var isAttemptingConnection = true
        private set

    @Throws(Exception::class)
    override fun operationComplete(future: ChannelFuture) {
        if (future.isSuccess) {
            clearAttemptingConnection()
            client.connectionEstablished(future)
        } else {
            future.channel().close()
            future.channel().eventLoop().schedule({
                try {
                    clearAttemptingConnection()
                    client.connect()
                } catch (e: InterruptedException) {
                    // TODO test to see what happens if this is reached
                    throw RuntimeException("ClientConnectionListener interrupted while trying to connect")
                }
            }, client.calculateRetryTime(), TimeUnit.SECONDS)
        }

    }

    internal fun setAttemptingConnection() {
        isAttemptingConnection = true
    }

    private fun clearAttemptingConnection() {
        isAttemptingConnection = false
    }

}
