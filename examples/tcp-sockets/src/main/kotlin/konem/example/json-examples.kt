package konem.example


import konem.Konem
import konem.data.json.Data
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.logger
import konem.netty.*
import konem.protocol.konem.KonemProtocolPipeline
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep


fun main(){

    val logger = LoggerFactory.getLogger("main")
    var count = 0

    logger.info("Creating Server")
    val server = Konem.createTcpSocketServer(
        config = {
            addChannel(6160)
            addChannel(6161)
        },
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
        heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) }
    )

    logger.info("Registering channel message receiver for port 6160")
    server.registerChannelMessageReceiver(6160,MessageReceiver{ from, msg ->
        logger.info("Received message: $msg on port 6160 receiver")
        sleep(500)
        server.sendMessage(from,KonemMessage(message = Data("Received $msg on port 6160 receiver")))
    })
    logger.info("Registering channel message receiver for port 6161")
    server.registerChannelMessageReceiver(6161,MessageReceiver{ from, msg ->
        logger.info("Received message: $msg on port 6161 receiver")
        sleep(500)
        server.sendMessage(from,KonemMessage(message = Data("Received $msg on port 6161 receiver")))
    })
    logger.info("Registering channel message receiver for all ports")
    server.registerChannelMessageReceiver(MessageReceiver{ from, msg ->
        logger.info("Received message: $msg channel wide receiver")
        sleep(500)
        server.sendMessage(from,KonemMessage(message = Data("Received $msg on channel wide receiver")))
    })

    logger.info("Starting server")
    server.startServer()
    sleep(1000)

    val clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
        heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
            message is Heartbeat
        }),
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )

    val client = clientFactory.createClient("localhost",6160)

    client.registerConnectionListener(ConnectionListener {
        logger.info("Connection established")
        client.sendMessage(KonemMessage(message = Data("Send message ${count++}") ))
    })

    client.registerChannelMessageReceiver(MessageReceiver{ from, msg ->
        logger.info("received Msg: $msg from $from")
    })

    client.connect()
    sleep(2000)
    repeat(10) {
        client.sendMessage(KonemMessage(message = Data("Send message ${count++}")))
        sleep(1000)
    }

    logger.info("Should see heartbeat messages after 15 seconds")

    sleep(20_000)

    client.disconnect()

    sleep(1000)

    println(client.toString())

    server.shutdownServer()
}
