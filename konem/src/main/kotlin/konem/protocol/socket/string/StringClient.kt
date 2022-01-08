package konem.protocol.socket.string

import konem.logger
import konem.netty.tcp.Receiver
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientInternal
import kotlinx.coroutines.launch
import java.net.SocketAddress

class StringClient(private val serverAddress: SocketAddress, config: ClientBootstrapConfig<String>):
    ClientInternal<String>(serverAddress,config), StringChannelReceiver {

    private val logger = logger(javaClass)
    private val transceiver = config.transceiver
    private val receiveListeners: ArrayList<Receiver<String>> = ArrayList()

    override fun sendMessage(message: String) {
        if (!isActive()) {
            logger.warn("attempted to send data on null or closed channel")
            return
        }
        logger.trace("remote: {} message: {}", channel?.remoteAddress(), message)
        transceiver.transmit(serverAddress, message)
    }

    override fun registerChannelReceiverListener(receiver: Receiver<String>) {
        receiveListeners.add(receiver)
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: String) {
        clientScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: String) {
        logger.trace("got message: {}", message)
        for (listener in receiveListeners) {
            listener.handle(addr, message)
        }
    }

    override fun toString(): String {
        return "StringClient{Transceiver=$transceiver}"
    }

}
