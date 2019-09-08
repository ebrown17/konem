package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class WireClient(private val serverAddress: InetSocketAddress, config: ClientBootstrapConfig) :
  Client(serverAddress, config), ClientTransmitter<KonemMessage>, WireChannelReader {

  private val logger = LoggerFactory.getLogger(WireClient::class.java)
  private val transceiver = config.transceiver as WireTransceiver
  private val receiveListeners: ConcurrentHashMap<Int, ArrayList<Receiver>> =
    ConcurrentHashMap()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("sendMessage attempted to send data on null or closed channel")
      return
    }
    logger.trace("sendMessage remote: {} message: {}", channel?.remoteAddress(), message)
    transceiver.transmit(serverAddress, message)
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    for(receiveListenerList in receiveListeners.values){
      receiveListenerList.add(receiver)
    }
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver) {
    var receiveListenerList = receiveListeners[port]
    if (receiveListenerList == null) {
      receiveListenerList = arrayListOf()
    }
    receiveListenerList.add(receiver)
    receiveListeners[port] = receiveListenerList
  }

  override fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any) {
    clientScope.launch {
      readMessage(addr, port, message)
    }
  }

  override suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any) {
    logger.trace("readMessage got message: {}", message)
     val receiveListenerList =  receiveListeners[port]

    if (receiveListenerList != null) {
      for (listener in receiveListenerList) {
        listener.receive(addr, message)
      }
    }
  }

  override fun toString(): String {
    return "WireClient{Transceiver=$transceiver}"
  }
}
