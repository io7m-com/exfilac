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

object EFUploadEditModel {

  enum class EditOperation {
    CREATE,
    MODIFY
  }

  var editOperation: EditOperation = EditOperation.CREATE

  private var unsaved: Boolean = false

  var bucket: EFBucketReferenceName? = null
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var name: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var source: String = ""
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var schedule: EFUploadSchedule = EFUploadSchedule.EVERY_HOUR
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  var triggers: Set<EFUploadTrigger> = setOf()
    set(value) {
      val diff = field != value
      field = value
      this.unsaved = diff
    }

  fun addTrigger(t: EFUploadTrigger) {
    this.triggers = this.triggers.plus(t)
    this.unsaved = true
  }

  fun removeTrigger(t: EFUploadTrigger) {
    this.triggers = this.triggers.minus(t)
    this.unsaved = true
  }

  fun clear() {
    this.editOperation = EditOperation.CREATE
    this.name = ""
    this.source = ""
    this.bucket = null
    this.schedule = EFUploadSchedule.EVERY_HOUR
    this.triggers = setOf()
    this.unsaved = false
  }

  fun setUploadConfiguration(
    configuration: EFUploadConfiguration
  ) {
    this.editOperation = EditOperation.MODIFY
    this.name = configuration.name.value
    this.source = configuration.source.toString()
    this.bucket = configuration.bucket
    this.schedule = configuration.policy.schedule
    this.triggers = configuration.policy.triggers.toSet()
    this.unsaved = false
  }

  fun isUnsaved(): Boolean {
    return this.unsaved
  }
}
