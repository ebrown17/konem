package konem

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> logger(from: T): Logger {
    return if (from is Class<*>) {
        LoggerFactory.getLogger(from)
    } else {
        LoggerFactory.getLogger(T::class.java)
    }
}
