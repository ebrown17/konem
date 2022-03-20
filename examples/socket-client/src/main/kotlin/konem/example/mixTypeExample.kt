package konem.example


import konem.netty.tcp.ConnectionListener

import konem.netty.tcp.client.ClientFactoryConfig
import konem.protocol.socket.string.StringClientFactory
import konem.protocol.socket.string.StringMessageReceiver


import konem.protocol.socket.string.StringServerBuilder



fun main() {




/*    val server = WireServer()
    server.addChannel(6060)
    server.startServer()

    server.registerChannelReadListener(WireMessageReceiver { from, message -> println("Got $message from $from") })

    val clientFactory = StringClientFactory(ClientFactoryConfig())

    val client = clientFactory.createClient("localhost", 6060)

    client.connect()

    server.registerConnectionListener(ConnectionListener {
        Thread.sleep(3_000)
        server.sendMessage(it, KonemMessage(
            messageType = MessageType.DATA,
            data = Data("First Message")
        ))

    })

        Thread.sleep(30_000)

        client.disconnect()

        Thread.sleep(1000)


        server.shutdownServer()*/

    val server = StringServerBuilder().createServer()

    server.addChannel(6060)
    server.startServer()

    val clientFactory = StringClientFactory(ClientFactoryConfig())

    val client = clientFactory.createClient("localhost", 6060)


    client.connect()

    client.registerChannelReceiverListener(StringMessageReceiver {  from, message ->
        println("Got $message from $from")

    })

    server.registerConnectionListener(ConnectionListener {
        Thread.sleep(3_000)
        println("XXX")
        server.sendMessage(it,"Test msg")

    })

    Thread.sleep(30_000)

    client.disconnect()

    Thread.sleep(1000)


    server.shutdownServer()

}
