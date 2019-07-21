package konem.netty.stream

import java.net.InetSocketAddress

interface Receiver {
  fun handleChannelRead(addr: InetSocketAddress, msg: Any)
}
