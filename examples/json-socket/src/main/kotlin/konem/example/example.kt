package konem.example

import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.ConnectionListener


import konem.protocol.konem.KonemJsonMessageReceiver
import konem.protocol.konem.json.JsonClientFactory
import konem.protocol.konem.json.JsonServer


fun main(){
    val server = JsonServer.create { config ->
        config.addChannel(6069)
    }


    var count = 0
    server.registerChannelReceiveListener(KonemJsonMessageReceiver{ from, msg ->
        println("SERVER Msg: $msg from $from")
        Thread.sleep(500)

        server.sendMessage(from,
            KonemMessage(message = Data("Send message ${count++}")
            ))
    })


    server.startServer()

    val clientFactory = JsonClientFactory.createDefault()

    val client = clientFactory.createClient("localhost", 6069)
    client.connect()

    client.registerConnectionListener(ConnectionListener {
        client.sendMessage(KonemMessage(message = Data("Send message ${count++}") ))
    })

    client.registerChannelReceiveListener(KonemJsonMessageReceiver{from, msg ->
        println("CLIENT Msg: $msg from $from")


        if(count < 10) {
            client.sendMessage( KonemMessage(message = Data("Send message ${count++}")))
        }
    })

    Thread.sleep(8000)

    client.disconnect()

    Thread.sleep(1000)

    println(client.toString())

    server.shutdownServer()
}
