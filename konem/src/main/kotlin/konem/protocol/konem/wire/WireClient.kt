package konem.protocol.konem.wire

import konem.data.protobuf.KonemMessage
import konem.logger
import konem.netty.tcp.Receiver
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientInternal
import kotlinx.coroutines.launch
import java.net.SocketAddress

class WireClient(private val serverAddress: SocketAddress, config: ClientBootstrapConfig<KonemMessage>):
    ClientInternal<KonemMessage>(serverAddress,config) {

    private val logger = logger(javaClass)
    private val transceiver = config.transceiver
    private val receiveListeners: ArrayList<Receiver<KonemMessage>> = ArrayList()

    override fun sendMessage(message: KonemMessage) {
        if (!isActive()) {
            logger.warn("attempted to send data on null or closed channel")
            return
        }
        logger.info("remote: {} message: {}", channel?.remoteAddress(), message)
        transceiver.transmit(serverAddress, message)
    }

    override fun registerChannelReceiverListener(receiver: Receiver<KonemMessage>) {
        receiveListeners.add(receiver)
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: KonemMessage) {
        clientScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: KonemMessage) {
        logger.trace("got message: {}", message)
        for (listener in receiveListeners) {
            listener.handle(addr, message)
        }
    }

    override fun toString(): String {
        return "WireClient{Transceiver=$transceiver}"
    }

}
