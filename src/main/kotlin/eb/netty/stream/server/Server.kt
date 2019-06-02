package eb.netty.stream.server

import eb.netty.stream.ChannelReader
import eb.netty.stream.ConnectionStatusListener
import eb.netty.stream.HandlerListener
import eb.netty.stream.Transceiver
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

abstract class Server : ChannelReader, HandlerListener{

    private val logger = LoggerFactory.getLogger(javaClass)

    private val bossGroup: EventLoopGroup
    private val workerGroup: EventLoopGroup
    private var bootstrapMap: ConcurrentHashMap<Int, ServerBootstrap> = ConcurrentHashMap()
    private val channelMap: ConcurrentHashMap<Int, Channel> = ConcurrentHashMap()
    private val channelListenerMap: ConcurrentHashMap<Int, ArrayList<ChannelFutureListener>> = ConcurrentHashMap()
    private val portAddressMap: ConcurrentHashMap<Int, InetSocketAddress> = ConcurrentHashMap()
    private val transceiverMap: ConcurrentHashMap<Int, Transceiver<Any>> = ConcurrentHashMap()
    private val channelConnectionMap: ConcurrentHashMap<Int, ArrayList<InetSocketAddress>> = ConcurrentHashMap()
    private val remoteHostToChannelMap: ConcurrentHashMap<InetSocketAddress, Int> = ConcurrentHashMap()
    private val connectionListeners: MutableList<ConnectionStatusListener> = ArrayList()

    init{
        val threadFactory = DefaultThreadFactory("server")
        bossGroup = NioEventLoopGroup(1, threadFactory)
        workerGroup = NioEventLoopGroup(0, threadFactory)
    }

}