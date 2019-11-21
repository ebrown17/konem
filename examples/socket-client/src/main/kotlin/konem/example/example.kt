package konem.example

import konem.data.protobuf.Data
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.stream.ConnectionListener
import konem.protocol.socket.wire.WireClientFactory
import konem.protocol.socket.wire.WireMessageReceiver
import konem.protocol.socket.wire.WireServer


fun main() {

  val server = WireServer()
  server.addChannel(6060)
  server.startServer()

  server.registerChannelReadListener(WireMessageReceiver { from, message -> println("Got $message from $from") })

  val clientFactory = WireClientFactory()

  val client = clientFactory.createClient("localhost", 6060)

  client.connect()


  client.registerConnectionListener(ConnectionListener {

    client.sendMessage(
      KonemMessage(
        messageType = MessageType.DATA,
        data = Data("First Message")
      )
    )

    Thread.sleep(1000)

    client.disconnect()

    Thread.sleep(1000)


    server.shutdownServer()

  })


}
