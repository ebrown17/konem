package konem.example


import konem.Konem
import konem.data.protobuf.Data
import konem.data.protobuf.HeartBeat
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.*
import konem.protocol.konem.KonemProtocolPipeline
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.time.LocalDateTime


fun main(){

    val logger = LoggerFactory.getLogger("main")
    var count = 0

    logger.info("Creating Server")
    val server = Konem.createTcpSocketServer(
        config = {
            addChannel(6160)
            addChannel(6161)
        },
        protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline(),
        heartbeatProtocol = ServerHeartbeatProtocol {
            KonemMessage(
                MessageType.HEARTBEAT,
                heartBeat = HeartBeat(LocalDateTime.now().toString())
            )
        },
    )

    logger.info("Registering channel message receiver for port 6160")
    server.registerChannelMessageReceiver(6160,MessageReceiver{ from, msg ->
        logger.info("Received message: $msg on port 6160 receiver")
        sleep(500)
        server.sendMessage(from,dataMessage(("Received $msg on port 6160 receiver")))
    })
    logger.info("Registering channel message receiver for port 6161")
    server.registerChannelMessageReceiver(6161,MessageReceiver{ from, msg ->
        logger.info("Received message: $msg on port 6161 receiver")
        sleep(500)
        server.sendMessage(from,dataMessage(("Received $msg on port 6161 receiver")))
    })
    logger.info("Registering channel message receiver for all ports")
    server.registerChannelMessageReceiver(MessageReceiver{ from, msg ->
        logger.info("Received message: $msg channel wide receiver")
        sleep(500)
        server.sendMessage(from,dataMessage(("Received $msg on channel wide receiver")))
    })

    logger.info("Starting server")
    server.startServer()
    sleep(1000)

    val clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
        heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
            message is HeartBeat
        }),
        protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline()
    )

    val client = clientFactory.createClient("localhost",6160)

    val client2 = clientFactory.createClient("localhost",6161)

    client.registerConnectionListener(ConnectionListener {
        logger.info("Connection established")
        client.sendMessage(dataMessage(("Send message ${count++}") ))
    })

    client.registerChannelMessageReceiver(MessageReceiver{ from, msg ->
        logger.info("received Msg: $msg from $from")
    })

    client.connect()
    client2.connect()
    sleep(2000)
    repeat(10) {
        client.sendMessage(dataMessage(("Send message ${count++}")))
        sleep(1000)
        client2.sendMessage(dataMessage(("Send message ${count}")))
        sleep(1000)
    }

    logger.info("Should see heartbeat messages after 15 seconds")

    sleep(20_000)

    client.disconnect()
    client2.disconnect()

    sleep(1000)

    println(client.toString())

    server.shutdownServer()
}

fun dataMessage(message: String): KonemMessage {
    return KonemMessage(messageType = MessageType.DATA,
        data_ = Data(message)
    )
}
