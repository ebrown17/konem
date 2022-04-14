package konem.example


import konem.Konem
import konem.data.json.Data
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ConnectionStatusListener
import konem.netty.ServerHeartbeatProtocol
import konem.protocol.konem.KonemProtocolPipeline
import java.lang.Thread.sleep


fun main(){

    val server = Konem.createTcpSocketServer<KonemMessage>(
        config = { serverConfig ->
            serverConfig.addChannel(6160)
        },
        heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) },
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )

    server.startServer()

    server.registerConnectionStatusListener(
        ConnectionStatusListener(
            connected = { addr ->
                println("Connection from $addr")
                sleep(1000)
                server.sendMessage(addr, KonemMessage(Data("You just connected!")))
            },
            disconnected = {
                println("Disconnection from $it")
            })
    )

    sleep(1000)

    val clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
        heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
            message is KonemMessage && (message.message is Heartbeat)
         }),
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
        )

    val client = clientFactory.createClient("localhost",6160)
    val client2 = clientFactory.createClient("localhost",6160)
    client.connect()
    client2.connect()
    sleep(15_000L)

    client.disconnect()
    client2.disconnect()

}
