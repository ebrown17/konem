package eb.netty.stream.client

import eb.netty.stream.Transceiver
import io.netty.bootstrap.Bootstrap
import kotlinx.coroutines.CoroutineScope

data class ClientBootstrapConfig constructor(val transceiver: Transceiver<Any>, val bootstrap: Bootstrap,val scope: CoroutineScope)
