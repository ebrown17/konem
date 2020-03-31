package konem.protocol.socket.wire

import io.netty.bootstrap.ServerBootstrap
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap
import konem.data.protobuf.KonemMessage
import konem.netty.stream.Handler
import konem.netty.stream.Receiver
import konem.netty.stream.server.Server
import konem.netty.stream.server.ServerTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class WireServer : Server<KonemMessage,KonemMessage>(), ServerTransmitter<KonemMessage>, WireServerChannelReader {

  private val logger = LoggerFactory.getLogger(WireServer::class.java)

  private val receiveListeners: ConcurrentHashMap<Int, ArrayList<Receiver>> =
    ConcurrentHashMap()

  override fun addChannel(port: Int, vararg args: String): Boolean {
    if (isPortConfigured(port)) {
      logger.warn("port {} already in use; not creating channel", port)
      return false
    }

    val transceiver = WireTransceiver(port)

    return if (addChannel(port, transceiver)) {
      receiveListeners[port] = ArrayList()
      true
    } else {
      false
    }
  }

  override fun createServerBootstrap(port: Int): ServerBootstrap {
    val transceiver = getTransceiverMap()[port]
    val channel = WireServerChannel(transceiver as WireTransceiver)
    return createServerBootstrap(channel)
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver) {
    if (!isPortConfigured(port)) {
      throw IllegalArgumentException("port type can't be null or port is not configured: port $port")
    }

    var readerListenerList = receiveListeners[port]
    if (readerListenerList == null) {
      readerListenerList = arrayListOf()
    }
    readerListenerList.add(receiver)
    receiveListeners[port] = readerListenerList
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    for (list in receiveListeners.values) {
      list.add(receiver)
    }
  }

  override fun broadcastOnChannel(port: Int, message: KonemMessage, vararg args: String) {
    val transceiver = getTransceiverMap()[port] as WireTransceiver
    transceiver.broadcastMessage(message)
  }

  override fun broadcastOnAllChannels(message: KonemMessage, vararg args: String) {
    val transceiverMap = getTransceiverMap()
    for (transceiver in transceiverMap.values) {
      transceiver as WireTransceiver
      transceiver.broadcastMessage(message)
    }
  }

  override fun sendMessage(addr: SocketAddress, message: KonemMessage) {
    val channelPort = getRemoteHostToChannelMap()[addr]
    if (channelPort != null) {
      val transceiver = getTransceiverMap()[channelPort] as WireTransceiver
      transceiver.transmit(addr, message)
    }
  }

  override fun handleChannelRead(addr: SocketAddress, port: Int, message: Any) {
    serverScope.launch {
      readMessage(addr, port, message)
    }
  }

  override suspend fun readMessage(addr: SocketAddress, port: Int, message: Any) {
    logger.trace("got message: {}", message)
    val readerListenerList = receiveListeners[port]
    if (readerListenerList != null) {
      for (listener in readerListenerList) {
        listener.handle(addr, message)
      }
    }
  }

  override fun connectionActive(handler: Handler<KonemMessage,KonemMessage>) {
    for (listener in connectionListeners) {
      listener.onConnection(handler.remoteAddress)
    }
  }

  override fun connectionInActive(handler: Handler<KonemMessage,KonemMessage>) {
    for (listener in disconnectionListeners) {
      listener.onDisconnection(handler.remoteAddress)
    }
  }
}
