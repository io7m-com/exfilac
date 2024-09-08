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
import androidx.recyclerview.widget.RecyclerView
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadStatusCancelled
import com.io7m.exfilac.core.EFUploadStatusCancelling
import com.io7m.exfilac.core.EFUploadStatusFailed
import com.io7m.exfilac.core.EFUploadStatusNone
import com.io7m.exfilac.core.EFUploadStatusRunning
import com.io7m.exfilac.core.EFUploadStatusSucceeded
import java.time.format.DateTimeFormatterBuilder

class EFStatusAdapter(
  private var items: List<EFUploadName>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val dateTimeFormatter =
    DateTimeFormatterBuilder()
      .appendPattern("YYYY")
      .appendLiteral('-')
      .appendPattern("MM")
      .appendLiteral('-')
      .appendPattern("dd")
      .appendLiteral(' ')
      .appendPattern("HH")
      .appendLiteral(':')
      .appendPattern("mm")
      .appendLiteral(':')
      .appendPattern("ss")
      .appendPattern("Z")
      .toFormatter()

  fun setUploadNames(
    uploadList: List<EFUploadName>,
  ) {
    this.items = uploadList
    this.notifyDataSetChanged()
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
    (holder as? StatusViewHolder)?.bind(this.items[position])
  }

  override fun getItemCount(): Int {
    return this.items.size
  }

  enum class OnClick {
    START,
    CANCEL
  }

  inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private var onClick: OnClick = OnClick.START
    private lateinit var uploadName: EFUploadName

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
    }

    private fun executeStartOrCancel() {
      when (this.onClick) {
        OnClick.START -> {
          EFApplication.application.exfilac.uploadStart(this.uploadName, EFUploadReasonManual)
        }

        OnClick.CANCEL -> {
          EFApplication.application.exfilac.uploadCancel(this.uploadName)
        }
      }
    }

    fun bind(
      newUploadName: EFUploadName,
    ) {
      this.uploadName = newUploadName
      this.name.text = newUploadName.value

      when (val status = EFApplication.application.exfilac.uploadStatus(this.uploadName)) {
        is EFUploadStatusNone -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text = view.context.getString(R.string.statusNotRunning)

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusRunning -> {
          this.startCancel.setImageResource(R.drawable.stop_24)
          this.onClick = OnClick.CANCEL

          this.description.text = status.description

          this.progressMajor.progress = (status.progressMajor * 100.0).toInt()
          val minor = status.progressMinor
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
            EFApplication.application.exfilac.uploadStart(status.name, EFUploadReasonManual)
          }

          this.description.text =
            view.context.getString(
              R.string.statusCompleted,
              dateTimeFormatter.format(status.completedAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusCancelled -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text =
            view.context.getString(
              R.string.statusCancelled,
              dateTimeFormatter.format(status.cancelledAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusCancelling -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.description.text =
            view.context.getString(R.string.statusCancelling)

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }

        is EFUploadStatusFailed -> {
          this.startCancel.setImageResource(R.drawable.start_24)
          this.onClick = OnClick.START

          this.startCancel.setOnClickListener {
            EFApplication.application.exfilac.uploadStart(status.name, EFUploadReasonManual)
          }

          this.description.text =
            view.context.getString(
              R.string.statusFailed,
              dateTimeFormatter.format(status.failedAt)
            )

          this.progressMinor.isIndeterminate = false
          this.progressMinor.progress = 0
          this.progressMajor.progress = 0
        }
      }
    }
  }
}
