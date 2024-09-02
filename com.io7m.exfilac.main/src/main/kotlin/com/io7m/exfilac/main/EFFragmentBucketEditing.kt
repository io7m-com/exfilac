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
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketEditModel
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.CREATE
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.MODIFY
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import java.net.URI

class EFFragmentBucketEditing : EFFragment() {

  private lateinit var accessKey: EditText
  private lateinit var accessStyle: Spinner
  private lateinit var cancel: Button
  private lateinit var confirm: Button
  private lateinit var endpoint: EditText
  private lateinit var header: TextView
  private lateinit var name: EditText
  private lateinit var region: EditText
  private lateinit var secret: EditText

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.bucket_edit, container, false)

    this.header =
      view.findViewById(R.id.bucketHeader)
    this.name =
      view.findViewById(R.id.bucketName)
    this.region =
      view.findViewById(R.id.bucketRegion)
    this.accessKey =
      view.findViewById(R.id.bucketAccessKey)
    this.secret =
      view.findViewById(R.id.bucketSecret)
    this.endpoint =
      view.findViewById(R.id.bucketEndpoint)
    this.accessStyle =
      view.findViewById(R.id.bucketAccessStyle)
    this.confirm =
      view.findViewById(R.id.bucketConfirm)
    this.cancel =
      view.findViewById(R.id.bucketCancel)

    this.name.setText(EFBucketEditModel.name)
    this.region.setText(EFBucketEditModel.region)
    this.accessKey.setText(EFBucketEditModel.accessKey)
    this.secret.setText(EFBucketEditModel.secret)
    this.endpoint.setText(EFBucketEditModel.endpoint.toString())
    this.accessStyle.setSelection(this.accessStyleIndexOf(EFBucketEditModel.accessStyle))

    this.name.addTextChangedListener {
      EFBucketEditModel.name = this.name.text.toString().trim()
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
            EFBucketEditModel.accessStyle =
              EFBucketAccessStyle.valueOf(
                this@EFFragmentBucketEditing.accessStyle.adapter.getItem(position)
                  .toString()
                  .trim()
              )
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

    this.confirm.setOnClickListener {
      EFApplication.application.exfilac.bucketEditConfirm(
        EFBucketConfiguration(
          name = EFBucketName(EFBucketEditModel.name),
          region = EFRegion(EFBucketEditModel.region),
          accessKey = EFAccessKey(EFBucketEditModel.accessKey),
          secret = EFSecretKey(EFBucketEditModel.secret),
          accessStyle = EFBucketEditModel.accessStyle,
          endpoint = EFBucketEditModel.endpoint
        )
      )
    }

    when (EFBucketEditModel.editOperation) {
      CREATE -> {
        this.confirm.setText(R.string.create)
        this.header.setText(R.string.bucketEditCreate)
        this.name.isEnabled = true
      }

      MODIFY -> {
        this.confirm.setText(R.string.modify)
        this.header.setText(R.string.bucketEditModify)
        this.name.isEnabled = false
      }
    }

    this.cancel.setOnClickListener {
      EFApplication.application.exfilac.bucketEditCancel()
    }

    this.validate()
    return view
  }

  /**
   * Spinners are weakly typed. In order to set the value of the spinner correctly, we need
   * to search the spinner for the string value that corresponds to the enum value that we
   * want to set. This is yet another problem that systems that aren't Android solved long ago.
   */

  private fun accessStyleIndexOf(
    accessStyle: EFBucketAccessStyle
  ): Int {
    for (index in 0 until this.accessStyle.adapter.count) {
      try {
        val style = this.accessStyle.adapter.getItem(index)
        if (EFBucketAccessStyle.valueOf(style.toString()) == accessStyle) {
          return index
        }
      } catch (e: Exception) {
        // Nothing we can do about it.
      }
    }
    return 0
  }

  private fun validate() {
    var ok = this.validateName()
    ok = ok and this.validateRegion()
    ok = ok and this.validateAccessKey()
    ok = ok and this.validateSecret()
    ok = ok and this.validateEndpoint()
    this.confirm.isEnabled = ok
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
    return when (EFBucketEditModel.editOperation) {
      CREATE -> {
        try {
          this.validateNameNonexistent(EFBucketName(this.name.text.toString().trim()))
        } catch (e: Exception) {
          this.name.error = e.message
          false
        }
      }

      MODIFY -> {
        this.name.error = null
        true
      }
    }
  }

  private fun validateNameNonexistent(
    bucketName: EFBucketName
  ): Boolean {
    return if (EFApplication.application.exfilac.bucketExists(bucketName)) {
      this.name.error = EFApplication.application.getString(R.string.bucketNameTaken)
      false
    } else {
      this.name.error = null
      true
    }
  }
}
