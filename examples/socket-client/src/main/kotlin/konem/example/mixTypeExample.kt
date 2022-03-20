package konem.example


import konem.netty.tcp.ConnectionListener

import konem.protocol.socket.string.StringClientFactory
import konem.protocol.socket.string.StringMessageReceiver
import konem.protocol.socket.string.StringServer


fun main() {

    val server = StringServer.create { config->
        config.addChannel(6060)
    }
    server.startServer()

    val clientFactory = StringClientFactory.createDefault()

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

    Thread.sleep(10_000)

    client.disconnect()

    clientFactory.shutdown()

    Thread.sleep(1000)


    server.shutdownServer()

}
