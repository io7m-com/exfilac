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
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.MaterialToolbar
import com.io7m.exfilac.core.EFNetworkStatus
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException

class EFFragmentTabStatus : Fragment() {

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  private lateinit var adapter: EFStatusAdapter
  private lateinit var emptyView: ViewGroup
  private lateinit var listView: RecyclerView
  private lateinit var statusIcon: ImageView
  private lateinit var statusText: TextView
  private lateinit var toolbar: MaterialToolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.tab_status, container, false)
    this.toolbar =
      view.findViewById(R.id.statusAppBar)
    this.listView =
      view.findViewById(R.id.statusListView)
    this.emptyView =
      view.findViewById(R.id.statusListEmpty)
    this.statusIcon =
      view.findViewById(R.id.statusNetworkIcon)
    this.statusText =
      view.findViewById(R.id.statusNetworkText)

    this.listView.layoutManager = LinearLayoutManager(view.context)
    this.listView.setHasFixedSize(true)
    (this.listView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return view
  }

  override fun onStart() {
    super.onStart()

    this.adapter = EFStatusAdapter(items = listOf())
    this.listView.adapter = this.adapter
    this.listView.visibility = View.VISIBLE
    this.emptyView.visibility = View.INVISIBLE

    this.subscriptions = CloseableCollection.create()
    this.subscriptions.add(
      EFApplication.application.exfilac.uploads.subscribe { _, newValue ->
        this.onUploadsChanged(newValue)
      }
    )
    this.subscriptions.add(
      EFApplication.application.exfilac.uploadStatus.subscribe { _, _ ->
        this.onUploadStatusChanged()
      }
    )
    this.subscriptions.add(
      EFApplication.application.exfilac.networkStatus.subscribe { _, newValue ->
        this.onNetworkStatusChanged(newValue)
      }
    )
  }

  @UiThread
  private fun onNetworkStatusChanged(newValue: EFNetworkStatus) {
    EFUIThread.checkIsUIThread()

    when (newValue) {
      EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE -> {
        this.statusIcon.setImageResource(R.drawable.network_none_16)
        this.statusText.setText(R.string.networkStatusUnavailable)
      }
      EFNetworkStatus.NETWORK_STATUS_CELLULAR -> {
        this.statusIcon.setImageResource(R.drawable.network_cellular_16)
        this.statusText.setText(R.string.networkStatusCellular)
      }
      EFNetworkStatus.NETWORK_STATUS_WIFI -> {
        this.statusIcon.setImageResource(R.drawable.network_wifi_16)
        this.statusText.setText(R.string.networkStatusWifi)
      }
    }
  }

  @UiThread
  private fun onUploadStatusChanged() {
    EFUIThread.checkIsUIThread()

    this.adapter.notifyDataSetChanged()
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }

  @UiThread
  private fun onUploadsChanged(
    status: List<EFUploadConfiguration>
  ) {
    EFUIThread.checkIsUIThread()

    this.adapter.setUploadNames(status.map { upload -> upload.name })

    if (status.isEmpty()) {
      this.listView.visibility = View.INVISIBLE
      this.emptyView.visibility = View.VISIBLE
      return
    }

    this.listView.visibility = View.VISIBLE
    this.emptyView.visibility = View.INVISIBLE
  }
}
