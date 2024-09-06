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

package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFDeviceSource
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.ExfilacFactory
import com.io7m.exfilac.core.ExfilacType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ExfilacTest {

  private lateinit var e: ExfilacType
  private lateinit var database: Path
  private lateinit var directory: Path

  @BeforeEach
  fun testSetup(
    @TempDir directory: Path
  ) {
    this.directory = directory
    this.database = directory.resolve("exfilac.db")
  }

  @AfterEach
  fun tearDown() {
    this.e.close()
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOpenEmpty() {
    this.e = this.openAndWait()

    assertEquals(listOf<EFBucketConfiguration>(), this.e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCreate() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    this.e.bucketEditBegin().get()
    this.e.bucketEditConfirm(bucket).get()

    assertEquals(listOf(bucket), this.e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCancel() {
    this.e = this.openAndWait()

    this.e.bucketEditBegin().get()
    this.e.bucketEditCancel().get()

    assertEquals(listOf<EFBucketConfiguration>(), this.e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCreateSelect() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    this.e.bucketEditBegin().get()
    this.e.bucketEditConfirm(bucket).get()

    this.e.bucketSelectionAdd(bucket.referenceName).get()
    assertEquals(setOf(bucket.referenceName), this.e.bucketsSelected.get())
    assertTrue(this.e.bucketSelectionContains(bucket.referenceName))

    this.e.bucketSelectionRemove(bucket.referenceName).get()
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
    assertFalse(this.e.bucketSelectionContains(bucket.referenceName))

    this.e.bucketSelectionAdd(bucket.referenceName).get()
    assertEquals(setOf(bucket.referenceName), this.e.bucketsSelected.get())
    assertTrue(this.e.bucketSelectionContains(bucket.referenceName))

    this.e.bucketSelectionClear().get()
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
    assertFalse(this.e.bucketSelectionContains(bucket.referenceName))

    this.e.bucketSelectionAdd(bucket.referenceName).get()
    assertEquals(setOf(bucket.referenceName), this.e.bucketsSelected.get())
    assertTrue(this.e.bucketSelectionContains(bucket.referenceName))

    this.e.bucketsDelete(setOf(bucket.referenceName)).get()
    assertEquals(listOf<EFBucketConfiguration>(), this.e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), this.e.bucketsSelected.get())
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadConfigurationCreate() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_FIVE_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    assertEquals(listOf(upload), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadConfigurationCancel() {
    this.e = this.openAndWait()

    this.e.uploadEditBegin().get()
    this.e.uploadEditCancel().get()

    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadConfigurationCreateSelect() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_FIVE_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    this.e.uploadSelectionAdd(upload.name).get()
    assertEquals(setOf(upload.name), this.e.uploadsSelected.get())
    assertTrue(this.e.uploadSelectionContains(upload.name))

    this.e.uploadSelectionRemove(upload.name).get()
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
    assertFalse(this.e.uploadSelectionContains(upload.name))

    this.e.uploadSelectionAdd(upload.name).get()
    assertEquals(setOf(upload.name), this.e.uploadsSelected.get())
    assertTrue(this.e.uploadSelectionContains(upload.name))

    this.e.uploadSelectionClear().get()
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
    assertFalse(this.e.uploadSelectionContains(upload.name))

    this.e.uploadSelectionAdd(upload.name).get()
    assertEquals(setOf(upload.name), this.e.uploadsSelected.get())
    assertTrue(this.e.uploadSelectionContains(upload.name))

    this.e.uploadsDelete(setOf(upload.name)).get()
    assertEquals(listOf<EFUploadConfiguration>(), this.e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), this.e.uploadsSelected.get())
  }

  private fun openAndWait(): ExfilacType {
    val c = ExfilacFactory.open(
      contentTrees = EFContentTreeNull,
      s3Uploaders = EFS3UploaderFactoryNull,
      dataDirectory = this.directory
    )
    while (c.state.get() !is EFStateReady) {
      Thread.sleep(1_000L)
    }
    return c
  }
}
