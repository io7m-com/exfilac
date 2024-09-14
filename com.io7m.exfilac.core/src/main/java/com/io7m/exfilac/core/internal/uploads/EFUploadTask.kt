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

package com.io7m.exfilac.core.internal.uploads

import com.io7m.darco.api.DDatabaseUnit
import com.io7m.exfilac.clock.api.EFClockServiceType
import com.io7m.exfilac.content_tree.api.EFContentDirectoryType
import com.io7m.exfilac.content_tree.api.EFContentFileType
import com.io7m.exfilac.content_tree.api.EFContentPath
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.content_tree.api.EFContentTreeNodeType
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadReason
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadReasonTime
import com.io7m.exfilac.core.EFUploadReasonTrigger
import com.io7m.exfilac.core.EFUploadResult
import com.io7m.exfilac.core.EFUploadStatus
import com.io7m.exfilac.core.EFUploadStatusCancelled
import com.io7m.exfilac.core.EFUploadStatusCancelling
import com.io7m.exfilac.core.EFUploadStatusChanged
import com.io7m.exfilac.core.EFUploadStatusFailed
import com.io7m.exfilac.core.EFUploadStatusRunning
import com.io7m.exfilac.core.EFUploadStatusSucceeded
import com.io7m.exfilac.core.EFUploadTrigger
import com.io7m.exfilac.core.internal.EFUploadEventID
import com.io7m.exfilac.core.internal.EFUploadEventRecord
import com.io7m.exfilac.core.internal.EFUploadRecord
import com.io7m.exfilac.core.internal.database.EFDatabaseType
import com.io7m.exfilac.core.internal.database.EFQBucketListType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationListType
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordAddType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordCreateParameters
import com.io7m.exfilac.core.internal.database.EFQUploadRecordCreateType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordUpdateType
import com.io7m.exfilac.s3_uploader.api.EFS3TransferStatistics
import com.io7m.exfilac.s3_uploader.api.EFS3UploadRequest
import com.io7m.exfilac.s3_uploader.api.EFS3UploaderType
import com.io7m.jattribute.core.AttributeType
import com.io7m.taskrecorder.core.TRNoResult
import com.io7m.taskrecorder.core.TRTaskRecorder
import com.io7m.taskrecorder.core.TRTaskRecorderType
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.nio.file.Paths
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

