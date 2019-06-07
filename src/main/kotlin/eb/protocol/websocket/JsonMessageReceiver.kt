package eb.protocol.websocket

import eb.netty.stream.ReceiverHandler
import java.net.InetSocketAddress

class JsonMessageReceiver(private val receive: (InetSocketAddress,String) -> Unit) : ReceiverHandler<String>(){
    override fun read(addr: InetSocketAddress, message: String) {
        receive(addr,message)
    }
}