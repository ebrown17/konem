package konem.netty

import io.netty.channel.ChannelHandlerAdapter
import kotlin.collections.LinkedHashMap

class ProtocolPipeline<T>(
    private val protocolMessageHandler: () -> Pair<String, Handler<T>>,
    private val protoPipelineCodecs: (LinkedHashMap<String, ChannelHandlerAdapter>) -> Unit
) {

    fun getProtocolMessageHandler(): Pair<String, Handler<T>> {
        return protocolMessageHandler()
    }

    fun getProtocolPipelineCodecs(): LinkedHashMap<String, ChannelHandlerAdapter> {
        val codecs = LinkedHashMap<String, ChannelHandlerAdapter>()
        protoPipelineCodecs(codecs)
        return codecs
    }

}
