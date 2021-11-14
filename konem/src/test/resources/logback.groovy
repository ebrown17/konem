// logback.groovy config file must be on classpath along with Groovy to be read

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.FileAppender

// logs configuration details
statusListener(OnConsoleStatusListener)

// period to scan for config file changes
scan('1 Minutes')

// config file constants

def defaultLogPattern = "%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36}.%M - %msg%n"


appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) { pattern = defaultLogPattern }
}
// Log will display all levels that are greater than set level
// TRACE < DEBUG < INFO < WARN < ERROR < OFF

// Log level for logger in these classes, will override root level if set, will duplicate logs if same appender is added
logger('io.netty', WARN)


// Root log level for all logging
root(warn, ["STDOUT"])
