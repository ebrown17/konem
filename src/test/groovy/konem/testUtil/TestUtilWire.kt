package konem.testUtil

import konem.data.protobuf.KonemMessage

class TestUtilWire {

    fun createWireMessage(datas: String): KonemMessage {
      return KonemMessage(
        messageType = KonemMessage.MessageType.DATA,
        data = KonemMessage.Data(datas)
      )

    }

}

