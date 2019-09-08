package konem.netty.stream

import java.net.InetSocketAddress

interface Receiver {
  fun receive(addr: InetSocketAddress, msg: Any)
}