class EFUploadTask(
  val database: EFDatabaseType,
  val statusChangedSource: AttributeType<EFUploadStatusChanged>,
  val contentTrees: EFContentTreeFactoryType,
  val s3Uploader: EFS3UploaderType,
  val name: EFUploadName,
  val reason: EFUploadReason,
  val onStatusChanged: (EFUploadStatus) -> Unit,
  val clock: EFClockServiceType,
) {

  private val uploadRecord: AtomicReference<EFUploadRecord> = AtomicReference()
  private var bucketConfiguration: EFBucketConfiguration? = null
  private var uploadConfiguration: EFUploadConfiguration? = null

  private val logger =
    LoggerFactory.getLogger(EFUploadTask::class.java)
  private val cancelled =
    AtomicBoolean(false)
  private val failed =
    AtomicBoolean(false)
  private val files =
    mutableListOf<EFContentFileType>()

  private val taskRecorder: TRTaskRecorderType<TRNoResult> =
    TRTaskRecorder.create(this.logger, "Upload files.")

  fun execute() {
    try {
      this.createRecord()
      this.findConfiguration()
      this.listFiles()
      this.uploadFiles()
      this.finish()
    } catch (e: CancellationException) {
      this.setCancelled()
      throw e
    } catch (e: Throwable) {
      this.setFailed(e)
      throw e
    }
  }

  private fun createRecord() {
    val step = this.taskRecorder.beginStep("Creating upload record…")

    try {
      this.checkCancelled()
      this.setRunningFromTask(0.0, null)

      this.database.openTransaction()
        .use { transaction ->
          this.uploadRecord.set(
            transaction.query(EFQUploadRecordCreateType::class.java)
              .execute(
                EFQUploadRecordCreateParameters(
                  timeStart = this.clock.now(),
                  uploadName = this.name,
                  reason = reasonTextOf(this.reason)
                )
              )
          )
          transaction.commit()
        }

      this.onUploadInformativeEvent(
        EFContentPath(URI.create("urn:unused"), listOf()),
        "Upload started."
      )
      step.setStepSucceeded("OK")
    } catch (e: Throwable) {
      step.setStepFailed(this.exceptionMessage(e), e)
      this.failed.set(true)
      throw e
    }
  }

  private fun reasonTextOf(
    reason: EFUploadReason
  ): String {
    return when (reason) {
      EFUploadReasonManual -> "Upload was triggered manually."
      EFUploadReasonTime -> "Upload was triggered due to the time-based schedule."
      is EFUploadReasonTrigger ->
        when (reason.trigger) {
          EFUploadTrigger.TRIGGER_WHEN_PHOTO_TAKEN -> {
            "Upload was triggered because a photo was taken."
          }

          EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE -> {
            "Upload was triggered because the network became available."
          }
        }
    }
  }

  private fun uploadFiles() {
    val step = this.taskRecorder.beginStep("Uploading files…")

    try {
      this.checkCancelled()
      this.setRunningFromTask(0.0, null)

      for ((index, file) in this.files.withIndex()) {
        this.uploadFile(index, file)
      }

      step.setStepSucceeded("OK")
    } catch (e: Throwable) {
      step.setStepFailed(this.exceptionMessage(e), e)
      this.failed.set(true)
      throw e
    }
  }

  private fun uploadFile(
    fileIndex: Int,
    file: EFContentFileType
  ) {
    val step = this.taskRecorder.beginStep("Uploading ${file.path.asS3Path()}…")
    try {
      this.checkCancelled()
      this.setRunningFromTaskIndexed(fileIndex, this.files.size, 0.0)

      val uConfiguration = this.uploadConfiguration
      checkNotNull(uConfiguration)
      val bConfiguration = this.bucketConfiguration
      checkNotNull(bConfiguration)

      val uploadInfo =
        EFS3UploadRequest(
          accessKey = bConfiguration.accessKey.value,
          secretKey = bConfiguration.secret.value,
          region = bConfiguration.region.value,
          endpoint = bConfiguration.endpoint,
          bucket = bConfiguration.name.value,
          path = file.path.asS3Path(),
          contentType = "application/octet-stream",
          size = file.size,
          streams = { file.read() },
          pathStyle = when (bConfiguration.accessStyle) {
            EFBucketAccessStyle.VIRTUALHOST_STYLE -> false
            EFBucketAccessStyle.PATH_STYLE -> true
          },
          temporaryDirectory = Paths.get("/tmp"),
          onStatistics = { statistics ->
            this.onUploadStatisticsReceived(statistics, fileIndex)
          },
          onInformativeEvent = { message ->
            this.onUploadInformativeEvent(file.path, message)
          },
          onError = { exception ->
            this.onUploadError(file.path, exception)
          },
          onFileSkipped = {
            this.onUploadFileAlreadyUploaded(file.path)
          },
          onFileSuccessfullyUploaded = {
            this.onUploadFileSuccessfullyUploaded(file.path)
          }
        )

      this.s3Uploader.create(uploadInfo, this.clock).use { upload -> upload.execute() }
      step.setStepSucceeded("OK")
    } catch (e: Throwable) {
      step.setStepFailed(this.exceptionMessage(e), e)
      this.failed.set(true)
    }
  }

  private fun onUploadFileSuccessfullyUploaded(
    path: EFContentPath
  ) {
    this.uploadRecord.getAndUpdate { r -> r.copy(filesUploaded = r.filesUploaded + 1L) }

    val e = EFUploadEventRecord(
      eventID = EFUploadEventID(ULong.MIN_VALUE),
      uploadID = this.uploadRecord.get().id,
      time = this.clock.now(),
      message = "File was successfully uploaded.",
      file = path.asS3Path(),
      exceptionTrace = null,
      failed = false
    )

    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.query(EFQUploadEventRecordAddType::class.java).execute(e)
      transaction.commit()
    }
  }

  private fun onUploadFileAlreadyUploaded(
    path: EFContentPath
  ) {
    this.uploadRecord.getAndUpdate { r -> r.copy(filesSkipped = r.filesSkipped + 1L) }

    val e = EFUploadEventRecord(
      eventID = EFUploadEventID(ULong.MIN_VALUE),
      uploadID = this.uploadRecord.get().id,
      time = this.clock.now(),
      message = "File has already been uploaded and so will not be uploaded again.",
      file = path.asS3Path(),
      exceptionTrace = null,
      failed = false
    )

    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.query(EFQUploadEventRecordAddType::class.java).execute(e)
      transaction.commit()
    }
  }

  private fun onUploadError(
    path: EFContentPath,
    exception: Throwable
  ) {
    this.uploadRecord.getAndUpdate { r -> r.copy(filesFailed = r.filesFailed + 1L) }

    val e = EFUploadEventRecord(
      eventID = EFUploadEventID(ULong.MIN_VALUE),
      uploadID = this.uploadRecord.get().id,
      time = this.clock.now(),
      message = exception.message ?: exception.javaClass.name,
      file = path.asS3Path(),
      exceptionTrace = this.exceptionTextOf(exception),
      failed = true
    )

    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.query(EFQUploadEventRecordAddType::class.java).execute(e)
      transaction.commit()
    }
  }

  private fun exceptionTextOf(
    exception: Throwable
  ): String {
    val w = StringWriter()
    val p = PrintWriter(w)
    exception.printStackTrace(p)
    p.flush()
    return w.toString()
  }

  private fun onUploadInformativeEvent(
    path: EFContentPath,
    message: String
  ) {
    this.logger.debug("{}", message)

    val e = EFUploadEventRecord(
      eventID = EFUploadEventID(ULong.MIN_VALUE),
      uploadID = this.uploadRecord.get().id,
      time = this.clock.now(),
      message = message,
      file = path.asS3Path(),
      exceptionTrace = null,
      failed = false
    )

    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadEventRecordAddType::class.java).execute(e)
      transaction.commit()
    }
  }

  private fun onUploadStatisticsReceived(
    statistics: EFS3TransferStatistics,
    fileIndex: Int
  ) {
    val minor =
      statistics.octetsTransferred.toDouble() / statistics.octetsExpected.toDouble()

    this.setRunningFromTaskIndexed(
      fileIndex = fileIndex,
      size = this.files.size,
      progressMinor = minor
    )
  }

  /**
   * Collect the files that need to be uploaded. The list of files is shuffled such that, if
   * a single file repeatedly fails to upload, the failure doesn't prevent other files from being
   * uploaded.
   */

  private fun listFiles() {
    val step = this.taskRecorder.beginStep("Listing files…")
    try {
      this.checkCancelled()
      this.setRunningFromTask(0.0, null)

      val uConfiguration = this.uploadConfiguration
      checkNotNull(uConfiguration)
      val bConfiguration = this.bucketConfiguration
      checkNotNull(bConfiguration)

      this.listFilesTreeWalk(this.contentTrees.create(uConfiguration.source.value))
      this.files.shuffle()

      this.uploadRecord.getAndUpdate { r -> r.copy(filesRequired = this.files.size.toLong()) }
      this.database.openTransaction().use { transaction ->
        transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
        transaction.commit()
      }

      step.setStepSucceeded("Collected ${this.files.size} files.")
    } catch (e: Throwable) {
      step.setStepFailed(this.exceptionMessage(e), e)
      this.failed.set(true)
      throw e
    }
  }

  private fun exceptionMessage(e: Throwable): String {
    return e.message ?: e.javaClass.name
  }

  private fun checkCancelled() {
    if (this.cancelled.get()) {
      throw CancellationException("Cancelled task")
    }
  }

  private fun listFilesTreeWalk(
    node: EFContentTreeNodeType
  ) {
    this.checkCancelled()

    return when (node) {
      is EFContentFileType -> {
        this.files.add(node)
        Unit
      }

      is EFContentDirectoryType -> {
        for (e in node.children) {
          this.listFilesTreeWalk(e)
        }
      }

      else -> {
        throw IllegalStateException("Unrecognized node type: ${node.javaClass}")
      }
    }
  }

  private fun findConfiguration() {
    val step = this.taskRecorder.beginStep("Loading configurations…")
    try {
      this.checkCancelled()
      this.setRunningFromTask(0.0, null)

      this.database.openTransaction().use { transaction ->
        this.checkCancelled()

        val uConfiguration =
          transaction.query(EFQUploadConfigurationListType::class.java)
            .execute(DDatabaseUnit.UNIT)
            .find { c -> c.name == this.name }

        this.uploadConfiguration = uConfiguration
        if (uConfiguration == null) {
          throw IllegalStateException("No such upload configuration.")
        }

        this.checkCancelled()

        val bConfiguration =
          transaction.query(EFQBucketListType::class.java)
            .execute(DDatabaseUnit.UNIT)
            .find { c -> c.referenceName == uConfiguration.bucket }

        this.bucketConfiguration = bConfiguration
        if (bConfiguration == null) {
          throw IllegalStateException("No such bucket configuration.")
        }

        this.uploadRecord.getAndUpdate { r -> r.copy(bucket = bConfiguration.referenceName) }
        transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
        transaction.commit()

        step.setStepSucceeded("OK")
      }
    } catch (e: Throwable) {
      step.setStepFailed(this.exceptionMessage(e), e)
      this.failed.set(true)
      throw e
    }
  }

  private fun setRunningFromTaskIndexed(
    fileIndex: Int,
    size: Int,
    progressMinor: Double
  ) {
    this.setRunningFromTask(
      progressMajor = fileIndex.toDouble() / size.toDouble(),
      progressMinor = progressMinor
    )
  }

  private fun setRunningFromTask(
    progressMajor: Double,
    progressMinor: Double?
  ) {
    this.onStatusChanged(
      EFUploadStatusRunning(
        name = this.name,
        id = this.uploadRecord.get()?.id,
        description = this.taskRecorder.stepCurrent().toStep().description,
        progressMajor = progressMajor,
        progressMinor = progressMinor
      )
    )
    this.statusChangedSource.set(EFUploadStatusChanged())
  }

  private fun finish() {
    if (this.failed.get()) {
      this.uploadRecord.getAndUpdate { r ->
        r.copy(result = EFUploadResult.FAILED, timeEnd = this.clock.now())
      }
      this.database.openTransaction().use { transaction ->
        transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
        transaction.commit()
      }

      this.taskRecorder.setTaskFailed("One or more steps failed.")
      this.onStatusChanged(
        EFUploadStatusFailed(
          name = this.name,
          id = this.uploadRecord.get()?.id,
          result = this.taskRecorder.toTask(),
          failedAt = this.clock.now()
        )
      )
      this.statusChangedSource.set(EFUploadStatusChanged())
      return
    }

    this.uploadRecord.getAndUpdate { r ->
      r.copy(result = EFUploadResult.SUCCEEDED, timeEnd = this.clock.now())
    }
    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.commit()
    }

    this.taskRecorder.setTaskSucceeded("OK", TRNoResult.NO_RESULT)
    this.onStatusChanged(
      EFUploadStatusSucceeded(
        name = this.name,
        id = this.uploadRecord.get()?.id,
        completedAt = this.clock.now()
      )
    )
    this.statusChangedSource.set(EFUploadStatusChanged())
  }

  private fun setCancelled() {
    this.taskRecorder.setTaskSucceeded("Cancelled", TRNoResult.NO_RESULT)

    this.uploadRecord.getAndUpdate { r ->
      r.copy(result = EFUploadResult.CANCELLED, timeEnd = this.clock.now())
    }
    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.commit()
    }

    this.onStatusChanged(
      EFUploadStatusCancelled(
        name = this.name,
        id = this.uploadRecord.get()?.id,
        cancelledAt = this.clock.now()
      )
    )
    this.statusChangedSource.set(EFUploadStatusChanged())
  }

  private fun setFailed(e: Throwable) {
    this.taskRecorder.setTaskFailed(this.exceptionMessage(e), Optional.of(e))

    this.uploadRecord.getAndUpdate { r ->
      r.copy(result = EFUploadResult.FAILED, timeEnd = this.clock.now())
    }
    this.database.openTransaction().use { transaction ->
      transaction.query(EFQUploadRecordUpdateType::class.java).execute(this.uploadRecord.get())
      transaction.commit()
    }

    this.onStatusChanged(
      EFUploadStatusFailed(
        name = this.name,
        id = this.uploadRecord.get()?.id,
        result = this.taskRecorder.toTask(),
        failedAt = this.clock.now()
      )
    )
    this.statusChangedSource.set(EFUploadStatusChanged())
  }

  fun cancel() {
    this.cancelled.set(true)
    this.onStatusChanged(EFUploadStatusCancelling(this.name, id = this.uploadRecord.get()?.id))
    this.statusChangedSource.set(EFUploadStatusChanged())
  }
}
