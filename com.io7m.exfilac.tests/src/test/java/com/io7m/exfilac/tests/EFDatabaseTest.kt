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

import com.io7m.darco.api.DDatabaseUnit
import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFDeviceSource
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import com.io7m.exfilac.core.EFSettings
import com.io7m.exfilac.core.EFSettingsNetworking
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadResult
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.EFUploadTrigger
import com.io7m.exfilac.core.internal.EFUploadEventID
import com.io7m.exfilac.core.internal.EFUploadEventRecord
import com.io7m.exfilac.core.internal.EFUploadRecord
import com.io7m.exfilac.core.internal.database.EFDatabaseConfiguration
import com.io7m.exfilac.core.internal.database.EFDatabaseFactory
import com.io7m.exfilac.core.internal.database.EFDatabaseType
import com.io7m.exfilac.core.internal.database.EFQBucketDeleteType
import com.io7m.exfilac.core.internal.database.EFQBucketListType
import com.io7m.exfilac.core.internal.database.EFQBucketPutType
import com.io7m.exfilac.core.internal.database.EFQSettingsGetType
import com.io7m.exfilac.core.internal.database.EFQSettingsPutType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationDeleteType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationListType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationPutType
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordAddType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordCreateParameters
import com.io7m.exfilac.core.internal.database.EFQUploadRecordCreateType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordDeleteByAgeType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordGetType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordListParameters
import com.io7m.exfilac.core.internal.database.EFQUploadRecordListType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordMostRecentType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordUpdateType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

class EFDatabaseTest {

  private lateinit var database: EFDatabaseType
  private lateinit var databaseFile: Path
  private lateinit var directory: Path

  @BeforeEach
  fun testSetup(
    @TempDir directory: Path
  ) {
    this.directory = directory
    this.databaseFile = directory.resolve("exfilac.db")
    this.database =
      EFDatabaseFactory()
        .open(
          EFDatabaseConfiguration(
            saxParsersOpt = Optional.empty(),
            filePath = this.databaseFile,
            concurrency = 1
          )
        ) {

        }
  }

