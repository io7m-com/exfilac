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
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import com.io7m.exfilac.core.EFUploadReasonTrigger
import com.io7m.exfilac.core.EFUploadTrigger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class EFPhotoService : Service() {

  private val logger =
    LoggerFactory.getLogger(EFPhotoService::class.java)

  companion object {
    private val started =
      AtomicBoolean(false)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    if (started.compareAndSet(false, true)) {
      this.logger.debug("Starting photo service…")

      try {
        this.contentResolver.registerContentObserver(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          true,
          this.MediaObserver(Handler(Looper.getMainLooper()))
        )
      } catch (e: Exception) {
        this.logger.debug("Failed to register observer for Images.Media.EXTERNAL_CONTENT_URI: ", e)
      }

      try {
        this.contentResolver.registerContentObserver(
          MediaStore.Images.Media.INTERNAL_CONTENT_URI,
          true,
          this.MediaObserver(Handler(Looper.getMainLooper()))
        )
      } catch (e: Exception) {
        this.logger.debug("Failed to register observer for Images.Media.INTERNAL_CONTENT_URI: ", e)
      }

      try {
        this.contentResolver.registerContentObserver(
          MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          true,
          this.MediaObserver(Handler(Looper.getMainLooper()))
        )
      } catch (e: Exception) {
        this.logger.debug("Failed to register observer for Video.Media.EXTERNAL_CONTENT_URI: ", e)
      }

      try {
        this.contentResolver.registerContentObserver(
          MediaStore.Video.Media.INTERNAL_CONTENT_URI,
          true,
          this.MediaObserver(Handler(Looper.getMainLooper()))
        )
      } catch (e: Exception) {
        this.logger.debug("Failed to register observer for Video.Media.INTERNAL_CONTENT_URI: ", e)
      }
    } else {
      this.logger.debug("Ignoring redundant request to start photo service.")
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  private inner class MediaObserver(
    handler: Handler
  ) : ContentObserver(handler) {
    override fun onChange(
      selfChange: Boolean
    ) {
      this@EFPhotoService.logger.debug("onChange: {}", selfChange)
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uris: MutableCollection<Uri>,
      flags: Int
    ) {
      this@EFPhotoService.logger.debug("onChange: {} {} {}", selfChange, uris, flags)
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uri: Uri?,
      flags: Int
    ) {
      this@EFPhotoService.logger.debug("onChange: {} {} {}", selfChange, uri, flags)
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uri: Uri?
    ) {
      this@EFPhotoService.logger.debug("onChange: {} {}", selfChange, uri)
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }
  }
}
