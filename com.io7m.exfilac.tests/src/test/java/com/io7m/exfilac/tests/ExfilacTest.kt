package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFUploadConfiguration
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
    this.e = openAndWait()

    assertEquals(listOf<EFBucketConfiguration>(), e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCreate() {
    this.e = openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    this.e.bucketEditBegin().get()
    this.e.bucketEditConfirm(bucket).get()

    assertEquals(listOf(bucket), e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCancel() {
    this.e = openAndWait()

    this.e.bucketEditBegin().get()
    this.e.bucketEditCancel().get()

    assertEquals(listOf<EFBucketConfiguration>(), e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
    assertEquals(listOf<EFUploadConfiguration>(), e.uploads.get())
    assertEquals(setOf<EFUploadConfiguration>(), e.uploadsSelected.get())
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testBucketCreateSelect() {
    this.e = openAndWait()

    val bucket = EFBucketConfiguration(
      EFBucketName("bucket"),
      EFRegion("us-east-1"),
      EFAccessKey("aaaa"),
      EFSecretKey("bbbb"),
      EFBucketAccessStyle.PATH_STYLE,
      URI.create("https://www.example.com")
    )

    this.e.bucketEditBegin().get()
    this.e.bucketEditConfirm(bucket).get()

    this.e.bucketSelectionAdd(bucket.name).get()
    assertEquals(setOf(bucket.name), e.bucketsSelected.get())
    assertTrue(e.bucketSelectionContains(bucket.name))

    this.e.bucketSelectionRemove(bucket.name).get()
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
    assertFalse(e.bucketSelectionContains(bucket.name))

    this.e.bucketSelectionAdd(bucket.name).get()
    assertEquals(setOf(bucket.name), e.bucketsSelected.get())
    assertTrue(e.bucketSelectionContains(bucket.name))

    this.e.bucketSelectionClear().get()
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
    assertFalse(e.bucketSelectionContains(bucket.name))

    this.e.bucketSelectionAdd(bucket.name).get()
    assertEquals(setOf(bucket.name), e.bucketsSelected.get())
    assertTrue(e.bucketSelectionContains(bucket.name))

    this.e.bucketsDelete(setOf(bucket.name)).get()
    assertEquals(listOf<EFBucketConfiguration>(), e.buckets.get())
    assertEquals(setOf<EFBucketConfiguration>(), e.bucketsSelected.get())
  }

  private fun openAndWait(): ExfilacType {
    val c = ExfilacFactory.open(this.directory)
    while (c.state.get() !is EFStateReady) {
      Thread.sleep(1_000L)
    }
    return c
  }
}
