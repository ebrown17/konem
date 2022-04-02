package konem.example


import konem.Konem
import konem.data.json.Data
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ConnectionStatusListener
import konem.netty.ServerHeartbeatProtocol
import konem.netty.client.ClientFactoryConfig
import konem.protocol.konem.json.KonemJsonPipeline
import java.lang.Thread.sleep


fun main(){

    val server = Konem.createTcpServer<KonemMessage>(
        config = { serverConfig ->
            serverConfig.addChannel(6160)
        },
        ServerHeartbeatProtocol { KonemMessage(Heartbeat()) },
        KonemJsonPipeline.getKonemJsonPipeline()
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

    val clientFactory = Konem.createClientFactoryOfDefaults<KonemMessage> (
        heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
            when(message) {
                is Heartbeat -> true
                else -> false
            }
         }),
        protocolPipeline = KonemJsonPipeline.getKonemJsonPipeline()
        )

    val client = clientFactory.createClient("localhost",6160)

    client.connect()

    sleep(15_000L)

    client.disconnect()

    sleep(1000)
    client.connect()


}
