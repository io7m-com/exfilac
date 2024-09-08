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

import com.io7m.jattribute.core.AttributeReadableType
import java.util.concurrent.CompletableFuture

interface ExfilacType : AutoCloseable {

  val state: AttributeReadableType<EFState>

  val buckets: AttributeReadableType<List<EFBucketConfiguration>>

  val bucketsSelected: AttributeReadableType<Set<EFBucketReferenceName>>

  fun bucketEditBegin(): CompletableFuture<*>

  fun bucketEditCancel(): CompletableFuture<*>

  fun bucketEditConfirm(bucket: EFBucketConfiguration): CompletableFuture<*>

  fun bucketsDelete(names: Set<EFBucketReferenceName>): CompletableFuture<*>

  fun bucketExists(name: EFBucketReferenceName): Boolean

  fun bucketSelectionAdd(name: EFBucketReferenceName): CompletableFuture<*>

  fun bucketSelectionRemove(name: EFBucketReferenceName): CompletableFuture<*>

  fun bucketSelectionClear(): CompletableFuture<*>

  fun bucketSelectionContains(name: EFBucketReferenceName): Boolean

  val uploads: AttributeReadableType<List<EFUploadConfiguration>>

  val uploadsSelected: AttributeReadableType<Set<EFUploadName>>

  fun uploadEditBegin(): CompletableFuture<*>

  fun uploadEditCancel(): CompletableFuture<*>

  fun uploadEditConfirm(upload: EFUploadConfiguration): CompletableFuture<*>

  fun uploadsDelete(names: Set<EFUploadName>): CompletableFuture<*>

  fun uploadExists(name: EFUploadName): Boolean

  fun uploadSelectionAdd(name: EFUploadName): CompletableFuture<*>

  fun uploadSelectionRemove(name: EFUploadName): CompletableFuture<*>

  fun uploadSelectionClear(): CompletableFuture<*>

  fun uploadSelectionContains(name: EFUploadName): Boolean

  val uploadStatus: AttributeReadableType<EFUploadStatusChanged>

  fun uploadStatus(name: EFUploadName): EFUploadStatus

  fun uploadStart(
    name: EFUploadName,
    reason: EFUploadReason
  ): CompletableFuture<*>

  fun uploadCancel(name: EFUploadName)

  fun uploadStartAllAsNecessary(
    reason: EFUploadReason
  ): CompletableFuture<*>

  val networkStatus: AttributeReadableType<EFNetworkStatus>

  fun networkStatusSet(status: EFNetworkStatus)

  val settings: AttributeReadableType<EFSettings>

  fun settingsUpdate(settings: EFSettings): CompletableFuture<*>
}
