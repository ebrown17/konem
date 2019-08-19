package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class ProtobufClient(private val serverAddress: InetSocketAddress, config: ClientBootstrapConfig) :
  Client(serverAddress, config), ClientTransmitter<KonemMessage>, ProtobufChannelReader {

  private val logger = LoggerFactory.getLogger(ProtobufClient::class.java)
  private val transceiver = config.transceiver as ProtobufTransceiver
  val readListeners = mutableListOf<Receiver>()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("sendMessage attempted to send data on null or closed channel")
      return
    }
    logger.trace("sendMessage remote: {} message: {}", channel?.remoteAddress(), message)
    transceiver.transmit(serverAddress, message)
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    readListeners.add(receiver)
  }

  override fun registerChannelReadListener(receiver: Receiver, vararg args: String) {
    registerChannelReadListener(receiver)
  }

  override fun handleChannelRead(addr: InetSocketAddress,port: Int, message: Any) {
    clientScope.launch {
      readMessage(addr,port, message)
    }
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver) {

  }

  override suspend fun readMessage(addr: InetSocketAddress,port: Int, message: Any) {
    logger.trace("readMessage got message: {}", message)
    for (listener in readListeners) {
      listener.handleChannelRead(addr, message)
    }
  }

  override fun toString(): String {
    return "ProtobufClient{Transceiver=$transceiver}"
  }
}
