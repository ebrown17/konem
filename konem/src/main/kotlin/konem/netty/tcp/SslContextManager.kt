package konem.netty.tcp

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.util.SelfSignedCertificate
import konem.logger
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

object SslContextManager {

    private val logger = logger(javaClass)

    private val useSsl = System.getProperty("konem.secure.secure", "true")
    private val protocol = System.getProperty("konem.secure.protocol", "TLS")
    private val algorithm = System.getProperty("konem.secure.algorithm", "SunX509")
    private val keyStoreLocation = System.getProperty("konem.secure.keyStoreLocation", "")
    private val keyStoreType = System.getProperty("konem.secure.keyStoreType", "JKS")
    private val keyStorePassword = System.getProperty("konem.secure.keyStorePassword", "")
    private val certificatePassword = System.getProperty("konem.secure.certificatePassword", "")

    private lateinit var keyStore: KeyStore
    private lateinit var trustFactory: TrustManagerFactory

    private var serverContext: SslContext? = null
    private var clientContext: SslContext? = null

    fun getServerContext(): SslContext? {
        synchronized(this) {
            return if(ensureInitialized()){
                serverContext
            } else{
                null
            }
        }
    }

    fun getClientContext(): SslContext? {
        synchronized(this) {
            return if(ensureInitialized()){
                clientContext
            } else{
                null
            }
        }
    }

    private fun ensureInitialized():Boolean {
        if (serverContext == null || clientContext == null) {
            keyStore = KeyStore.getInstance(keyStoreType)
            trustFactory = TrustManagerFactory.getInstance(algorithm)
            logger.info("keyStoreLocation is set to {}",keyStoreLocation)
            if (!keyStoreLocation.isNullOrEmpty()) {
                val certificate = loadCertificate()
                return if(certificate != null){
                    logger.info("loading {}", keyStoreLocation)
                    keyStore.load(certificate, keyStorePassword.toCharArray())
                    val kmf = KeyManagerFactory.getInstance(algorithm)
                    kmf.init(keyStore, keyStorePassword.toCharArray())
                    trustFactory.init(keyStore)
                    serverContext = SslContextBuilder.forServer(kmf).build()
                    clientContext = SslContextBuilder.forClient().trustManager(trustFactory).build()
                    certificate.close()
                    true
                } else {
                    logger.error("failed to load 'konem.secure.keyStoreLocation={}'",keyStoreLocation)
                    false
                }

            }
            else {
                logger.info("Loading self signed cert")
                val ssc = SelfSignedCertificate()
                serverContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
                clientContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                return true
            }
        }
        return true
    }

    private fun loadCertificate(): FileInputStream? {
        return try {
            File(keyStoreLocation).inputStream()
        } catch (e: FileNotFoundException) {
            null
        }
    }
}
