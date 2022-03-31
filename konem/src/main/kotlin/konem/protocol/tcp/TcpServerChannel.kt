package konem.protocol.tcp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.*
import konem.netty.server.ServerChannelInfo

class TcpServerChannel<I>(
    private val transceiver: ServerTransceiver<I>,
    private val serverChannelInfo: ServerChannelInfo<I>
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {

        val pipeline = channel.pipeline()
        if (serverChannelInfo.use_ssl) {
            SslContextManager.getServerContext()?.let { context ->
                pipeline.addLast("serverSslHandler", context.newHandler(channel.alloc()))
            }?: run {
                throw Exception("SslContextManager.getServerContext() failed to initialize... closing channel")
            }
        }

        serverChannelInfo.protocolPipeline?.getProtocolPipelineCodecs()?.forEach { entry ->
            pipeline.addLast(entry.key,entry.value)
        }

  /*      pipeline.addLast("jsonDecoder", JsonObjectDecoder())
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("konemCodec", KonemJsonCodec())*/

        //
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
        pipeline.addLast("heartBeatHandler", HeartbeatProducer(transceiver,serverChannelInfo.protocolPipeline!!.getHeartbeat()))

        pipeline.addLast("messageHandler", MessageHandler(serverChannelInfo.channel_id, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
