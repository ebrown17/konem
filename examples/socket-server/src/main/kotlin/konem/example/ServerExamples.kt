package konem.example

import konem.data.protobuf.Data
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.stream.ConnectionListener
import konem.protocol.socket.wire.WireClientFactory
import konem.protocol.socket.wire.WireMessageReceiver
import konem.protocol.socket.wire.WireServer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("wireServerExamples")

fun main(){
  wireServerExamples()
}


fun wireServerExamples() {
  val server = WireServer()
  server.addChannel(8085)
  server.startServer()

  server.registerChannelReadListener(WireMessageReceiver { _, konemMessage ->
    logger.info("KoneMessageReceiver: {} ", konemMessage.toString())
  })

  val client = WireClientFactory().createClient("localhost", 8085)
  client.connect()

  client.registerConnectionListener(ConnectionListener {
    logger.info("Client connected")

    client.sendMessage(wireMessage("Client connected to $it"))
    Thread.sleep(1000)

    repeat(10) { time ->
      client.sendMessage(wireMessage("Client send $time"))
      Thread.sleep(1000)
    }
    client.disconnect()
    server.shutdownServer()

  })
}

fun wireMessage(test: String): KonemMessage {
  return KonemMessage(
    messageType = MessageType.DATA,
    data = Data(test)
  )
}
