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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.io7m.exfilac.core.EFSettings
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException
import org.slf4j.LoggerFactory

class EFFragmentTabSettings : Fragment() {

  private val logger =
    LoggerFactory.getLogger(EFFragmentTabSettings::class.java)

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  private lateinit var uploadWifi: SwitchMaterial
  private lateinit var uploadCellular: SwitchMaterial
  private lateinit var toolbar: MaterialToolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.tab_settings, container, false)
    this.toolbar =
      view.findViewById(R.id.settingsAppBar)
    this.uploadCellular =
      view.findViewById(R.id.settingsUploadCellular)
    this.uploadWifi =
      view.findViewById(R.id.settingsUploadWIFI)

    this.uploadCellular.setOnCheckedChangeListener { _, isChecked ->
      this.updateSettings { settings: EFSettings ->
        settings.copy(settings.networking.copy(uploadOnCellular = isChecked))
      }
    }
    this.uploadWifi.setOnCheckedChangeListener { _, isChecked ->
      this.updateSettings { settings: EFSettings ->
        settings.copy(settings.networking.copy(uploadOnWifi = isChecked))
      }
    }

    return view
  }

  private fun updateSettings(
    updater: (EFSettings) -> EFSettings
  ) {
    EFApplication.application.exfilac.settingsUpdate(
      updater(EFApplication.application.exfilac.settings.get())
    )
  }

  override fun onStart() {
    super.onStart()

    this.subscriptions = CloseableCollection.create()
    this.subscriptions.add(
      EFApplication.application.exfilac.settings.subscribe { _, newValue ->
        this.onSettingsChanged(newValue)
      }
    )
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }

  private fun onSettingsChanged(
    newSettings: EFSettings
  ) {
    this.logger.debug("Settings changed.")

    this.uploadCellular.isChecked =
      newSettings.networking.uploadOnCellular
    this.uploadWifi.isChecked =
      newSettings.networking.uploadOnWifi
  }
}
