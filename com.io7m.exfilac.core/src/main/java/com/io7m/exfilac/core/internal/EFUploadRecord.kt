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

package com.io7m.exfilac.core.internal

import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadResult
import java.time.OffsetDateTime

data class EFUploadRecord(
  val id: EFUploadID,
  val timeStart: OffsetDateTime,
  val timeEnd: OffsetDateTime?,
  val configuration: EFUploadName,
  val bucket: EFBucketReferenceName?,
  val reason: String,
  val filesRequired: Long,
  val filesSkipped: Long,
  val filesUploaded: Long,
  val filesFailed: Long,
  val result: EFUploadResult?
)
