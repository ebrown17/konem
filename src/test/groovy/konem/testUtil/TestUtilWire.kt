package konem.testUtil

import konem.data.protobuf.KonemMessage

public class TestUtilWire {


    fun createWireMessage(datas: String): KonemMessage {
      return KonemMessage(
        messageType = KonemMessage.MessageType.DATA,
        data = KonemMessage.Data(datas)
      )

    }

}

