package konem.netty.stream

import java.net.InetSocketAddress
import java.net.SocketAddress

interface HandlerListener {
  fun registerActiveHandler(channelPort: Int, remoteConnection: SocketAddress)
  fun registerInActiveHandler(channelPort: Int, remoteConnection: SocketAddress)
}
