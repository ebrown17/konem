package konem.protocol.websocket.json

import io.netty.bootstrap.ServerBootstrap
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import konem.data.json.KonemMessage
import konem.netty.stream.*
import konem.netty.stream.server.Server
import konem.netty.stream.server.ServerTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class WebSocketServer : Server<WebSocketFrame,WebSocketFrame>(), ServerTransmitter<KonemMessage>,
  WebSocketServerChannelReader {

  private val logger = LoggerFactory.getLogger(WebSocketServer::class.java)

  private val readListenerMap: ConcurrentHashMap<Int, ConcurrentHashMap<String, ArrayList<Receiver>>> =
    ConcurrentHashMap()
  private val websocketMap: ConcurrentHashMap<Int, Array<String>> = ConcurrentHashMap()


  private  val pathConnectionListeners: MutableList<WsConnectListener> = ArrayList()
  private val pathDisconnectionListeners: MutableList<WsDisconnectListener> = ArrayList()

  override fun addChannel(port: Int, vararg websocketPaths: String): Boolean {
    if (isPortConfigured(port)) {
      logger.warn("port {} already in use; not creating channel", port)
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
      val added = addChannel(port, transceiver as Transceiver<WebSocketFrame,WebSocketFrame>)
      if (added) {
        if (readListenerMap[port] == null) {
          readListenerMap[port] = ConcurrentHashMap()
        }
      } else {
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
    val channel = WebSocketServerChannel(
      transceiver as WebSocketTransceiver,
      *websocketPaths!!
    )
    return createServerBootstrap(channel)
  }

  override fun handleChannelRead(addr: SocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    serverScope.launch {
      readMessage(addr, channelPort, webSocketPath, message)
    }
  }

  override suspend fun readMessage(addr: SocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    logger.trace("got message: {}, addr: {} readListenerMap: {} ", message, addr, readListenerMap)
    val readListeners = readListenerMap[channelPort]
    if (readListeners != null) {
      val readerListenerList = readListeners[webSocketPath]
      if (readerListenerList != null) {
        for (listener in readerListenerList) {
          listener.handle(addr, message)
        }
      }
    }
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    for (readListeners in readListenerMap.values) {
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
  }

  override fun registerChannelReadListener(receiver: Receiver, vararg webSocketPaths: String) {
    require(webSocketPaths.isNotEmpty()) { "webSocketPaths type can't be null or empty" }

    for (path in webSocketPaths) {
      for (configuredPaths in websocketMap.values) {
        if (!configuredPaths.contains(path)) {
          continue
        }
        logger.info("{}", path)
        for (readListeners in readListenerMap.values) {
          var readerListenerList: ArrayList<Receiver>? = readListeners[path]
          if (readerListenerList == null) {
            readerListenerList = arrayListOf()
          }
          readerListenerList.add(receiver)
          readListeners[path] = readerListenerList
        }
      }
    }
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver) {
    val readListener = readListenerMap[port]
    if (readListener != null) {
      for (websocketPaths in websocketMap.values) {
        for (path in websocketPaths) {
          var readerListenerList: ArrayList<Receiver>? = readListener[path]
          if (readerListenerList == null) {
            readerListenerList = ArrayList()
          }
          readerListenerList.add(receiver)
          readListener[path] = readerListenerList
        }
      }
    }
  }

  override fun registerChannelReadListener(port: Int, receiver: Receiver, vararg webSocketPaths: String) {
    require(webSocketPaths.isNotEmpty()) { "webSocketPaths type can't be null or empty" }

    for (path in webSocketPaths) {
      for (configuredPaths in websocketMap.values) {
        if (!configuredPaths.contains(path)) {
          continue
        }
        logger.info("{}", path)
        val readListeners = readListenerMap[port]
        if (readListeners != null) {
          var readerListenerList: ArrayList<Receiver>? = readListeners[path]
          if (readerListenerList == null) {
            readerListenerList = arrayListOf()
          }
          readerListenerList.add(receiver)
          readListeners[path] = readerListenerList
        }
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

  override fun sendMessage(addr: SocketAddress, message: KonemMessage) {
    val channelPort = getRemoteHostToChannelMap()[addr]
    if (channelPort != null) {
      val transceiver = getTransceiverMap()[channelPort] as WebSocketTransceiver
      transceiver.transmit(addr, message)
    }
  }

  fun registerPathConnectionListener(listener: WebSocketConnectionListener) {
    pathConnectionListeners.add(listener)
  }

  fun registerPathDisconnectionListener(listener: WebSocketDisconnectionListener) {
    pathDisconnectionListeners.add(listener)
  }

  fun registerPathConnectionStatusListener(listener: WebSocketConnectionStatusListener) {
    pathConnectionListeners.add(listener)
    pathDisconnectionListeners.add(listener)
  }

  private fun onPathConnect(remoteConnection: InetSocketAddress, paths: String) {
    for (listener in pathConnectionListeners) {
      listener.onConnection(remoteConnection, paths)
    }
  }

  private fun onPathDisconnect(remoteConnection: InetSocketAddress, paths: String) {
    for (listener in pathDisconnectionListeners) {
      listener.onDisconnection(remoteConnection, paths)
    }
  }


  override fun connectionActive(handler: Handler<WebSocketFrame,WebSocketFrame>) {
    val wHandler = handler as WebSocketFrameHandler
    onPathConnect(wHandler.remoteAddress as InetSocketAddress, wHandler.webSocketPath)
    for (listener in connectionListeners) {
      listener.onConnection(wHandler.remoteAddress)
    }
  }

  override fun connectionInActive(handler: Handler<WebSocketFrame,WebSocketFrame>) {
    val wHandler = handler as WebSocketFrameHandler
    onPathDisconnect(wHandler.remoteAddress as InetSocketAddress, wHandler.webSocketPath)
    for (listener in disconnectionListeners) {
      listener.onDisconnection(wHandler.remoteAddress)
    }

  }

}
