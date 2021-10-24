package konem.example

import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.data.json.Message

import konem.netty.stream.ConnectionListener
import konem.protocol.socket.json.JsonClientFactory
import konem.protocol.socket.json.JsonServer
import konem.protocol.socket.json.KonemMessageReceiver

fun main(){
  val server = JsonServer()
  server.addChannel(6069)

  var count = 0
  server.registerChannelReadListener(KonemMessageReceiver{from, msg ->
    println("SERVER Msg: $msg from $from")
    Thread.sleep(500)

    server.sendMessage(from,
      KonemMessage(message = Data("Send message ${count++}")
    ))
  })


  server.startServer()

  val clientFactory = JsonClientFactory()

  val client = clientFactory.createClient("localhost", 6069)

  client.connect()

  client.registerConnectionListener(ConnectionListener {
    client.sendMessage(KonemMessage(message = Data("Send message ${count++}") ))
  })

  client.registerChannelReadListener(KonemMessageReceiver{from, msg ->
    println("CLIENT Msg: $msg from $from")


    if(count < 10) {
        client.sendMessage( KonemMessage(message = Data("Send message ${count++}")))
    }
  })

  Thread.sleep(8000)

  client.disconnect()

  Thread.sleep(1000)


  server.shutdownServer()
}
