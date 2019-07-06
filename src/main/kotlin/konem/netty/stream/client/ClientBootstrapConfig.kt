package konem.netty.stream.client

import konem.netty.stream.Transceiver
import io.netty.bootstrap.Bootstrap
import kotlinx.coroutines.CoroutineScope

data class ClientBootstrapConfig constructor(
  val transceiver: Transceiver<Any>,
  val bootstrap: Bootstrap,
  val scope: CoroutineScope
)
