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
import android.webkit.WebView
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.io7m.exfilac.core.EFStateBootFailed
import com.io7m.exfilac.core.EFStateBooting
import com.io7m.exfilac.core.EFStateBucketEditing
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFStateSettingsReadingDocument
import com.io7m.exfilac.core.EFStateUploadConfigurationEditing
import com.io7m.exfilac.core.EFStateUploadStatusViewing

class EFFragmentDocument : EFScreenFragment() {

  private lateinit var liveText: TextView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var webView: WebView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.web_view, container, false)

    this.toolbar =
      view.findViewById(R.id.documentAppBar)
    this.webView =
      view.findViewById(R.id.webView)
    this.liveText =
      view.findViewById(R.id.webViewLive)

    this.toolbar.setNavigationIcon(R.drawable.back_24)
    this.toolbar.setNavigationOnClickListener {
      this.onWantClose()
    }
    this.toolbar.menu.clear()

    this.webView.settings.allowFileAccess = true

    this.liveText.setOnClickListener {
      this.openLiveText()
    }
    return view
  }

  private fun openLiveText() {
    when (val state = EFApplication.application.exfilac.state.get()) {
      is EFStateSettingsReadingDocument -> {
        val i = Intent(Intent.ACTION_VIEW)
        i.setData(Uri.parse(state.externalURI.toString()))
        this.startActivity(i)
      }

      is EFStateBootFailed,
      is EFStateBooting,
      is EFStateBucketEditing,
      is EFStateReady,
      is EFStateUploadConfigurationEditing,
      is EFStateUploadStatusViewing -> {
        // Nothing to do
      }
    }
  }

  private fun onWantClose() {
    EFApplication.application.exfilac.settingsDocumentClose()
  }

  override fun onStart() {
    super.onStart()

    if (this.webView.url == null) {
      when (val state = EFApplication.application.exfilac.state.get()) {
        is EFStateSettingsReadingDocument -> {
          this.webView.loadUrl(state.bundledURI.toString())
        }

        is EFStateBootFailed,
        is EFStateBooting,
        is EFStateBucketEditing,
        is EFStateReady,
        is EFStateUploadConfigurationEditing,
        is EFStateUploadStatusViewing -> {
          // Nothing to do
        }
      }
    }
  }

  override fun onBackPressed(): EFBackResult {
    if (this.webView.canGoBack()) {
      this.webView.goBack()
      return EFBackResult.BACK_HANDLED
    }

    this.onWantClose()
    return EFBackResult.BACK_HANDLED
  }
}
