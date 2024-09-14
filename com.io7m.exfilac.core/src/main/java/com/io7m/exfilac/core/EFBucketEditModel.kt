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

package com.io7m.exfilac.core

import java.net.URI

object EFBucketEditModel {

  enum class EditOperation {
    CREATE,
    MODIFY
  }

  var referenceName: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var editOperation: EditOperation = EditOperation.CREATE

  private var unsaved: Boolean = false

  var name: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var region: String = "us-east-1"
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var accessKey: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var secret: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var endpoint: URI = URI.create("https://s3.example.com")
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var accessStyle: EFBucketAccessStyle = EFBucketAccessStyle.VIRTUALHOST_STYLE
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  fun clear() {
    this.editOperation = EditOperation.CREATE
    this.referenceName = ""
    this.name = ""
    this.region = "us-east-1"
    this.accessKey = ""
    this.secret = ""
    this.endpoint = URI.create("https://s3.example.com")
    this.accessStyle = EFBucketAccessStyle.VIRTUALHOST_STYLE
    this.unsaved = false
  }

  fun setBucket(
    configuration: EFBucketConfiguration
  ) {
    this.editOperation = EditOperation.MODIFY
    this.referenceName = configuration.referenceName.value
    this.name = configuration.name.value
    this.region = configuration.region.value
    this.accessKey = configuration.accessKey.value
    this.secret = configuration.secret.value
    this.endpoint = configuration.endpoint
    this.accessStyle = configuration.accessStyle
    this.unsaved = false
  }

  fun isUnsaved(): Boolean {
    return this.unsaved
  }
}
