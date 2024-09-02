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
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException

class EFFragmentTabUploads : Fragment() {

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  private lateinit var emptyView: ViewGroup
  private lateinit var adapter: EFUploadsAdapter
  private lateinit var listView: RecyclerView
  private lateinit var toolbar: MaterialToolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.tab_uploads, container, false)
    this.toolbar =
      view.findViewById(R.id.uploadsAppBar)
    this.listView =
      view.findViewById(R.id.uploadsListView)
    this.emptyView =
      view.findViewById(R.id.uploadsListEmpty)

    this.listView.layoutManager = LinearLayoutManager(view.context)
    this.listView.setHasFixedSize(true)
    (this.listView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.toolbar.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.uploadsMenuAddUpload -> {
          this.onUploadMenuAddSelected()
          true
        }

        R.id.uploadsMenuRemoveUpload -> {
          this.onUploadMenuRemoveSelected()
          true
        }

        else -> false
      }
    }

    return view
  }

  override fun onStart() {
    super.onStart()

    this.adapter = EFUploadsAdapter(items = listOf())
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
      EFApplication.application.exfilac.uploadsSelected.subscribe { _, newValue ->
        this.onUploadSelectionChanged(newValue)
      }
    )
  }

  @UiThread
  private fun onUploadSelectionChanged(
    selected: Set<EFUploadName>
  ) {
    this.toolbar.menu.clear()
    val activity = this.requireActivity()
    if (selected.isEmpty()) {
      activity.menuInflater.inflate(R.menu.uploads_menu_add, this.toolbar.menu)
    } else {
      activity.menuInflater.inflate(R.menu.uploads_menu_remove, this.toolbar.menu)
    }
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }

  @UiThread
  private fun onUploadsChanged(
    uploads: List<EFUploadConfiguration>
  ) {
    EFUIThread.checkIsUIThread()

    if (uploads.isEmpty()) {
      this.listView.visibility = View.INVISIBLE
      this.emptyView.visibility = View.VISIBLE
      return
    }

    this.listView.visibility = View.VISIBLE
    this.emptyView.visibility = View.INVISIBLE
  }

  @UiThread
  private fun onUploadMenuAddSelected() {
    EFUIThread.checkIsUIThread()

    EFApplication.application.exfilac.uploadEditBegin()
  }

  @UiThread
  private fun onUploadMenuRemoveSelected() {
    EFUIThread.checkIsUIThread()

    val builder = MaterialAlertDialogBuilder(this.requireContext())
    builder.setMessage(R.string.uploadDeleteConfirm)
    builder.setPositiveButton(R.string.delete) { _, _ ->
      EFApplication.application.exfilac.uploadsDelete(
        EFApplication.application.exfilac.uploadsSelected.get()
      )
    }
    builder.setNegativeButton(R.string.cancel) { d, _ ->
      d.cancel()
    }
    builder.show()
  }
}
