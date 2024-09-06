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
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketEditModel

class EFBucketsAdapter(
  private var items: List<EFBucketConfiguration>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  fun setBuckets(
    bucketsList: List<EFBucketConfiguration>
  ) {
    this.items = bucketsList
    this.notifyDataSetChanged()
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecyclerView.ViewHolder {
    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.bucket_item, parent, false)

    return this.BucketViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int
  ) {
    (holder as? BucketViewHolder)?.bind(this.items[position])
  }

  override fun getItemCount(): Int {
    return this.items.size
  }

  inner class BucketViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val name: TextView =
      this.itemView.findViewById(R.id.bucketName)
    private val selected: CheckBox =
      this.itemView.findViewById(R.id.bucketCheckBox)

    fun bind(
      configuration: EFBucketConfiguration
    ) {
      this.name.text = configuration.name.value
      this.selected.isChecked =
        EFApplication.application.exfilac.bucketSelectionContains(configuration.referenceName)

      this.selected.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          EFApplication.application.exfilac.bucketSelectionAdd(configuration.referenceName)
        } else {
          EFApplication.application.exfilac.bucketSelectionRemove(configuration.referenceName)
        }
      }

      this.view.setOnClickListener {
        EFBucketEditModel.setBucket(configuration)
        EFApplication.application.exfilac.bucketEditBegin()
      }
    }
  }
}
