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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.io7m.exfilac.core.EFTimes
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadStatus
import com.io7m.exfilac.core.EFUploadStatusCancelled
import com.io7m.exfilac.core.EFUploadStatusCancelling
import com.io7m.exfilac.core.EFUploadStatusFailed
import com.io7m.exfilac.core.EFUploadStatusNone
import com.io7m.exfilac.core.EFUploadStatusRunning
import com.io7m.exfilac.core.EFUploadStatusSucceeded
import java.time.Duration

class EFStatusAdapter : ListAdapter<EFUploadStatus, RecyclerView.ViewHolder>(diffCallback) {

  companion object {
    private val diffCallback =
      object : DiffUtil.ItemCallback<EFUploadStatus>() {
        override fun areContentsTheSame(
          oldItem: EFUploadStatus,
          newItem: EFUploadStatus
        ): Boolean {
          return oldItem == newItem
        }

        override fun areItemsTheSame(
          oldItem: EFUploadStatus,
          newItem: EFUploadStatus
        ): Boolean {
          return oldItem.id == newItem.id
        }
      }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): RecyclerView.ViewHolder {
    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.status_item, parent, false)

    return this.StatusViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    (holder as? StatusViewHolder)?.bind(this.getItem(position))
  }

  enum class OnClick {
    START,
    CANCEL
  }

  inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private lateinit var uploadStatus: EFUploadStatus

    private var onClick: OnClick = OnClick.START

    private val name: TextView =
      this.itemView.findViewById(R.id.statusName)
    private val description: TextView =
      this.itemView.findViewById(R.id.statusDescription)
    private val progressMajor: ProgressBar =
      this.itemView.findViewById(R.id.statusProgressMajor)
    private val progressMinor: ProgressBar =
      this.itemView.findViewById(R.id.statusProgressMinor)
    private val startCancel: ImageView =
      this.itemView.findViewById(R.id.statusStartOrCancel)

    init {
      this.startCancel.setOnClickListener {
        this.executeStartOrCancel()
      }
      this.view.setOnClickListener {
        EFApplication.application.exfilac.uploadViewSelect(this.uploadStatus.name, uploadId = null)
      }
    }

    private fun executeStartOrCancel() {
      when (this.onClick) {
        OnClick.START -> {
          EFApplication.application.exfilac.uploadStart(
            this.uploadStatus.name,
            Duration.ofMillis(0L),
            EFUploadReasonManual
          )
        }

        OnClick.CANCEL -> {
          EFApplication.application.exfilac.uploadCancel(this.uploadStatus.name)
        }
      }
    }

    fun bind(
      newUploadStatus: EFUploadStatus,
    ) {
      this.uploadStatus = newUploadStatus
      this.name.text = newUploadStatus.name.value

      when (newUploadStatus) {
        is EFUploadStatusNone -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text = this.view.context.getString(R.string.statusNotRunning)

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusRunning -> {
          this.startCancel.setImageResource(R.drawable.stop_24)
          this.onClick = OnClick.CANCEL

          this.description.text = newUploadStatus.description

          this.progressMajor.progress = (newUploadStatus.progressMajor * 100.0).toInt()
          val minor = newUploadStatus.progressMinor
          if (minor == null) {
            this.progressMinor.isIndeterminate = true
          } else {
            this.progressMinor.isIndeterminate = false
            this.progressMinor.progress = (minor * 100.0).toInt()
          }
        }

        is EFUploadStatusSucceeded -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.startCancel.setOnClickListener {
            EFApplication.application.exfilac.uploadStart(
              newUploadStatus.name,
              Duration.ofMillis(0L),
              EFUploadReasonManual
            )
          }

          this.description.text =
            this.view.context.getString(
              R.string.statusCompleted,
              EFTimes.dateTimeFormatter.format(newUploadStatus.completedAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusCancelled -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text =
            this.view.context.getString(
              R.string.statusCancelled,
              EFTimes.dateTimeFormatter.format(newUploadStatus.cancelledAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusCancelling -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text =
            this.view.context.getString(R.string.statusCancelling)

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusFailed -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.startCancel.setOnClickListener {
            EFApplication.application.exfilac.uploadStart(
              newUploadStatus.name,
              Duration.ofMillis(0L),
              EFUploadReasonManual
            )
          }

          this.description.text =
            this.view.context.getString(
              R.string.statusFailed,
              EFTimes.dateTimeFormatter.format(newUploadStatus.failedAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }
      }
    }
  }
}
