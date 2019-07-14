package konem.protocol.websocket

import io.netty.bootstrap.ServerBootstrap
import konem.data.json.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.server.Server
import konem.netty.stream.server.ServerTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

class WebSocketServer : Server(), ServerTransmitter<KonemMessage> {

  private val logger = LoggerFactory.getLogger(WebSocketServer::class.java)

  private val readListeners: ConcurrentHashMap<String, ArrayList<Receiver>> =
    ConcurrentHashMap()
  private val websocketMap: ConcurrentHashMap<Int, Array<String>> = ConcurrentHashMap()

  override fun addChannel(port: Int, vararg websocketPaths: String): Boolean {
    if (isPortConfigured(port)) {
      logger.warn("addChannel port {} already in use; not creating channel", port)
      return false
    }

    val validPaths = hashSetOf(*websocketPaths)

    for (path in websocketPaths) {
      if (isPathConfiguredOnPort(port, path)) {
        validPaths.remove(path)
      }
    }

    return if (validPaths.isNotEmpty()) {
      val transceiver = WebSocketTransceiver(port)
      websocketMap.putIfAbsent(port, validPaths.toTypedArray())
      val added = addChannel(port, transceiver)
      if (!added) {
        websocketMap.remove(port)
      }
      added
    } else {
      false
    }
  }

  override fun createServerBootstrap(port: Int): ServerBootstrap {
    val transceiver = getTransceiverMap()[port]
    val websocketPaths = websocketMap[port]
    val channel = WebSocketServerChannel(transceiver as WebSocketTransceiver, *websocketPaths!!)
    return createServerBootstrap(channel)
  }

  override fun handleChannelRead(addr: InetSocketAddress, webSocketPath: String, message: Any) {
    serverScope.launch {
      readMessage(addr, webSocketPath, message)
    }
  }

  override suspend fun readMessage(addr: InetSocketAddress, webSocketPath: String, message: Any) {
    logger.trace("readMessage got message: {}", message)
    val readerListenerList = readListeners[webSocketPath]
    if (readerListenerList != null) {
      for (listener in readerListenerList) {
        listener.handleChannelRead(addr, message)
      }
    }
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    for (websocketPaths in websocketMap.values) {
      for (path in websocketPaths) {
        var readerListenerList: ArrayList<Receiver>? = readListeners[path]
        if (readerListenerList == null) {
          readerListenerList = ArrayList()
        }
        readerListenerList.add(receiver)
        readListeners[path] = readerListenerList
      }
    }
  }

  override fun registerChannelReadListener(vararg webSocketPaths: String, receiver: Receiver) {
    if (webSocketPaths.isEmpty()) {
      throw IllegalArgumentException("webSocketPaths type can't be null or empty")
    }

    for (path in webSocketPaths) {
      for (configuredPaths in websocketMap.values) {
        if (!configuredPaths.contains(path)) {
          continue
        }
        logger.info("registerChannelReadListener for {}", path)
        var readerListenerList: ArrayList<Receiver>? = readListeners[path]
        if (readerListenerList == null) {
          readerListenerList = arrayListOf()
        }
        readerListenerList.add(receiver)
        readListeners[path] = readerListenerList
      }
    }
  }

  private fun isPathConfiguredOnPort(port: Int, path: String): Boolean {
    val configuredPaths = websocketMap[port]
    return configuredPaths?.contains(path) ?: false
  }

  override fun broadcastOnChannel(port: Int, message: KonemMessage, vararg webSocketPaths: String) {
    val transceiver = getTransceiverMap()[port] as WebSocketTransceiver
    transceiver.broadcastMessage(message, *webSocketPaths)
  }

  override fun broadcastOnAllChannels(message: KonemMessage, vararg webSocketPaths: String) {
    val transceiverMap = getTransceiverMap()
    for (transceiver in transceiverMap.values) {
      transceiver as WebSocketTransceiver
      transceiver.broadcastMessage(message, *webSocketPaths)
    }
  }

  override fun sendMessage(addr: InetSocketAddress, message: KonemMessage) {
    val channelPort = getRemoteHostToChannelMap()[addr]
    val transceiver = getTransceiverMap()[channelPort] as WebSocketTransceiver
    transceiver.transmit(addr, message)
  }
}
