package eb.protocol.websocket


import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import eb.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress


@JsonClass(generateAdapter = true)
data class KoneMesssage(val type: String, val message: String)


data class KoneHeartbeat private constructor(val type: String)  {
    constructor( ) : this(heartbeat)
    companion object{
        const val heartbeat = "heartbeat"
    }
}


class KoneMessageReceiver(private val receive: (InetSocketAddress, KoneMesssage) -> Unit) : ReceiverHandler<String>() {
    private val logger = LoggerFactory.getLogger(KoneMessageReceiver::class.java)
    private val adaptor = moshi.adapter(KoneMesssage::class.java)

    override fun read(addr: InetSocketAddress, message: String) {
        try {
            val kMessage = adaptor.fromJson(message)
            if (kMessage != null) {
                receive(addr, kMessage)
            }
        } catch (e: Exception) {
            logger.error("exception in casting of message : {} ", e.message)
        }

    }

    companion object{
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

}