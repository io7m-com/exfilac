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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketEditModel
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.CREATE
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.MODIFY
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import java.net.URI

class EFFragmentBucketEditing : EFFragment() {

  private lateinit var refName: EditText
  private lateinit var accessKey: EditText
  private lateinit var accessStyle: Spinner
  private lateinit var endpoint: EditText
  private lateinit var name: EditText
  private lateinit var region: EditText
  private lateinit var saveButton: MenuItem
  private lateinit var secret: EditText
  private lateinit var toolbar: MaterialToolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.bucket_edit, container, false)

    this.name =
      view.findViewById(R.id.bucketEditName)
    this.refName =
      view.findViewById(R.id.bucketEditRefName)
    this.region =
      view.findViewById(R.id.bucketEditRegion)
    this.accessKey =
      view.findViewById(R.id.bucketEditAccessKey)
    this.secret =
      view.findViewById(R.id.bucketEditSecret)
    this.endpoint =
      view.findViewById(R.id.bucketEditEndpoint)
    this.accessStyle =
      view.findViewById(R.id.bucketEditAccessStyle)
    this.toolbar =
      view.findViewById(R.id.bucketEditAppBar)

    this.toolbar.setNavigationIcon(R.drawable.back_24)
    this.toolbar.setNavigationOnClickListener {
      EFApplication.application.exfilac.bucketEditCancel()
    }
    this.toolbar.menu.clear()
    val activity = this.requireActivity()
    activity.menuInflater.inflate(R.menu.buckets_edit_save, this.toolbar.menu)
    this.saveButton = this.toolbar.menu.findItem(R.id.bucketEditMenuSave)

    this.name.setText(EFBucketEditModel.name)
    this.refName.setText(EFBucketEditModel.referenceName)
    this.region.setText(EFBucketEditModel.region)
    this.accessKey.setText(EFBucketEditModel.accessKey)
    this.secret.setText(EFBucketEditModel.secret)
    this.endpoint.setText(EFBucketEditModel.endpoint.toString())
    this.accessStyle.setSelection(EFBucketEditModel.accessStyle.ordinal)

    this.name.addTextChangedListener {
      EFBucketEditModel.name = this.name.text.toString().trim()
      this.validate()
    }
    this.refName.addTextChangedListener {
      EFBucketEditModel.referenceName = this.refName.text.toString().trim()
      this.validate()
    }
    this.region.addTextChangedListener {
      EFBucketEditModel.region = this.region.text.toString().trim()
      this.validate()
    }
    this.accessKey.addTextChangedListener {
      EFBucketEditModel.accessKey = this.accessKey.text.toString().trim()
      this.validate()
    }
    this.secret.addTextChangedListener {
      EFBucketEditModel.secret = this.secret.text.toString().trim()
      this.validate()
    }
    this.endpoint.addTextChangedListener {
      try {
        EFBucketEditModel.endpoint = URI.create(this.endpoint.text.toString().trim())
        this.endpoint.error = null
      } catch (e: Throwable) {
        this.endpoint.error = EFApplication.application.getString(R.string.bucketInvalidURI)
      }
      this.validate()
    }

    this.accessStyle.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
          parent: AdapterView<*>,
          view: View,
          position: Int,
          id: Long,
        ) {
          try {
            EFBucketEditModel.accessStyle = EFBucketAccessStyle.entries.toTypedArray()[position]
          } catch (e: Exception) {
            // Nothing can be done.
          }
        }

        override fun onNothingSelected(
          parent: AdapterView<*>
        ) {
          // Nothing
        }
      }

    this.saveButton.setOnMenuItemClickListener {
      EFApplication.application.exfilac.bucketEditConfirm(
        EFBucketConfiguration(
          referenceName = EFBucketReferenceName(EFBucketEditModel.referenceName),
          name = EFBucketName(EFBucketEditModel.name),
          region = EFRegion(EFBucketEditModel.region),
          accessKey = EFAccessKey(EFBucketEditModel.accessKey),
          secret = EFSecretKey(EFBucketEditModel.secret),
          accessStyle = EFBucketEditModel.accessStyle,
          endpoint = EFBucketEditModel.endpoint
        )
      )
      EFBucketEditModel.clear()
      true
    }

    when (EFBucketEditModel.editOperation) {
      CREATE -> {
        this.toolbar.setTitle(R.string.bucketEditCreate)
        this.refName.isEnabled = true
      }

      MODIFY -> {
        this.toolbar.setTitle(R.string.bucketEditModify)
        this.refName.isEnabled = false
      }
    }

    this.validate()
    return view
  }

  private fun validate() {
    var ok = this.validateName()
    ok = ok and this.validateReferenceName()
    ok = ok and this.validateRegion()
    ok = ok and this.validateAccessKey()
    ok = ok and this.validateSecret()
    ok = ok and this.validateEndpoint()
    this.saveButton.isEnabled = ok
  }

  private fun validateEndpoint(): Boolean {
    try {
      URI.create(this.endpoint.text.toString().trim())
    } catch (e: Exception) {
      this.endpoint.error = EFApplication.application.getString(R.string.bucketInvalidURI)
      return false
    }
    return true
  }

  private fun validateSecret(): Boolean {
    return try {
      EFSecretKey(this.secret.text.toString().trim())
      this.secret.error = null
      true
    } catch (e: Exception) {
      this.secret.error = e.message
      false
    }
  }

  private fun validateAccessKey(): Boolean {
    return try {
      EFAccessKey(this.accessKey.text.toString().trim())
      this.accessKey.error = null
      true
    } catch (e: Exception) {
      this.accessKey.error = e.message
      false
    }
  }

  private fun validateRegion(): Boolean {
    return try {
      EFRegion(this.region.text.toString().trim())
      this.region.error = null
      true
    } catch (e: Exception) {
      this.region.error = e.message
      false
    }
  }

  private fun validateName(): Boolean {
    return try {
      EFBucketName(this.name.text.toString().trim())
      this.name.error = null
      true
    } catch (e: Exception) {
      this.name.error = e.message
      false
    }
  }

  private fun validateReferenceName(): Boolean {
    return when (EFBucketEditModel.editOperation) {
      CREATE -> {
        try {
          this.validateNameNonexistent(EFBucketReferenceName(this.refName.text.toString().trim()))
        } catch (e: Exception) {
          this.refName.error = e.message
          false
        }
      }

      MODIFY -> {
        this.refName.error = null
        true
      }
    }
  }

  private fun validateNameNonexistent(
    bucketName: EFBucketReferenceName
  ): Boolean {
    return if (EFApplication.application.exfilac.bucketExists(bucketName)) {
      this.refName.error = EFApplication.application.getString(R.string.bucketRefNameTaken)
      false
    } else {
      this.refName.error = null
      true
    }
  }
}
