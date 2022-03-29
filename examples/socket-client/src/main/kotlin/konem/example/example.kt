package konem.example

import konem.data.protobuf.Data
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.tcp.ConnectionListener
import konem.protocol.konem.KonemWireMessageReceiver
import konem.protocol.konem.wire.WireClientFactory
import konem.protocol.konem.wire.WireServer


fun main() {

  val server = WireServer.create {
      it.addChannel(6060)
  }
  server.startServer()

  server.registerChannelReceiveListener(KonemWireMessageReceiver { from, message -> println("Got $message from $from") })

  val clientFactory = WireClientFactory.createDefault()

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
