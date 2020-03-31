package konem.netty.stream.client

import io.netty.bootstrap.Bootstrap
import konem.netty.stream.Transceiver
import kotlinx.coroutines.CoroutineScope

data class ClientBootstrapConfig<T, H> constructor(
  val transceiver: Transceiver<T, H>,
  val bootstrap: Bootstrap,
  val scope: CoroutineScope
)
