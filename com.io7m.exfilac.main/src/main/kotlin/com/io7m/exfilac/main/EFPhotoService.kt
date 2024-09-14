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
    private val isRunning =
      AtomicBoolean(false)

    private val mediaObserver =
      MediaObserver(Handler(Looper.getMainLooper()))

    fun isRunning(): Boolean {
      return this.isRunning.get()
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    if (isRunning.compareAndSet(false, true)) {
      this.logger.debug("Starting photo service…")

      val sourceURIs = listOf(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Images.Media.INTERNAL_CONTENT_URI,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Video.Media.INTERNAL_CONTENT_URI,
      )

      val ok =
        sourceURIs.all { uri ->
          try {
            this.contentResolver.registerContentObserver(uri, true, mediaObserver)
            true
          } catch (e: Throwable) {
            this.logger.debug("Failed to register observer for {}: ", uri, e)
            false
          }
        }

      if (!ok) {
        try {
          this.contentResolver.unregisterContentObserver(mediaObserver)
        } catch (e: Throwable) {
          this.logger.debug("Failed to deregister observer: ", e)
        } finally {
          isRunning.set(false)
        }
      }
    } else {
      this.logger.debug("Ignoring redundant request to start photo service.")
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  private class MediaObserver(
    handler: Handler
  ) : ContentObserver(handler) {
    override fun onChange(
      selfChange: Boolean
    ) {
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uris: MutableCollection<Uri>,
      flags: Int
    ) {
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uri: Uri?,
      flags: Int
    ) {
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }

    override fun onChange(
      selfChange: Boolean,
      uri: Uri?
    ) {
      EFApplication.application.exfilac.uploadStartAllAsNecessary(
        EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    }
  }
}
