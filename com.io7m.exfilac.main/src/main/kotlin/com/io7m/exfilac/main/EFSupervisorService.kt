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

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.io7m.jattribute.core.AttributeReadableType
import com.io7m.jattribute.core.Attributes
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A service that does nothing but sit in the foreground.
 */

class EFSupervisorService : Service() {

  private val logger =
    LoggerFactory.getLogger(EFSupervisorService::class.java)

  companion object {
    private val executor =
      Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r)
        thread.isDaemon = true
        thread.name = "com.io7m.exfilac.main.supervisor_service[${thread.id}]"
        thread.priority = Thread.MIN_PRIORITY
        thread
      }

    private val areNotificationsPermittedSource =
      Attributes.create({ e -> })
        .withValue(false)

    val areNotificationsPermitted: AttributeReadableType<Boolean> =
      this.areNotificationsPermittedSource

    private val isRunning =
      AtomicBoolean(false)

    fun isRunning(): Boolean {
      return this.isRunning.get()
    }
  }

  override fun onBind(
    intent: Intent?
  ): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()

    executor.scheduleWithFixedDelay({
      EFUIThread.runOnUIThread {
        areNotificationsPermittedSource.set(
          EFNotifications.notificationsArePermitted(EFApplication.application)
        )
      }
    }, 1L, 5L, TimeUnit.SECONDS)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    this.startForegroundSupervisorService()

    try {
      if (!EFSchedulerService.isRunning()) {
        this.logger.debug("Starting scheduler service…")
        this.startService(Intent(this, EFSchedulerService::class.java))
      }
    } catch (e: Throwable) {
      this.logger.error("Failed to start service: ", e)
    }

    try {
      if (!EFNetworkConnectivityService.isRunning()) {
        this.logger.debug("Starting network connectivity service…")
        this.startService(Intent(this, EFNetworkConnectivityService::class.java))
      }
    } catch (e: Throwable) {
      this.logger.error("Failed to start service: ", e)
    }

    try {
      if (!EFPhotoService.isRunning()) {
        this.logger.debug("Starting photo service…")
        this.startService(Intent(this, EFPhotoService::class.java))
      }
    } catch (e: Throwable) {
      this.logger.error("Failed to start service: ", e)
    }

    return super.onStartCommand(intent, flags, startId)
  }

  private fun startForegroundSupervisorService() {
    try {
      EFNotifications.createNotificationChannel(this)
    } catch (e: Throwable) {
      this.logger.error("Failed to create notification channel: ", e)
    }

    try {
      ServiceCompat.startForeground(
        this,
        1,
        EFNotifications.buildNotification(this),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
          0
        }
      )
    } catch (e: Throwable) {
      this.logger.error("Failed to start foreground service: ", e)
    }
  }
}
