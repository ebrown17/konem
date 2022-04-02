package konem.netty

import io.netty.channel.ChannelHandlerAdapter
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.LinkedHashMap

class ProtocolPipeline<T>(private val protocolMessageHandler:  (LinkedHashMap<String, Handler<T>>) -> Unit, private val protoPipelineCodecs: (LinkedHashMap<String, ChannelHandlerAdapter>) -> Unit){

    fun getProtocolMessageHandler(): LinkedHashMap<String, Handler<T>> {
        val messageHandler = LinkedHashMap<String, Handler<T>>()
        protocolMessageHandler(messageHandler)
       return messageHandler
    }

    fun getProtocolPipelineCodecs(): LinkedHashMap<String, ChannelHandlerAdapter>  {
        val codecs = LinkedHashMap<String, ChannelHandlerAdapter>()
        protoPipelineCodecs(codecs)
        return codecs
    }



}
