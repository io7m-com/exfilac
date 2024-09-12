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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.io7m.exfilac.core.internal.EFUploadEventRecord

class EFUploadStatusAdapter(
  private var items: List<EFUploadEventRecord>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  fun setStatusEvents(
    newList: List<EFUploadEventRecord>
  ) {
    this.items = newList
    this.notifyDataSetChanged()
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecyclerView.ViewHolder {
    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.status_detail_item, parent, false)

    return this.StatusViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int
  ) {
    (holder as? StatusViewHolder)?.bind(this.items[position])
  }

  override fun getItemCount(): Int {
    return this.items.size
  }

  inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    private val message: TextView =
      this.itemView.findViewById(R.id.statusDetailMessage)
    private val file: TextView =
      this.itemView.findViewById(R.id.statusDetailFile)
    private val time: TextView =
      this.itemView.findViewById(R.id.statusDetailTime)
    private val icon: ImageView =
      this.itemView.findViewById(R.id.statusDetailIcon)

    fun bind(
      event: EFUploadEventRecord
    ) {
      this.message.text = event.message
      this.file.text = event.file ?: ""
      this.time.text = event.time.format(EFTimes.dateTimeFormatter)

      when (event.failed) {
        true -> this.icon.setImageResource(R.drawable.error_24)
        false -> this.icon.setImageResource(R.drawable.status_upload_item_ok)
      }
    }
  }
}
