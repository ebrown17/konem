package eb.netty.stream

import java.net.InetSocketAddress

interface Receiver<I> {

    fun handleChannelRead(addr: InetSocketAddress, msg: I)
}