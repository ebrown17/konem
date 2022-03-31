package konem.protocol.tcp

import konem.data.json.KonemMessage
import konem.logger
import konem.netty.Receiver
import konem.netty.client.ClientBootstrapConfig
import konem.netty.client.ClientInternal
import kotlinx.coroutines.launch
import java.net.SocketAddress

class TcpClient<I>(private val serverAddress: SocketAddress, config: ClientBootstrapConfig<I>):
    ClientInternal<I>(serverAddress,config) {

    private val logger = logger(javaClass)
    private val transceiver = config.transceiver
    private val receiveListeners: ArrayList<Receiver<I>> = ArrayList()

    override fun sendMessage(message: I) {
        if (!isActive()) {
            logger.warn("attempted to send data on null or closed channel")
            return
        }
        logger.info("remote: {} message: {}", channel?.remoteAddress(), message)
        transceiver.transmit(serverAddress, message)
    }

    override fun registerChannelReceiveListener(receiver: Receiver<I>) {
        receiveListeners.add(receiver)
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: I) {
        clientScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: I) {
        logger.trace("got message: {}", message)
        for (listener in receiveListeners) {
            listener.handle(addr, message)
        }
    }

    override fun toString(): String {
        return "TcpClient{Transceiver=$transceiver}"
    }

}
