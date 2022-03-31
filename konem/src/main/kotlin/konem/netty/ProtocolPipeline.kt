package konem.netty

import io.netty.channel.ChannelHandlerAdapter
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.LinkedHashMap


// ChannelHandlerAdapter
class ProtocolPipeline<I>(private val generateHeartBeat: () -> I, private val protoPipelineCodecs: (LinkedHashMap<String, ChannelHandlerAdapter>) -> Unit){

    fun getProtocolPipelineCodecs(): (LinkedHashMap<String, ChannelHandlerAdapter>)  {
        val codecs = LinkedHashMap<String, ChannelHandlerAdapter>()
        protoPipelineCodecs(codecs)
        return codecs
    }

    fun getHeartbeat(): () -> I{
        return { generateHeartBeat() }
    }
}


fun main(){


}
