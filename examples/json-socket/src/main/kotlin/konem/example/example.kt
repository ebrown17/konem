package konem.example


import konem.Konem
import konem.data.json.Data
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.*
import konem.protocol.konem.KonemProtocolPipeline
import java.lang.Thread.sleep


fun main(){

    var count = 0

    val server = Konem.createTcpServer(
        config = { serverConfig ->
            serverConfig.addChannel(6160)
        },
        heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) },
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )

    server.startServer()

    server.registerChannelReceiveListener(MessageReceiver{ from, msg ->
        println("SERVER Msg: $msg from $from")
        sleep(500)
        server.sendMessage(from,KonemMessage(message = Data("Send message ${count++}")))

    })


    sleep(1000)

    val clientFactory = Konem.createClientFactoryOfDefaults(
        heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
            message is Heartbeat
        }),
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )

    val client = clientFactory.createClient("localhost",6160)
    client.connect()

    client.registerConnectionListener(ConnectionListener {
        client.sendMessage(KonemMessage(message = Data("Send message ${count++}") ))
    })


    client.registerChannelReceiveListener(MessageReceiver{ from, msg ->
        println("CLIENT Msg: $msg from $from")


        if(count < 10) {
            client.sendMessage( KonemMessage(message = Data("Send message ${count++}")))
        }
    })

    sleep(8000)

    client.disconnect()

    sleep(1000)

    println(client.toString())

    server.shutdownServer()
}
