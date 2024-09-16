package com.io7m.exfilac.main

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Functions to configure logging.
 */

object EFApplicationLogging {

  /**
   * Configure the default logcat appender.
   */

  private fun configureLogcatAppender(
    loggerContext: LoggerContext
  ): Appender<ILoggingEvent> {
    val encoder =
      PatternLayoutEncoder().apply {
        this.context = loggerContext
        this.pattern = "%msg%n"
        this.start()
      }
    return LogcatAppender().apply {
      this.context = loggerContext
      this.name = "LOGCAT"
      this.encoder = encoder
      this.start()
    }
  }

  /**
   * Configure a file based logging appender.
   */

  private fun configureFileAppender(
    loggerContext: LoggerContext,
    cacheDirectory: File,
    filename: String
  ): Appender<ILoggingEvent> {
    val logDirectory = logDirectory(cacheDirectory)
    logDirectory.mkdirs()

    val encoder =
      PatternLayoutEncoder().apply {
        this.context = loggerContext
        this.pattern = "%d{\"yyyy-MM-dd'T'HH:mm:ss,SSS\"} %level %logger{128} - %msg%n"
        this.start()
      }
    val rollingPolicy =
      TimeBasedRollingPolicy<ILoggingEvent>().apply {
        this.context = loggerContext
        this.fileNamePattern = "$filename.%d.gz"
        this.maxHistory = 7
        this.setTotalSizeCap(FileSize.valueOf("10MB"))
      }
    val fileAppender =
      RollingFileAppender<ILoggingEvent>().apply {
        this.context = loggerContext
        this.encoder = encoder
        this.file = File(logDirectory, filename).absolutePath
        this.name = "FILE"
        this.rollingPolicy = rollingPolicy
      }

    rollingPolicy.setParent(fileAppender)
    rollingPolicy.start()
    fileAppender.start()

    return AsyncAppender().apply {
      this.context = loggerContext
      this.name = "ASYNC_FILE"
      this.addAppender(fileAppender)
      this.start()
    }
  }

  fun logDirectory(
    cacheDirectory: File
  ): File {
    return File(cacheDirectory, "log")
  }

  fun configure(
    cacheDirectory: File
  ) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    loggerContext.stop()

    loggerContext.statusManager.apply {
      this.add { StatusPrinter.print(listOf(it)) }
    }

    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    root.apply {
      this.addAppender(configureFileAppender(loggerContext, cacheDirectory, "log.txt"))
      this.addAppender(configureLogcatAppender(loggerContext))
    }
  }
}
