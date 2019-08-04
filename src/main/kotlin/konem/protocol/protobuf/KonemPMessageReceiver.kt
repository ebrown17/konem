package konem.protocol.protobuf

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.protobuf.KonemPMessage
import konem.netty.stream.ReceiverHandler
import kotlinx.serialization.json.JsonParsingException
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

open class KonemPMessageReceiver(private val receive: (InetSocketAddress, KonemPMessage) -> Unit) :
  ReceiverHandler<Any>() {
  private val logger = LoggerFactory.getLogger(KonemPMessageReceiver::class.java)


  // TODO look at using channels to pass value from receiver
  override fun read(addr: InetSocketAddress, message: Any) {
    synchronized(this) {
        when(message){
          is KonemPMessage -> receive(addr, message)
          else ->{
            logger.error("read got unexpected message type: {} ", message.javaClass)
          }
        }

    }
  }
}
