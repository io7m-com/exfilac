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
import com.io7m.exfilac.core.EFNetworkStatus
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadReasonTime
import com.io7m.exfilac.core.EFUploadReasonTrigger
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.EFUploadTrigger
import com.io7m.exfilac.core.ExfilacFactory
import com.io7m.exfilac.core.ExfilacType
import com.io7m.exfilac.core.internal.EFUploadEventRecord
import com.io7m.exfilac.core.internal.EFUploadRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.util.Optional
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

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunManual() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.ONLY_MANUALLY,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()
    this.e.uploadStart(upload.name, Duration.ofMillis(0L), EFUploadReasonManual).get()
    this.e.uploadViewSelect(upload.name, uploadId = null).get()

    assertEquals(
      "Upload started.",
      this.e.uploadViewEvents.get().get(0).message
    )

    this.e.uploadViewCancel().get()
    assertEquals(listOf<EFUploadEventRecord>(), this.e.uploadViewEvents.get())
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunPhotoTaken() {
    this.e = this.openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.ONLY_MANUALLY,
        setOf(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()
    this.e.uploadStart(
      upload.name,
      Duration.ofMillis(0L),
      EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
    ).get()
    this.e.uploadViewSelect(upload.name, uploadId = null).get()

    assertEquals(
      "Upload started.",
      this.e.uploadViewEvents.get().get(0).message
    )
    assertEquals(
      "Upload was triggered because a photo was taken.",
      this.e.uploadViewRecord.get().get().reason
    )

    this.e.uploadViewCancel().get()
    assertEquals(listOf<EFUploadEventRecord>(), this.e.uploadViewEvents.get())
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunNetworkStatusChanged() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.ONLY_MANUALLY,
        setOf(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()

    assertEquals(
      "Upload started.",
      this.e.uploadViewEvents.get().get(0).message
    )
    assertEquals(
      "Upload was triggered because the network became available.",
      this.e.uploadViewRecord.get().get().reason
    )

    this.e.uploadViewCancel().get()
    assertEquals(listOf<EFUploadEventRecord>(), this.e.uploadViewEvents.get())
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
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

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()

    assertEquals(
      "Upload started.",
      this.e.uploadViewEvents.get().get(0).message
    )
    assertEquals(
      "Upload was triggered due to the time-based schedule.",
      this.e.uploadViewRecord.get().get().reason
    )

    this.e.uploadViewCancel().get()
    assertEquals(listOf<EFUploadEventRecord>(), this.e.uploadViewEvents.get())
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged5() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
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

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(6L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock moved forward enough, another upload should have been triggered.
     */

    assertNotEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged5NotEnough() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
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

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload. The clock didn't move enough
     * for the schedule to result in a new upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(4L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock didn't move forward enough, the most recent upload should still be
     * the first one.
     */

    assertEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged10() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_TEN_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(11L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock moved forward enough, another upload should have been triggered.
     */

    assertNotEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged10NotEnough() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_TEN_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload. The clock didn't move enough
     * for the schedule to result in a new upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(9L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock didn't move forward enough, the most recent upload should still be
     * the first one.
     */

    assertEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged20() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_TWENTY_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(21L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock moved forward enough, another upload should have been triggered.
     */

    assertNotEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged20NotEnough() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_TWENTY_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload. The clock didn't move enough
     * for the schedule to result in a new upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(19L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock didn't move forward enough, the most recent upload should still be
     * the first one.
     */

    assertEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged30() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_THIRTY_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(31L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock moved forward enough, another upload should have been triggered.
     */

    assertNotEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged30NotEnough() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_THIRTY_MINUTES,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload. The clock didn't move enough
     * for the schedule to result in a new upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(29L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock didn't move forward enough, the most recent upload should still be
     * the first one.
     */

    assertEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged60() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_HOUR,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(61L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock moved forward enough, another upload should have been triggered.
     */

    assertNotEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChanged60NotEnough() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.EVERY_HOUR,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * Schedule an upload to create a starting time.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec0 = this.e.uploadViewRecord.get().get()

    /*
     * Move the clock forward and schedule another upload. The clock didn't move enough
     * for the schedule to result in a new upload.
     */

    EFClockMock.timeNow = EFClockMock.timeNow.plusMinutes(59L)
    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    Thread.sleep(1_000L)
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    val rec1 = this.e.uploadViewRecord.get().get()

    /*
     * Because the clock didn't move forward enough, the most recent upload should still be
     * the first one.
     */

    assertEquals(rec1.id, rec0.id)
    assertEquals("Upload was triggered due to the time-based schedule.", rec1.reason)
  }


  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChangedOnlyTriggersNot() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.ONLY_ON_TRIGGERS,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * The upload doesn't have a time-based schedule. Nothing will happen.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadRunTimeChangedOnlyManualNot() {
    this.e = this.openAndWait()
    this.e.networkStatusSet(EFNetworkStatus.NETWORK_STATUS_WIFI)

    val bucket = EFBucketConfiguration(
      EFBucketReferenceName("bucket"),
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("http://localhost")
    )

    val upload = EFUploadConfiguration(
      EFUploadName("upload"),
      EFDeviceSource(URI.create("content://xyz")),
      EFBucketReferenceName("bucket"),
      EFUploadPolicy(
        EFUploadSchedule.ONLY_MANUALLY,
        setOf()
      )
    )

    this.e.bucketEditConfirm(bucket).get()
    this.e.uploadEditBegin().get()
    this.e.uploadEditConfirm(upload).get()

    /*
     * The upload doesn't have a time-based schedule. Nothing will happen.
     */

    this.e.uploadStartAllAsNecessary(EFUploadReasonTime).get()
    this.e.uploadViewSelect(upload.name, uploadId = null).get()
    assertEquals(Optional.empty<EFUploadRecord>(), this.e.uploadViewRecord.get())
  }

  private fun openAndWait(): ExfilacType {
    val c = ExfilacFactory.open(
      contentTrees = EFContentTreeNull,
      s3Uploaders = EFS3UploaderFactoryNull,
      dataDirectory = this.directory,
      cacheDirectory = this.directory,
      clock = EFClockMock
    )
    while (c.state.get() !is EFStateReady) {
      Thread.sleep(1_000L)
    }
    c.uploadStartAllSetDelayMaximum(Duration.ofMillis(1L))
    return c
  }
}
