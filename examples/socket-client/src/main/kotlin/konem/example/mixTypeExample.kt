package konem.example


import konem.netty.tcp.ConnectionListener

import konem.protocol.string.StringClientFactory
import konem.protocol.string.StringMessageReceiver
import konem.protocol.string.StringServer


fun main() {

    System.setProperty("konem.secure.keyStoreLocation","konem/config/keystore/konem.jks")
    System.setProperty("konem.secure.keyStoreType","JKS")
    System.setProperty("konem.secure.keyStorePassword","test123")

    println(System.getProperty("user.dir"))

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

    Thread.sleep(30_000)

   // client.disconnect()

    clientFactory.shutdown()

    Thread.sleep(1000)


    server.shutdownServer()

}
