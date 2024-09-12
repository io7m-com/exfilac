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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.MaterialToolbar
import com.io7m.exfilac.core.EFUploadResult
import com.io7m.exfilac.core.internal.EFUploadEventRecord
import com.io7m.exfilac.core.internal.EFUploadRecord
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException
import java.time.Duration
import java.util.Optional

class EFFragmentUploadStatus : EFFragment() {

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  private lateinit var adapter: EFUploadStatusAdapter
  private lateinit var bucket: TextView
  private lateinit var eventCount: TextView
  private lateinit var failed: TextView
  private lateinit var icon: ImageView
  private lateinit var id: TextView
  private lateinit var listView: RecyclerView
  private lateinit var reason: TextView
  private lateinit var required: TextView
  private lateinit var skipped: TextView
  private lateinit var summary: TextView
  private lateinit var timeDuration: TextView
  private lateinit var timeEnded: TextView
  private lateinit var timeStarted: TextView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var uploaded: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.status_view, container, false)

    this.listView =
      view.findViewById(R.id.uploadsStatusListView)
    this.eventCount =
      view.findViewById(R.id.uploadStatusEventCount)
    this.id =
      view.findViewById(R.id.uploadID)
    this.reason =
      view.findViewById(R.id.uploadStatusReason)
    this.skipped =
      view.findViewById(R.id.uploadStatusSkipped)
    this.failed =
      view.findViewById(R.id.uploadStatusFailed)
    this.uploaded =
      view.findViewById(R.id.uploadStatusUploaded)
    this.required =
      view.findViewById(R.id.uploadStatusRequired)
    this.timeStarted =
      view.findViewById(R.id.uploadTimeStarted)
    this.timeEnded =
      view.findViewById(R.id.uploadTimeEnded)
    this.timeDuration =
      view.findViewById(R.id.uploadTimeDuration)
    this.icon =
      view.findViewById(R.id.uploadStatusIcon)
    this.summary =
      view.findViewById(R.id.uploadStatusSummary)
    this.bucket =
      view.findViewById(R.id.uploadBucket)

    this.toolbar = view.findViewById(R.id.uploadStatusAppBar)
    this.toolbar.setNavigationIcon(R.drawable.back_24)
    this.toolbar.setNavigationOnClickListener {
      EFApplication.application.exfilac.uploadViewCancel()
    }

    this.listView.layoutManager = LinearLayoutManager(view.context)
    this.listView.setHasFixedSize(true)
    (this.listView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.eventCount.text = ""
    return view
  }

  override fun onStart() {
    super.onStart()

    this.adapter = EFUploadStatusAdapter(items = listOf())
    this.listView.adapter = this.adapter
    this.listView.visibility = View.VISIBLE
    this.subscriptions = CloseableCollection.create()

    this.subscriptions.add(
      EFApplication.application.exfilac.uploadViewEvents.subscribe { _, newValue ->
        this.onUploadEventsChanged(newValue)
      }
    )
    this.subscriptions.add(
      EFApplication.application.exfilac.uploadViewRecord.subscribe { _, newValue ->
        this.onUploadRecordChanged(newValue)
      }
    )
  }

  private fun onUploadRecordChanged(
    newValue: Optional<EFUploadRecord>
  ) {
    newValue.ifPresent { rec ->
      val timeStart = rec.timeStart
      val timeEnd = rec.timeEnd

      this.bucket.text = rec.bucket?.value ?: ""
      this.failed.text = rec.filesFailed.toString()
      this.id.text = rec.id.toString()
      this.reason.text = rec.reason
      this.required.text = rec.filesRequired.toString()
      this.skipped.text = rec.filesSkipped.toString()
      this.timeDuration.text = ""
      this.timeEnded.text = timeEnd?.format(EFTimes.dateTimeFormatter) ?: ""
      this.timeStarted.text = timeStart.format(EFTimes.dateTimeFormatter)
      this.uploaded.text = rec.filesUploaded.toString()

      if (timeEnd != null) {
        val duration = Duration.between(timeStart, timeEnd)
        this.timeDuration.text = duration.toString()
      }

      this.summary.text = when (rec.result) {
        EFUploadResult.SUCCEEDED ->
          this.getString(R.string.statusCompleted, timeEnd)

        EFUploadResult.FAILED ->
          this.getString(R.string.statusFailed, timeEnd)

        EFUploadResult.CANCELLED ->
          this.getString(R.string.statusCancelled, timeEnd)

        null ->
          this.getString(R.string.statusRunning)
      }

      this.icon.setImageResource(
        when (rec.result) {
          EFUploadResult.SUCCEEDED ->
            R.drawable.status_upload_item_ok

          EFUploadResult.FAILED ->
            R.drawable.error_24

          EFUploadResult.CANCELLED ->
            R.drawable.cancelled_24

          null ->
            R.drawable.time_24
        }
      )
    }
  }

  private fun onUploadEventsChanged(
    newValue: List<EFUploadEventRecord>
  ) {
    this.adapter.setStatusEvents(newValue)
    this.eventCount.text =
      this.getString(R.string.uploadStatusEventCount, newValue.size)
    this.listView.scrollToPosition(Math.max(0, newValue.size - 1))
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }
}
