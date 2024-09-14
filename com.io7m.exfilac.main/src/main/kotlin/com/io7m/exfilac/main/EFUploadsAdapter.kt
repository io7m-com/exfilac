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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadEditModel

class EFUploadsAdapter : ListAdapter<EFUploadConfiguration, RecyclerView.ViewHolder>(diffCallback) {

  companion object {
    private val diffCallback =
      object : DiffUtil.ItemCallback<EFUploadConfiguration>() {
        override fun areContentsTheSame(
          oldItem: EFUploadConfiguration,
          newItem: EFUploadConfiguration
        ): Boolean {
          return oldItem == newItem
        }

        override fun areItemsTheSame(
          oldItem: EFUploadConfiguration,
          newItem: EFUploadConfiguration
        ): Boolean {
          return oldItem.name == newItem.name
        }
      }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): RecyclerView.ViewHolder {
    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.upload_item, parent, false)

    return this.UploadViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    (holder as? UploadViewHolder)?.bind(this.getItem(position))
  }

  inner class UploadViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val name: TextView =
      this.itemView.findViewById(R.id.uploadName)
    private val selected: CheckBox =
      this.itemView.findViewById(R.id.uploadCheckBox)

    fun bind(
      configuration: EFUploadConfiguration,
    ) {
      this.name.text = configuration.name.value

      this.selected.isChecked =
        EFApplication.application.exfilac.uploadSelectionContains(configuration.name)

      this.selected.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          EFApplication.application.exfilac.uploadSelectionAdd(configuration.name)
        } else {
          EFApplication.application.exfilac.uploadSelectionRemove(configuration.name)
        }
      }

      this.view.setOnClickListener {
        EFUploadEditModel.setUploadConfiguration(configuration)
        EFApplication.application.exfilac.uploadEditBegin()
      }
    }
  }
}
