package konem.example


import konem.data.json.Data
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.tcp.ConnectionListener
import konem.protocol.konem.json.KonemClientFactory
import konem.protocol.konem.json.KonemMessageReceiver
import konem.protocol.konem.json.KonemServer
import konem.protocol.string.StringClientFactory
import konem.protocol.string.StringMessageReceiver


fun main() {

    System.setProperty("konem.secure.keyStoreLocation","konem/config/keystore/konem.jks")
    System.setProperty("konem.secure.keyStoreType","JKS")
    System.setProperty("konem.secure.keyStorePassword","test123")

    println(KonemMessage(Heartbeat()))


    val server = KonemServer.create { config->

        config.addChannel(6060)
    }
    server.startServer()

    val clientFactory = KonemClientFactory.createDefault()

    val client = clientFactory.createClient("localhost", 6060)

    client.connect()

    client.registerChannelReceiverListener(KonemMessageReceiver { from, message ->
        println("Got $message from $from")

    })

    server.registerConnectionListener(ConnectionListener {
        Thread.sleep(1_000)
        println("XXX")
        server.sendMessage(it, KonemMessage(Data("Test Message")))

    })

    Thread.sleep(30_000)

   // client.disconnect()

    clientFactory.shutdown()

    Thread.sleep(1000)


    server.shutdownServer()

}
