package konem.example

import konem.Konem
import konem.data.protobuf.Data
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.ConnectionListener
import konem.netty.DisconnectionListener
import konem.netty.MessageReceiver
import konem.protocol.konem.KonemProtocolPipeline
import konem.protocol.websocket.WebSocketConnectionStatusListener
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.net.SocketAddress

private val logger = LoggerFactory.getLogger("Main")
fun main(){

    val server = Konem.createWebSocketServer(
        config = {
            addChannel(8080,"/tester")
        },
        protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline()
    )

    server.startServer()
    var count = 0

    sleep(5000)

    server.registerChannelMessageReceiver(MessageReceiver { from, message ->
        logger.info("Server KoneMessageReceiver: got {} from {} ", message,from)
        sleep(3000)
        server.broadcastOnAllChannels(dataMessage("${count++}"))
    },"/tester")

    server.registerPathConnectionStatusListener(
        WebSocketConnectionStatusListener(
            connected = { remoteAddr, wsPath ->
                logger.info("Connection from {} to path {}", remoteAddr, wsPath)
            }, disconnected = { remoteAddr, wsPath ->
                logger.info("Disconnection from  {} to path {}", remoteAddr, wsPath)
            }
        ))



    val fact = Konem.createWebSocketClientFactoryOfDefaults(KonemProtocolPipeline.getKonemWirePipeline())
    val client = fact.createClient("localhost", 8080, "/tester")
    val connectionListener = ConnectionListener { remoteAddr: SocketAddress ->
        logger.info("Client connected to {}", remoteAddr)
        sleep(2000)
        client.sendMessage(dataMessage("${count++}"))
    }

    client.registerConnectionListener(connectionListener)

    client.registerDisconnectionListener(DisconnectionListener { remoteAddr ->
        logger.info("Client {} disconnected from {}", client.toString(), remoteAddr)
    })

    client.connect()

    client.registerChannelMessageReceiver(MessageReceiver { from, msg ->
        logger.info("Client KonemMessageReceiver: got {} from {}", from, msg)
        sleep(3000)
        client.sendMessage(dataMessage("${count++}"))
    })


    sleep(60_000)

    client.disconnect()

    sleep(5000)
    println(count)
    server.shutdownServer()
}
fun dataMessage(message: String): KonemMessage {
    return KonemMessage(
        messageType = MessageType.DATA,
        data_ = Data(message)
    )
}
