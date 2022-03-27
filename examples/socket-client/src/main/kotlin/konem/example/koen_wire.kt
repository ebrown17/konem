package konem.example


import konem.data.protobuf.Data
import konem.data.protobuf.HeartBeat
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.tcp.ConnectionListener
import konem.protocol.konem.wire.WireClientFactory
import konem.protocol.konem.wire.WireMessageReceiver
import konem.protocol.konem.wire.WireServer
import java.util.*


fun main() {

    System.setProperty("konem.secure.keyStoreLocation","konem/config/keystore/konem.jks")
    System.setProperty("konem.secure.keyStoreType","JKS")
    System.setProperty("konem.secure.keyStorePassword","test123")

    println(
        KonemMessage(
        messageType = MessageType.HEARTBEAT,
        heartBeat = HeartBeat(Date().toString())
        )
    )


    val server = WireServer.create { config->

        config.addChannel(6060)
    }
    server.startServer()

    val clientFactory = WireClientFactory.createDefault()

    val client = clientFactory.createClient("localhost", 6060)

    client.connect()

    client.registerChannelReceiverListener(WireMessageReceiver { from, message ->
        println("Got $message from $from")

    })

    server.registerConnectionListener(ConnectionListener {
        Thread.sleep(1_000)
        println("XXX")
        server.sendMessage(it,         KonemMessage(
            messageType = MessageType.DATA,
            data = Data("Test Message")
        ))

    })

    Thread.sleep(30_000)

    // client.disconnect()

    clientFactory.shutdown()

    Thread.sleep(1000)


    server.shutdownServer()

}

