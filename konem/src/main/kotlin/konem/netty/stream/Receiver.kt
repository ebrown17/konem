package konem.netty.stream

import java.net.InetSocketAddress

interface Receiver {
  fun handle(addr: InetSocketAddress, msg: Any)
}
