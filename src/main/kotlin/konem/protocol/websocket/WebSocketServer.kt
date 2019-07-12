package konem.protocol.websocket

import io.netty.bootstrap.ServerBootstrap
import konem.netty.stream.Receiver
import konem.netty.stream.server.Server
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

class WebSocketServer : Server() {

  private val logger = LoggerFactory.getLogger(WebSocketServer::class.java)

  private val WS_MIN_SIZE = 1

  private val readListeners :ConcurrentHashMap<String, ArrayList<KonemMessageReceiver>> =  ConcurrentHashMap()
  private val websocketMap: ConcurrentHashMap<Int, HashSet<String>> =  ConcurrentHashMap()

  override fun addChannel(port: Int, vararg args: String): Boolean {


    if(args.isNotEmpty()){
      // need to check for duplicate ws
     // addChannel()
      return true
    }
    else{
      return false
    }

  }

  override fun createServerBootstrap(port: Int): ServerBootstrap {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun handleChannelRead(addr: InetSocketAddress, webSocketPath: String, message: Any) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun readMessage(addr: InetSocketAddress, webSocketPath: String, message: Any) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun registerChannelReadListener(receiver: Receiver<Any>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun registerChannelReadListener(vararg args: String, receiver: Receiver<Any>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private fun isWebSocketPathConfigured(vararg paths: String): Boolean {
    for (configuredPath in websocketMap.values) {
      for (path in paths) {
        if (configuredPath.contains(path)) {
          return true
        }
      }
    }
    return false
  }

}