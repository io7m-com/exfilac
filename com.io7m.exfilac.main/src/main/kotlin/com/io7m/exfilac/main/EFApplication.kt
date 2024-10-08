/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.exfilac.main

import android.app.Application
import android.content.Context
import android.content.Intent
import com.io7m.exfilac.clock.api.EFClockSystem
import com.io7m.exfilac.content_tree.device.EFContentTreeDevice
import com.io7m.exfilac.core.ExfilacFactory
import com.io7m.exfilac.core.ExfilacType
import com.io7m.exfilac.s3_uploader.amazon.EFS3AMZUploaders
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class EFApplication : Application() {

  private val logger =
    LoggerFactory.getLogger(EFApplication::class.java)

  private val supervisor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor { r ->
      val thread = Thread(r)
      thread.isDaemon = true
      thread.priority = Thread.MIN_PRIORITY
      thread.name = "com.io7m.exfilac.supervisor[${thread.id}]"
      thread
    }

  private lateinit var exfilacField: ExfilacType

  val exfilac: ExfilacType
    get() = this.exfilacField

  companion object {
    private lateinit var INSTANCE: EFApplication

    @JvmStatic
    val application: EFApplication
      get() = this.INSTANCE
  }

  override fun onCreate() {
    super.onCreate()
    INSTANCE = this

    EFApplicationLogging.configure(this.cacheDir)
    this.logger.info("Start")

    /*
     * Configure a crash handler that dumps logs when the application crashes.
     */

    val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
      this.logger.error("Uncaught exception: ", e)
      EFCrashLogging.saveLogs().join()
      existingHandler?.uncaughtException(t, e)
    }

    this.exfilacField =
      ExfilacOnUI(
        ExfilacFactory.open(
          contentTrees = EFContentTreeDevice(this, this.contentResolver),
          s3Uploaders = EFS3AMZUploaders(),
          dataDirectory = application.dataDir.toPath(),
          cacheDirectory = application.cacheDir.toPath(),
          clock = EFClockSystem
        )
      )

    startServices(this)
  }

  private fun startServices(
    context: Context
  ) {
    this.supervisor.scheduleWithFixedDelay({
      if (!EFSupervisorService.isRunning()) {
        try {
          this.logger.debug("(Re)starting controller service…")
          context.startService(Intent(context, EFSupervisorService::class.java))
        } catch (e: Throwable) {
          this.logger.error("Failed to start service: ", e)
        }
      }
    }, 5L, 30L, TimeUnit.SECONDS)
  }
}