  @AfterEach
  fun tearDown() {
    this.database.close()
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOpen() {

  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testCreateBucket() {
    val c0 =
      EFBucketConfiguration(
        referenceName = EFBucketReferenceName("bucket"),
        name = EFBucketName("bucket"),
        region = EFRegion("us-east-1"),
        accessKey = EFAccessKey("aaaa"),
        secret = EFSecretKey("bbbb"),
        accessStyle = EFBucketAccessStyle.PATH_STYLE,
        endpoint = URI.create("https://s3.example.com")
      )

    val c0r =
      EFBucketConfiguration(
        referenceName = EFBucketReferenceName("bucket"),
        name = EFBucketName("bucket2"),
        region = EFRegion("us-east-2"),
        accessKey = EFAccessKey("bbbb"),
        secret = EFSecretKey("cccc"),
        accessStyle = EFBucketAccessStyle.VIRTUALHOST_STYLE,
        endpoint = URI.create("https://s4.example.com")
      )

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(c0)
      t.commit()
      assertEquals(
        listOf(c0),
        t.query(EFQBucketListType::class.java).execute(DDatabaseUnit.UNIT)
      )

      t.query(EFQBucketPutType::class.java).execute(c0r)
      t.commit()
      assertEquals(
        listOf(c0r),
        t.query(EFQBucketListType::class.java).execute(DDatabaseUnit.UNIT)
      )

      t.query(EFQBucketDeleteType::class.java).execute(setOf(c0.referenceName))
      t.commit()
      assertEquals(
        listOf<EFBucketConfiguration>(),
        t.query(EFQBucketListType::class.java).execute(DDatabaseUnit.UNIT)
      )
    }
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testCreateUploadConfiguration() {
    val c0 =
      EFBucketConfiguration(
        referenceName = EFBucketReferenceName("bucket"),
        name = EFBucketName("bucket"),
        region = EFRegion("us-east-1"),
        accessKey = EFAccessKey("aaaa"),
        secret = EFSecretKey("bbbb"),
        accessStyle = EFBucketAccessStyle.PATH_STYLE,
        endpoint = URI.create("https://s3.example.com")
      )
    val u0 =
      EFUploadConfiguration(
        name = EFUploadName("upload"),
        source = EFDeviceSource(URI.create("content://xyz")),
        bucket = c0.referenceName,
        policy = EFUploadPolicy(
          schedule = EFUploadSchedule.EVERY_HOUR,
          triggers = setOf(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
        )
      )
    val u0r =
      EFUploadConfiguration(
        name = EFUploadName("upload"),
        source = EFDeviceSource(URI.create("content://xyza")),
        bucket = c0.referenceName,
        policy = EFUploadPolicy(
          schedule = EFUploadSchedule.EVERY_FIVE_MINUTES,
          triggers = setOf(EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN)
        )
      )

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(c0)
      t.query(EFQUploadConfigurationPutType::class.java).execute(u0)
      t.commit()

      assertEquals(
        listOf(u0),
        t.query(EFQUploadConfigurationListType::class.java)
          .execute(DDatabaseUnit.UNIT)
      )

      t.query(EFQUploadConfigurationPutType::class.java).execute(u0r)
      t.commit()

      assertEquals(
        listOf(u0r),
        t.query(EFQUploadConfigurationListType::class.java)
          .execute(DDatabaseUnit.UNIT)
      )

      t.query(EFQUploadConfigurationDeleteType::class.java).execute(setOf(u0.name))
      t.commit()
      assertEquals(
        listOf<EFUploadConfiguration>(),
        t.query(EFQUploadConfigurationListType::class.java)
          .execute(DDatabaseUnit.UNIT)
      )
    }
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testCreateUploadRecord() {
    val c0 =
      EFBucketConfiguration(
        referenceName = EFBucketReferenceName("bucket"),
        name = EFBucketName("bucket"),
        region = EFRegion("us-east-1"),
        accessKey = EFAccessKey("aaaa"),
        secret = EFSecretKey("bbbb"),
        accessStyle = EFBucketAccessStyle.PATH_STYLE,
        endpoint = URI.create("https://s3.example.com")
      )
    val u0 =
      EFUploadConfiguration(
        name = EFUploadName("upload"),
        source = EFDeviceSource(URI.create("content://xyz")),
        bucket = c0.referenceName,
        policy = EFUploadPolicy(
          schedule = EFUploadSchedule.EVERY_HOUR,
          triggers = setOf(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
        )
      )

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(c0)
      t.query(EFQUploadConfigurationPutType::class.java).execute(u0)
      t.commit()

      val ui =
        t.query(EFQUploadRecordCreateType::class.java)
          .execute(
            EFQUploadRecordCreateParameters(
              OffsetDateTime.parse("2010-01-01T00:00:00+00:00"),
              u0.name,
              "Manually triggered."
            )
          )
      t.commit()

      assertEquals(OffsetDateTime.parse("2010-01-01T00:00:00+00:00"), ui.timeStart)
      assertEquals(null, ui.timeEnd)
      assertEquals(0L, ui.filesUploaded)
      assertEquals(0L, ui.filesFailed)
      assertEquals(0L, ui.filesSkipped)
      assertEquals(0L, ui.filesRequired)
      assertEquals("Manually triggered.", ui.reason)
      assertEquals(null, ui.result)

      t.query(EFQUploadEventRecordAddType::class.java)
        .execute(
          EFUploadEventRecord(
            eventID = EFUploadEventID(ULong.MIN_VALUE),
            uploadID = ui.id,
            time = OffsetDateTime.parse("2010-01-01T00:00:01+00:00"),
            message = "Message 1",
            file = "file1.txt",
            exceptionTrace = null,
            failed = false
          )
        )

      t.query(EFQUploadEventRecordAddType::class.java)
        .execute(
          EFUploadEventRecord(
            eventID = EFUploadEventID(ULong.MIN_VALUE),
            uploadID = ui.id,
            time = OffsetDateTime.parse("2010-01-01T00:00:02+00:00"),
            message = "Message 2",
            file = "file1.txt",
            exceptionTrace = null,
            failed = false
          )
        )

      t.query(EFQUploadEventRecordAddType::class.java)
        .execute(
          EFUploadEventRecord(
            eventID = EFUploadEventID(ULong.MIN_VALUE),
            uploadID = ui.id,
            time = OffsetDateTime.parse("2010-01-01T00:00:03+00:00"),
            message = "Message 3",
            file = "file1.txt",
            exceptionTrace = null,
            failed = false
          )
        )

      val ui1 = ui.copy(
        timeEnd = OffsetDateTime.parse("2010-01-01T00:00:05+00:00"),
        bucket = u0.bucket,
        filesUploaded = 100,
        filesRequired = 102,
        filesFailed = 1,
        filesSkipped = 1,
        result = EFUploadResult.FAILED
      )

      t.query(EFQUploadRecordUpdateType::class.java).execute(ui1)
      t.commit()

      assertEquals(
        listOf(ui1),
        t.query(EFQUploadRecordListType::class.java)
          .execute(
            EFQUploadRecordListParameters(
              newerThan = OffsetDateTime.parse("2010-01-01T00:00:00+00:00").minusDays(1),
              limit = 1000,
              onlyIncludeForName = null
            )
          )
      )
      assertEquals(
        listOf(ui1),
        t.query(EFQUploadRecordListType::class.java)
          .execute(
            EFQUploadRecordListParameters(
              newerThan = OffsetDateTime.parse("2010-01-01T00:00:00+00:00").minusDays(1),
              limit = 1000,
              onlyIncludeForName = ui.configuration
            )
          )
      )
      assertEquals(
        Optional.of(ui1),
        t.query(EFQUploadRecordMostRecentType::class.java)
          .execute(ui.configuration)
      )
      assertEquals(
        Optional.of(ui1),
        t.query(EFQUploadRecordGetType::class.java)
          .execute(ui.id)
      )

      /*
       * Delete all records that are older than a year into the "future". That should
       * mean every record.
       */

      t.query(EFQUploadRecordDeleteByAgeType::class.java)
        .execute(OffsetDateTime.parse("2010-01-01T00:00:00+00:00").plusYears(1L))
      t.commit()

      assertEquals(
        listOf<EFUploadRecord>(),
        t.query(EFQUploadRecordListType::class.java)
          .execute(
            EFQUploadRecordListParameters(
              newerThan = OffsetDateTime.parse("2010-01-01T00:00:00+00:00").minusDays(1),
              limit = 1000,
              onlyIncludeForName = null
            )
          )
      )
    }
  }

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testSettings() {
    this.database.openTransaction().use { t ->
      assertEquals(
        EFSettings.defaults(),
        t.query(EFQSettingsGetType::class.java)
          .execute(DDatabaseUnit.UNIT)
      )

      val newSettings0 =
        EFSettings.defaults()
        .copy(
          networking = EFSettingsNetworking(
            uploadOnCellular = true,
            uploadOnWifi = false
          )
        )

      t.query(EFQSettingsPutType::class.java).execute(newSettings0)
      t.commit()

      assertEquals(
        newSettings0,
        t.query(EFQSettingsGetType::class.java).execute(DDatabaseUnit.UNIT)
      )

      val newSettings1 =
        EFSettings.defaults()
          .copy(
            networking = EFSettingsNetworking(
              uploadOnCellular = true,
              uploadOnWifi = true
            )
          )

      t.query(EFQSettingsPutType::class.java).execute(newSettings1)
      t.commit()

      assertEquals(
        newSettings1,
        t.query(EFQSettingsGetType::class.java).execute(DDatabaseUnit.UNIT)
      )
    }
  }
}
