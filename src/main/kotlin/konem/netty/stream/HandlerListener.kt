package konem.netty.stream

import java.net.InetSocketAddress

interface HandlerListener {
  fun registerActiveHandler(channelPort: Int, remoteConnection: InetSocketAddress)
  fun registerInActiveHandler(channelPort: Int, remoteConnection: InetSocketAddress)
}
