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

import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.io7m.exfilac.core.EFState
import com.io7m.exfilac.core.EFStateBootFailed
import com.io7m.exfilac.core.EFStateBooting
import com.io7m.exfilac.core.EFStateBucketEditing
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFStateSettingsReadingDocument
import com.io7m.exfilac.core.EFStateUploadConfigurationEditing
import com.io7m.exfilac.core.EFStateUploadStatusViewing
import com.io7m.exfilac.main.EFScreenFragment.EFBackResult
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException

class EFActivity : AppCompatActivity(R.layout.main_activity) {

  private lateinit var fragmentNow: EFScreenFragment

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  override fun onStart() {
    super.onStart()
    this.subscriptions = CloseableCollection.create()
    this.subscriptions.add(
      EFApplication.application.exfilac.state.subscribe { _, newValue ->
        this.onStateChanged(newValue)
      }
    )

    /*
     * If the user hasn't seen the permissions nag screen, then display it. They'll
     * be given an opportunity to grant permissions to the application. Unfortunately,
     * if they refuse the first time, there's no point making the system request them
     * again, because it'll just silently fail.
     */

    val settings = EFApplication.application.exfilac.settings.get()
    if (!settings.notifications.hasSeenNotificationsNagScreen) {
      EFNotifications.notificationsDisplayDialog(this) {
        EFApplication.application.exfilac.settingsUpdate(
          settings = settings.copy(
            notifications = settings.notifications.copy(
              hasSeenNotificationsNagScreen = true
            )
          )
        )
      }
    }
  }

  @UiThread
  private fun onStateChanged(
    newValue: EFState,
  ) {
    EFUIThread.checkIsUIThread()

    when (newValue) {
      is EFStateBootFailed -> {
        // Nothing
      }

      is EFStateBooting -> {
        this.switchFragment(EFFragmentBooting())
      }

      is EFStateReady -> {
        this.switchFragment(EFFragmentMain())
      }

      is EFStateBucketEditing -> {
        this.switchFragment(EFFragmentBucketEditing())
      }

      is EFStateUploadConfigurationEditing -> {
        this.switchFragment(EFFragmentUploadConfigurationEditing())
      }

      is EFStateUploadStatusViewing -> {
        this.switchFragment(EFFragmentUploadStatus())
      }

      is EFStateSettingsReadingDocument -> {
        this.switchFragment(EFFragmentDocument())
      }
    }
  }

  @Deprecated("This method has been deprecated by clueless \"engineers\".")
  override fun onBackPressed() {
    when (this.fragmentNow.onBackPressed()) {
      EFBackResult.BACK_HANDLED -> Unit
      EFBackResult.BACK_PROPAGATE_UP -> super.onBackPressed()
    }
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }

  private fun switchFragment(fragment: EFScreenFragment) {
    this.fragmentNow = fragment
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment)
      .commit()
  }
}
