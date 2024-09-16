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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.io7m.exfilac.core.EFSettings
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.StandardCharsets

class EFFragmentTabSettings : Fragment() {

  private val logger =
    LoggerFactory.getLogger(EFFragmentTabSettings::class.java)

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  private lateinit var commit: TextView
  private lateinit var manual: TextView
  private lateinit var paused: SwitchMaterial
  private lateinit var privacyPolicy: TextView
  private lateinit var saveLogs: TextView
  private lateinit var support: TextView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var uploadCellular: SwitchMaterial
  private lateinit var uploadWifi: SwitchMaterial
  private lateinit var version: TextView

  private var crashClicks = 0

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
    this.privacyPolicy =
      view.findViewById(R.id.settingsPrivacyPolicy)
    this.paused =
      view.findViewById(R.id.settingsUploadsPaused)
    this.commit =
      view.findViewById(R.id.settingsCommit)
    this.version =
      view.findViewById(R.id.settingsVersion)
    this.support =
      view.findViewById(R.id.settingsSupport)
    this.manual =
      view.findViewById(R.id.settingsUserManual)
    this.saveLogs =
      view.findViewById(R.id.settingsDumpLogs)

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
    this.paused.setOnCheckedChangeListener { _, isChecked ->
      this.updateSettings { settings: EFSettings ->
        settings.copy(paused = isChecked)
      }
    }
    this.privacyPolicy.setOnClickListener {
      this.openPrivacyPolicy()
    }
    this.support.setOnClickListener {
      val i = Intent(Intent.ACTION_VIEW)
      i.setData(Uri.parse("https://www.github.com/io7m-com/exfilac/discussions"))
      this.startActivity(i)
    }
    this.manual.setOnClickListener {
      EFApplication.application.exfilac.settingsDocumentOpen(
        bundledURI = URI.create("file:///android_asset/manual/index-m.xhtml"),
        externalURI = URI.create("https://www.io7m.com/software/exfilac/documentation/index-m.xhtml")
      )
    }

    this.saveLogs.setOnClickListener {
      EFCrashLogging.saveLogs()
        .thenApply { filePath ->
          EFUIThread.runOnUIThread {
            Toast.makeText(
              EFApplication.application,
              EFApplication.application.getString(R.string.settingsSavedLogsTo, filePath),
              Toast.LENGTH_LONG
            ).show()
          }
        }
    }

    this.commit.text = BuildConfig.EXFILAC_GIT_COMMIT
    this.version.text = BuildConfig.EXFILAC_VERSION

    /*
     * Register a commit click listener that will deliberately crash the application after
     * ten clicks. The purpose of this is to allow for testing the crash handler.
     */

    this.commit.setOnClickListener {
      this.crashClicks += 1
      if (this.crashClicks >= 10) {
        throw CrashedDeliberately()
      }
    }
    return view
  }

  private fun openPrivacyPolicy() {
    try {
      this.context?.assets?.let { assets ->
        assets.open("manual/privacy-link.txt").use { stream ->
          val linkBytes = stream.readBytes()
          val linkText = linkBytes.toString(StandardCharsets.UTF_8).trim()
          EFApplication.application.exfilac.settingsDocumentOpen(
            bundledURI = URI.create("file:///android_asset/manual/$linkText"),
            externalURI = URI.create("https://www.io7m.com/software/exfilac/documentation/index-m.xhtml")
          )
        }
      }
    } catch (e: Throwable) {
      this.logger.debug("Failed to open privacy policy: ", e)
    }
  }

  private class CrashedDeliberately : Exception("Crashed deliberately!")

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
    this.uploadCellular.isChecked =
      newSettings.networking.uploadOnCellular
    this.uploadWifi.isChecked =
      newSettings.networking.uploadOnWifi
    this.paused.isChecked =
      newSettings.paused
  }
}
