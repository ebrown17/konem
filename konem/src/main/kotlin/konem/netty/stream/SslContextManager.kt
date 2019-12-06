package konem.netty.stream


import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.slf4j.LoggerFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory


object SslContextManager {

  private val logger = LoggerFactory.getLogger("SslContextManager")

  private val useSsl = System.getProperty("konem.secure.secure", "true")
  private val protocol = System.getProperty("konem.secure.protocol", "TLS")
  private val algorithm = System.getProperty("konem.secure.algorithm", "SunX509")
  private val keyStoreLocation = System.getProperty("konem.secure.keyStoreLocation", "")
  private val keyStoreType = System.getProperty("konem.secure.keyStoreType", "JKS")
  private val keyStorePassword = System.getProperty("konem.secure.keyStorePassword", "")
  private val certificatePassword = System.getProperty("konem.secure.certificatePassword", "")

  private lateinit var keyStore: KeyStore
  private lateinit var trustFactory: TrustManagerFactory

  private lateinit var serverContext: SslContext
  private lateinit var clientContext: SslContext


   fun getServerContext(): SslContext {
     synchronized(this) {
       ensureInitialized()
       return serverContext
     }
  }

  fun getClientContext(): SslContext {
    synchronized(this) {
      ensureInitialized()
      return clientContext
    }
  }

  private fun ensureInitialized() {
    if (!this::serverContext.isInitialized || !this::clientContext.isInitialized) {
      keyStore = KeyStore.getInstance(keyStoreType)
      trustFactory = TrustManagerFactory.getInstance(algorithm)
      if (keyStoreLocation != "notSet") {
        val temp = File(keyStoreLocation)
        logger.info("keyStoreLocation is set to ${temp.absolutePath}")
        val cert = loadCertificate()
        if (cert != null) {
          logger.info("loading $keyStoreLocation")
          keyStore.load(cert, keyStorePassword.toCharArray())
          val kmf = KeyManagerFactory.getInstance(algorithm)
          kmf.init(keyStore, keyStorePassword.toCharArray());
          trustFactory.init(keyStore)
          serverContext = SslContextBuilder.forServer(kmf).build()
          clientContext = SslContextBuilder.forClient().trustManager(trustFactory).build()
          cert.close()
          return
        }
      }
      logger.info("Loading self signed cert")
      val ssc = SelfSignedCertificate()
      serverContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
      clientContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }
  }

  private fun loadCertificate(): FileInputStream? {
    return try {
      File(keyStoreLocation).inputStream()
    } catch (e: FileNotFoundException) {
      null
    }
  }
}

fun main() {
  println(SslContextManager.getServerContext())

}