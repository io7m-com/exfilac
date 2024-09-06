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

package com.io7m.exfilac.tests.device

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.os.Bundle
import android.widget.EditText
import com.io7m.exfilac.content_tree.api.EFContentDirectoryType
import com.io7m.exfilac.content_tree.api.EFContentFileType
import com.io7m.exfilac.content_tree.api.EFContentTreeNodeType
import com.io7m.exfilac.content_tree.device.EFContentTreeDevice
import java.net.URI

class EFContentTreeActivity : Activity() {

  private lateinit var text: EditText

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setContentView(R.layout.debug)
    this.text = this.findViewById(R.id.text)

    this.startActivityForResult(Intent(ACTION_OPEN_DOCUMENT_TREE), 10000)
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)

    val contentURI = data?.data.toString()
    this.text.text.append(contentURI)
    this.text.text.append("\n")

    val tree =
      EFContentTreeDevice(EFExApplication.application, this.contentResolver)
        .create(URI.create(contentURI))

    walk(tree)
  }

  private fun walk(tree: EFContentTreeNodeType) {
    this.text.text.append(tree.toString())
    this.text.text.append(" ")
    this.text.text.append(tree.path.path.toString())
    this.text.text.append("\n")

    when (tree) {
      is EFContentFileType -> {
        // Nothing
      }
      is EFContentDirectoryType -> {
        for (node in tree.children) {
          this.walk(node)
        }
      }
    }
  }
}
