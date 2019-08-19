package konem.protocol.wire

import io.netty.bootstrap.ServerBootstrap
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.KonemProtoMessage
import konem.netty.stream.Receiver
import konem.netty.stream.server.Server
import konem.netty.stream.server.ServerTransmitter
import konem.protocol.websocket.WebSocketTransceiver
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class ProtobufServer : Server(), ServerTransmitter<KonemMessage>, ProtobufChannelReader {

  private val logger = LoggerFactory.getLogger(ProtobufServer::class.java)

  private val readListeners: ConcurrentHashMap<Int, ArrayList<Receiver>> =
    ConcurrentHashMap()

  override fun addChannel(port: Int, vararg args: String): Boolean {
    if (isPortConfigured(port)) {
      logger.warn("addChannel port {} already in use; not creating channel", port)
      return false
    }

    val transceiver = ProtobufTransceiver(port)

    return if(addChannel(port, transceiver)){
      readListeners[port] = ArrayList()
      true
    } else{
      false
    }
  }

  override fun createServerBootstrap(port: Int): ServerBootstrap {
    val transceiver = getTransceiverMap()[port]
    val channel = ProtobufServerChannel(transceiver as ProtobufTransceiver)
    return createServerBootstrap(channel)
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver) {
    if (!isPortConfigured(port)) {
      throw IllegalArgumentException("port type can't be null or port is not configured: port " + port)
    }

    var readerListenerList = readListeners[port]
    if (readerListenerList == null) {
      readerListenerList = arrayListOf()
    }
    readerListenerList.add(receiver)
    readListeners[port] = readerListenerList;

  }

  override fun registerChannelReadListener(receiver: Receiver) {
    for (list in readListeners.values) {
      list.add(receiver)
    }
  }

  override fun registerChannelReadListener(receiver: Receiver, vararg args: String) {
    registerChannelReadListener(receiver)
  }

  override fun broadcastOnChannel(port: Int, message: KonemMessage, vararg args: String) {
    val transceiver = getTransceiverMap()[port] as ProtobufTransceiver
    transceiver.broadcastMessage(message)
  }

  override fun broadcastOnAllChannels(message: KonemMessage, vararg args: String) {
    val transceiverMap = getTransceiverMap()
    for (transceiver in transceiverMap.values) {
      transceiver as ProtobufTransceiver
      transceiver.broadcastMessage(message)
    }
  }

  override fun sendMessage(addr: InetSocketAddress, message: KonemMessage) {
    val channelPort = getRemoteHostToChannelMap()[addr]
    if (channelPort != null) {
      val transceiver = getTransceiverMap()[channelPort] as ProtobufTransceiver
      transceiver.transmit(addr, message)
    }
  }

  override fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any) {
    serverScope.launch {
      readMessage(addr, port, message)
    }
  }

  override suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any) {
    logger.trace("readMessage got message: {}", message)
    val readerListenerList = readListeners[port]
    if (readerListenerList != null) {
      for (listener in readerListenerList) {
        listener.handleChannelRead(addr, message)
      }
    }
  }
}
