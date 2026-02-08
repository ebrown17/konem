package konem.netty

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.embedded.EmbeddedChannel

class TransceiverCleanupSpec : FunSpec({

    test("channel receivers are removed when handlers go inactive") {
        val transceiver = TestTransceiver()

        repeat(50) {
            val handler = TestHandler(transceiver)
            val channel = EmbeddedChannel(handler)
            channel.pipeline().fireChannelActive()

            val connectionKey = handler.connectionKey
            transceiver.registerChannelReceiver(connectionKey, TestChannelReceiver())

            transceiver.activeCount() shouldBe 1
            transceiver.receiverCount() shouldBe 1

            channel.close().syncUninterruptibly()

            transceiver.activeCount() shouldBe 0
            transceiver.receiverCount() shouldBe 0
        }
    }
})

private class TestTransceiver : Transceiver<String>(9999) {
    override fun transmit(connectionKey: ConnectionKey, message: String) {}
    override fun receive(connectionKey: ConnectionKey, message: String, extra: String) {}

    fun activeCount(): Int = activeHandlers.size
    fun receiverCount(): Int = channelReceiver.size
}

private class TestHandler(transceiver: Transceiver<String>) : Handler<String>(transceiver) {
    override fun channelRead0(p0: ChannelHandlerContext?, p1: String) {}
}

private class TestChannelReceiver : ChannelReceiver<String> {
    override fun handleReceivedMessage(connectionKey: ConnectionKey, port: Int, message: String, extra: String) {}
    override suspend fun receiveMessage(connectionKey: ConnectionKey, port: Int, message: String, extra: String) {}
}
