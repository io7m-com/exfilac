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

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.io7m.exfilac.core.EFBucketEditModel
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.CREATE
import com.io7m.exfilac.core.EFBucketEditModel.EditOperation.MODIFY
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFDeviceSource
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadEditModel
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.EFUploadTrigger
import java.net.URI

class EFFragmentUploadConfigurationEditing : EFFragment() {

  private lateinit var bucketHeader: TextView
  private lateinit var pathSelectCallback: ActivityResultLauncher<Intent>
  private lateinit var bucket: Spinner
  private lateinit var name: EditText
  private lateinit var path: EditText
  private lateinit var pathSelect: Button
  private lateinit var saveButton: MenuItem
  private lateinit var schedule: Spinner
  private lateinit var toolbar: MaterialToolbar
  private lateinit var triggerNetwork: SwitchMaterial
  private lateinit var triggerPhone: SwitchMaterial
  private lateinit var triggerPhoto: SwitchMaterial

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.upload_edit, container, false)

    this.name =
      view.findViewById(R.id.uploadEditName)
    this.path =
      view.findViewById(R.id.uploadEditPath)
    this.toolbar =
      view.findViewById(R.id.uploadEditAppBar)
    this.bucket =
      view.findViewById(R.id.uploadEditBucket)
    this.bucketHeader =
      view.findViewById(R.id.uploadEditBucketHeader)
    this.schedule =
      view.findViewById(R.id.uploadEditSchedule)
    this.triggerPhone =
      view.findViewById(R.id.uploadTriggerPhoneCallEnded)
    this.triggerPhoto =
      view.findViewById(R.id.uploadTriggerPhotoTaken)
    this.triggerNetwork =
      view.findViewById(R.id.uploadTriggerNetworkAvailable)
    this.pathSelect =
      view.findViewById(R.id.uploadEditPathSelect)

    this.toolbar.setNavigationIcon(R.drawable.back_24)
    this.toolbar.menu.clear()
    val activity = this.requireActivity()
    activity.menuInflater.inflate(R.menu.uploads_edit_save, this.toolbar.menu)
    this.saveButton = this.toolbar.menu.findItem(R.id.uploadEditMenuSave)

    this.name.setText(EFUploadEditModel.name)
    this.path.setText(EFUploadEditModel.source)
    this.schedule.setSelection(EFUploadEditModel.schedule.ordinal)
    this.triggerPhone.isChecked =
      EFUploadEditModel.triggers.contains(EFUploadTrigger.TRIGGER_WHEN_PHONE_CALL_ENDED)
    this.triggerPhoto.isChecked =
      EFUploadEditModel.triggers.contains(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
    this.triggerNetwork.isChecked =
      EFUploadEditModel.triggers.contains(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)

    this.name.addTextChangedListener {
      EFUploadEditModel.name = this.name.text.toString().trim()
      this.validate()
    }
    this.path.addTextChangedListener {
      EFUploadEditModel.source = this.path.text.toString().trim()
      this.validate()
    }
    this.schedule.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        EFUploadEditModel.schedule = EFUploadSchedule.entries.toTypedArray()[position]
      }

      override fun onNothingSelected(
        parent: AdapterView<*>?
      ) {
        // Nothing
      }
    }
    this.bucket.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        EFUploadEditModel.bucket =
          EFApplication.application.exfilac.buckets.get().get(position).referenceName
      }

      override fun onNothingSelected(
        parent: AdapterView<*>?
      ) {
        EFUploadEditModel.bucket = null
      }
    }

    this.triggerPhoto.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        EFUploadEditModel.triggers.add(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      } else {
        EFUploadEditModel.triggers.remove(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      }
    }
    this.triggerNetwork.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        EFUploadEditModel.triggers.add(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
      } else {
        EFUploadEditModel.triggers.remove(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
      }
    }
    this.triggerPhone.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        EFUploadEditModel.triggers.add(EFUploadTrigger.TRIGGER_WHEN_PHONE_CALL_ENDED)
      } else {
        EFUploadEditModel.triggers.remove(EFUploadTrigger.TRIGGER_WHEN_PHONE_CALL_ENDED)
      }
    }

    when (EFUploadEditModel.editOperation) {
      EFUploadEditModel.EditOperation.CREATE -> {
        this.toolbar.setTitle(R.string.uploadEditCreate)
        this.name.isEnabled = true
      }

      EFUploadEditModel.EditOperation.MODIFY -> {
        this.toolbar.setTitle(R.string.uploadEditModify)
        this.name.isEnabled = false
      }
    }

    this.saveButton.setOnMenuItemClickListener {
      EFApplication.application.exfilac.uploadEditConfirm(
        EFUploadConfiguration(
          name = EFUploadName(EFUploadEditModel.name),
          source = EFDeviceSource(URI.create(EFUploadEditModel.source)),
          bucket = EFUploadEditModel.bucket!!,
          policy = EFUploadPolicy(
            EFUploadEditModel.schedule,
            EFUploadEditModel.triggers
          )
        )
      )
      EFUploadEditModel.clear()
      true
    }

    /*
     * Doing basic file I/O in Android now involves submitting a full medical history and stool
     * sample.
     *
     * Essentially, we publish an Intent that allows for receiving a content URI that points to
     * the chosen file or directory. Of course, it isn't enough to have to use a proprietary,
     * non-standard, Android-specific API just to get the bare minimum stream access to a file:
     * You also have to ask for permission to keep a reference to that file or directory in
     * the future. If you don't do this, attempting to read from the file after the process
     * has exited and restarted in the future will result in a SecurityException.
     */

    this.pathSelectCallback =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data = result.data?.data
          if (data != null) {
            EFApplication.application.contentResolver.takePersistableUriPermission(
              data,
              Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            EFUploadEditModel.source = data.toString()
            this.path.setText(EFUploadEditModel.source)
          }
        }
      }

    this.pathSelect.setOnClickListener {
      this.pathSelectCallback.launch(Intent(ACTION_OPEN_DOCUMENT_TREE))
    }

    this.validate()
    return view
  }

  private fun validate() {
    var ok = this.validateName()
    ok = ok and this.validateSource()
    ok = ok and this.validateBucket()
    this.saveButton.isEnabled = ok
  }

  private fun validateBucket(): Boolean {
    return try {
      if (EFUploadEditModel.bucket == null) {
        this.bucketHeader.error = getString(R.string.uploadBucketNotSpecified)
        false
      } else {
        this.bucketHeader.error = null
        true
      }
    } catch (e: Exception) {
      this.bucketHeader.error = e.message
      false
    }
  }

  private fun validateSource(): Boolean {
    return try {
      EFDeviceSource(URI.create(this.path.text.toString().trim()))
      this.path.error = null
      true
    } catch (e: Exception) {
      this.path.error = e.message
      false
    }
  }

  private fun validateName(): Boolean {
    return when (EFBucketEditModel.editOperation) {
      CREATE -> {
        try {
          this.validateNameNonexistent(EFUploadName(this.name.text.toString().trim()))
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
    uploadName: EFUploadName
  ): Boolean {
    return if (EFApplication.application.exfilac.uploadExists(uploadName)) {
      this.name.error = EFApplication.application.getString(R.string.uploadNameTaken)
      false
    } else {
      this.name.error = null
      true
    }
  }

  override fun onStart() {
    super.onStart()

    val adapter: ArrayAdapter<EFBucketName> =
      ArrayAdapter<EFBucketName>(
        this.requireContext(),
        android.R.layout.simple_spinner_item,
        EFApplication.application.exfilac.buckets.get().map { b -> b.name }
      )

    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    this.bucket.setAdapter(adapter)
  }
}
