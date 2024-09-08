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

import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFNetworkStatus
import com.io7m.exfilac.core.EFSettings
import com.io7m.exfilac.core.EFState
import com.io7m.exfilac.core.EFStateBooting
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadStatusChanged
import com.io7m.exfilac.core.ExfilacType
import com.io7m.jattribute.core.AttributeReadableType
import com.io7m.jattribute.core.AttributeSubscriptionType
import com.io7m.jattribute.core.AttributeType
import com.io7m.jattribute.core.Attributes
import com.io7m.jmulticlose.core.CloseableCollection
import org.slf4j.LoggerFactory

/**
 * An Exfilac proxy that republishes attributes on the UI thread.
 *
 * This eliminates the subtle and extremely nasty problems that can occur with scheduling
 * work on the Android UI thread. For example:
 *
 * 1. Something occurs on a non-UI thread, and sets an attribute.
 * 2. In setting the attribute, all subscribers to that attribute are notified.
 * 3. A particular subscriber receives the event, and then schedules work on the UI thread.
 * 4. At a later time, the scheduled UI work executes.
 *
 * The problem is that Android can invalidate the state of the world (by, for example, detaching
 * a fragment) between steps 3 and 4. This means that when the work actually executes, the world
 * is in an invalid state and the application crashes.
 *
 * We can eliminate this problem by buffering attributes. For a given attribute X, we create
 * an attribute Y of the same type. We subscribe to X and, when we receive an update, schedule
 * an operation on the UI thread that sets the value of Y. We then _only_ expose Y to the rest
 * of the application. This results in updates to Y effectively occurring on the UI thread, and
 * therefore any subscribers to Y will have their updates delivered synchronously on the UI
 * thread at the same time; there is no temporal disconnect between an event being delivered
 * and then being processed at a later date, and therefore no lifecycle-related crashes can
 * result.
 */

class ExfilacOnUI(
  private val delegate: ExfilacType
) : ExfilacType by delegate {

  private val logger =
    LoggerFactory.getLogger(ExfilacOnUI::class.java)

  private val attributes =
    Attributes.create { e -> this.logger.debug("Uncaught attribute exception: ", e) }

  private val subscriptions =
    CloseableCollection.create()
  private val bucketsUI: AttributeType<List<EFBucketConfiguration>> =
    this.attributes.withValue(this.delegate.buckets.get())
  private val bucketsSelectedUI: AttributeType<Set<EFBucketReferenceName>> =
    this.attributes.withValue(this.delegate.bucketsSelected.get())
  private val uploadsUI: AttributeType<List<EFUploadConfiguration>> =
    this.attributes.withValue(this.delegate.uploads.get())
  private val uploadsSelectedUI: AttributeType<Set<EFUploadName>> =
    this.attributes.withValue(this.delegate.uploadsSelected.get())
  private val uploadsStatusUI: AttributeType<EFUploadStatusChanged> =
    this.attributes.withValue(EFUploadStatusChanged())
  private val stateUI: AttributeType<EFState> =
    this.attributes.withValue(EFStateBooting("", 0.0))
  private val networkStatusUI: AttributeType<EFNetworkStatus> =
    this.attributes.withValue(EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE)
  private val settingsUI: AttributeType<EFSettings> =
    this.attributes.withValue(EFSettings.defaults())

  companion object {
    private fun <T> wrap(
      source: AttributeReadableType<T>,
      target: AttributeType<T>
    ): AttributeSubscriptionType {
      return source.subscribe { _, newValue -> EFUIThread.runOnUIThread { target.set(newValue) } }
    }
  }

  init {
    this.subscriptions.add(wrap(this.delegate.buckets, this.bucketsUI))
    this.subscriptions.add(wrap(this.delegate.bucketsSelected, this.bucketsSelectedUI))
    this.subscriptions.add(wrap(this.delegate.state, this.stateUI))
    this.subscriptions.add(wrap(this.delegate.uploadStatus, this.uploadsStatusUI))
    this.subscriptions.add(wrap(this.delegate.uploads, this.uploadsUI))
    this.subscriptions.add(wrap(this.delegate.uploadsSelected, this.uploadsSelectedUI))
    this.subscriptions.add(wrap(this.delegate.networkStatus, this.networkStatusUI))
    this.subscriptions.add(wrap(this.delegate.settings, this.settingsUI))
  }

  override fun close() {
    this.subscriptions.close()
    this.delegate.close()
  }

  override val uploadStatus: AttributeReadableType<EFUploadStatusChanged> =
    this.uploadsStatusUI

  override val uploadsSelected: AttributeReadableType<Set<EFUploadName>> =
    this.uploadsSelectedUI

  override val buckets: AttributeReadableType<List<EFBucketConfiguration>> =
    this.bucketsUI

  override val bucketsSelected: AttributeReadableType<Set<EFBucketReferenceName>> =
    this.bucketsSelectedUI

  override val uploads: AttributeReadableType<List<EFUploadConfiguration>> =
    this.uploadsUI

  override val state: AttributeReadableType<EFState> =
    this.stateUI

  override val networkStatus: AttributeReadableType<EFNetworkStatus> =
    this.networkStatusUI

  override val settings: AttributeReadableType<EFSettings> =
    this.settingsUI
}
