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

import com.io7m.darco.api.DDatabaseUnit
import com.io7m.exfilac.clock.api.EFClockServiceType
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFNetworkStatus
import com.io7m.exfilac.core.EFSettings
import com.io7m.exfilac.core.EFState
import com.io7m.exfilac.core.EFStateBooting
import com.io7m.exfilac.core.EFStateBucketEditing
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFStateSettingsReadingDocument
import com.io7m.exfilac.core.EFStateUploadConfigurationEditing
import com.io7m.exfilac.core.EFStateUploadStatusViewing
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadReason
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadReasonTime
import com.io7m.exfilac.core.EFUploadReasonTrigger
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_EIGHT_HOURS
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_FIVE_MINUTES
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_FOUR_HOURS
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_HOUR
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_TEN_MINUTES
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_THIRTY_MINUTES
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_TWELVE_HOURS
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_TWENTY_FOUR_HOURS
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_TWENTY_MINUTES
import com.io7m.exfilac.core.EFUploadSchedule.EVERY_TWO_HOURS
import com.io7m.exfilac.core.EFUploadSchedule.ONLY_MANUALLY
import com.io7m.exfilac.core.EFUploadSchedule.ONLY_ON_TRIGGERS
import com.io7m.exfilac.core.EFUploadStatus
import com.io7m.exfilac.core.EFUploadStatusChanged
import com.io7m.exfilac.core.EFUploadStatusNone
import com.io7m.exfilac.core.EFUploadTrigger
import com.io7m.exfilac.core.ExfilacType
import com.io7m.exfilac.core.internal.boot.EFBootContextType
import com.io7m.exfilac.core.internal.boot.EFBootS3Uploader
import com.io7m.exfilac.core.internal.boot.EFBootServiceType
import com.io7m.exfilac.core.internal.boot.EFBootUploads
import com.io7m.exfilac.core.internal.database.EFBootDatabase
import com.io7m.exfilac.core.internal.database.EFDatabaseType
import com.io7m.exfilac.core.internal.database.EFQBucketDeleteType
import com.io7m.exfilac.core.internal.database.EFQBucketListType
import com.io7m.exfilac.core.internal.database.EFQBucketPutType
import com.io7m.exfilac.core.internal.database.EFQSettingsGetType
import com.io7m.exfilac.core.internal.database.EFQSettingsPutType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationDeleteType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationListType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationPutType
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordListParameters
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordListType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordDeleteByAgeType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordGetType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordsMarkCancelledType
import com.io7m.exfilac.core.internal.uploads.EFUploadServiceType
import com.io7m.exfilac.s3_uploader.api.EFS3UploaderFactoryType
import com.io7m.exfilac.service.api.RPServiceDirectory
import com.io7m.exfilac.service.api.RPServiceType
import com.io7m.jattribute.core.AttributeReadableType
import com.io7m.jattribute.core.AttributeType
import com.io7m.jattribute.core.Attributes
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException
import com.io7m.taskrecorder.core.TRNoResult
import com.io7m.taskrecorder.core.TRTaskRecorder
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong

internal class Exfilac private constructor(
  private val resources: CloseableCollectionType<ClosingResourceFailedException>,
  private val databaseExecutor: ExecutorService,
  private val commandExecutor: ExecutorService,
  private val dataDirectory: Path,
  private val cacheDirectory: Path,
  private val contentTrees: EFContentTreeFactoryType,
  private val s3Uploaders: EFS3UploaderFactoryType,
  private val clock: EFClockServiceType
) : ExfilacType {

  private val attributes =
    Attributes.create { e -> logger.debug("Uncaught attribute exception: ", e) }
  private val stateSource: AttributeType<EFState> =
    this.attributes.withValue(EFStateBooting("", 0.0))
  private val bucketsSource: AttributeType<List<EFBucketConfiguration>> =
    this.attributes.withValue(listOf())
  private val bucketsSelectedSource: AttributeType<Set<EFBucketReferenceName>> =
    this.attributes.withValue(setOf())
  private val uploadsSource: AttributeType<List<EFUploadConfiguration>> =
    this.attributes.withValue(listOf())
  private val uploadsSelectedSource: AttributeType<Set<EFUploadName>> =
    this.attributes.withValue(setOf())
  private val statusChangedSource: AttributeType<EFUploadStatusChanged> =
    this.attributes.withValue(EFUploadStatusChanged())
  private val uploadsLastRan =
    ConcurrentHashMap<EFUploadName, OffsetDateTime>()
  private val networkStatusSource: AttributeType<EFNetworkStatus> =
    this.attributes.withValue(EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE)
  private val settingsSource: AttributeType<EFSettings> =
    this.attributes.withValue(EFSettings.defaults())
  private val uploadViewEventsSource: AttributeType<List<EFUploadEventRecord>> =
    this.attributes.withValue(listOf())
  private val uploadViewRecordSource: AttributeType<Optional<EFUploadRecord>> =
    this.attributes.withValue(Optional.empty())

  init {
    this.resources.add(
      this.networkStatusSource.subscribe { oldValue, newValue ->
        this.onNetworkStatusChanged(oldValue, newValue)
      }
    )
  }

  @Volatile
  private var uploadService: EFUploadServiceType? = null

  @Volatile
  private var database: EFDatabaseType? = null

  @Volatile
  private var serviceDirectory: RPServiceDirectory? = null

  companion object {

    private val logger =
      LoggerFactory.getLogger(Exfilac::class.java)

    fun open(
      contentTrees: EFContentTreeFactoryType,
      s3Uploaders: EFS3UploaderFactoryType,
      clock: EFClockServiceType,
      dataDirectory: Path,
      cacheDirectory: Path
    ): ExfilacType {
      val resources =
        CloseableCollection.create()

      val databaseExecutor =
        Executors.newSingleThreadExecutor { r ->
          val thread = Thread(r)
          thread.priority = Thread.MIN_PRIORITY
          thread.name = "com.io7m.exfilac.db"
          thread
        }

      val commandExecutor =
        Executors.newSingleThreadExecutor { r ->
          val thread = Thread(r)
          thread.priority = Thread.MIN_PRIORITY
          thread.name = "com.io7m.exfilac.command"
          thread
        }

      resources.add(AutoCloseable { databaseExecutor.shutdown() })
      resources.add(AutoCloseable { commandExecutor.shutdown() })

      val controller = Exfilac(
        resources = resources,
        databaseExecutor = databaseExecutor,
        commandExecutor = commandExecutor,
        dataDirectory = dataDirectory,
        cacheDirectory = cacheDirectory,
        contentTrees = contentTrees,
        s3Uploaders = s3Uploaders,
        clock = clock
      )

      commandExecutor.execute { controller.boot() }
      return controller
    }
  }

  private fun boot() {
    val taskRecorder =
      TRTaskRecorder.create<Unit>(logger, "Booting...")

    val progress =
      AtomicReference(0.0)
    val services =
      RPServiceDirectory()

    services.register(EFClockServiceType::class.java, this.clock)

    val bootContext =
      object : EFBootContextType {
        override val progress: Double
          get() = progress.get()
        override val services: com.io7m.exfilac.service.api.RPServiceDirectoryType
          get() = services
        override val resources: CloseableCollectionType<*>
          get() = this@Exfilac.resources
      }

    val bootProcesses: List<EFBootServiceType<out RPServiceType>> =
      listOf(
        EFBootDatabase(this.dataDirectory.resolve("exfilac.db")),
        EFBootS3Uploader(this.s3Uploaders),
        EFBootUploads(this.statusChangedSource, this.contentTrees)
      )

    for (index in 0 until bootProcesses.size) {
      val bootProcess =
        bootProcesses[index]
      val message =
        "Starting service: ${bootProcess.description}"
      val recorder =
        taskRecorder.beginSubtaskWithoutResult(message)

      try {
        progress.set(index.toDouble() / bootProcesses.size.toDouble())
        this.booting(message, progress.get())

        val service = bootProcess.execute(bootContext)
        val serviceClass = bootProcess.serviceClass
        services.register(serviceClass, this.unsafeCast(service))
        recorder.setTaskSucceeded("OK", TRNoResult.NO_RESULT)
      } catch (e: Throwable) {
        logger.error("Service error: ", e)
        recorder.setTaskFailed(e.message, Optional.of(e))
      }
    }

    this.serviceDirectory = services
    this.database = services.requireService(EFDatabaseType::class.java)
    this.uploadService = services.requireService(EFUploadServiceType::class.java)
    this.executeDatabase { this.loadData() }

    this@Exfilac.resources.add(
      this@Exfilac.uploadStatus.subscribe { _, _ ->
        this@Exfilac.onUploadStatusChanged()
      }
    )
  }

  private fun onUploadStatusChanged() {
    val currentlyViewedOpt = this.uploadViewRecord.get()
    if (currentlyViewedOpt.isPresent) {
      val currentlyViewed = currentlyViewedOpt.get()
      this.executeDatabase {
        this.database?.openTransaction()?.use { t ->
          this.uploadViewRecordSource.set(
            t.query(EFQUploadRecordGetType::class.java)
              .execute(currentlyViewed.id)
          )
          this.uploadViewEventsSource.set(
            t.query(EFQUploadEventRecordListType::class.java)
              .execute(
                EFQUploadEventRecordListParameters(
                  upload = currentlyViewed.id,
                  timeStart = currentlyViewed.timeStart,
                  limit = Integer.MAX_VALUE
                )
              )
          )
        }
      }
    }
  }

  private fun loadData() {
    try {
      this.database?.openTransaction()?.use { transaction ->
        try {
          transaction.query(EFQUploadRecordsMarkCancelledType::class.java)
            .execute(this.clock.now())
          transaction.commit()
        } catch (e: Exception) {
          logger.debug("Update uploads: ", e)
        }

        try {
          transaction.query(EFQUploadRecordDeleteByAgeType::class.java)
            .execute(this.clock.now().minusDays(7L))
          transaction.commit()
        } catch (e: Exception) {
          logger.debug("Delete old uploads: ", e)
        }

        try {
          this.bucketsSource.set(
            transaction.query(EFQBucketListType::class.java)
              .execute(DDatabaseUnit.UNIT)
          )
        } catch (e: Exception) {
          logger.debug("Bucket list: ", e)
        }

        try {
          this.uploadsSource.set(
            transaction.query(EFQUploadConfigurationListType::class.java)
              .execute(DDatabaseUnit.UNIT),
          )
        } catch (e: Exception) {
          logger.debug("Upload list: ", e)
        }

        try {
          this.settingsSource.set(
            transaction.query(EFQSettingsGetType::class.java)
              .execute(DDatabaseUnit.UNIT)
          )
        } catch (e: Exception) {
          logger.debug("Settings: ", e)
        }
      }
    } finally {
      this.stateSource.set(EFStateReady())
    }
  }

  private fun booting(
    message: String,
    progress: Double,
  ) {
    this.stateSource.set(EFStateBooting(message, progress))
  }

  private fun <A, B> unsafeCast(x: A): B {
    return x as B
  }

  private fun <T> executeCommand(
    f: () -> T
  ): CompletableFuture<T> {
    val cf = CompletableFuture<T>()
    this.commandExecutor.execute {
      try {
        cf.complete(f.invoke())
      } catch (e: Throwable) {
        logger.debug("executeCommand: ", e)
        cf.completeExceptionally(e)
      }
    }
    return cf
  }

  private fun executeDatabase(
    f: () -> Unit
  ): CompletableFuture<*> {
    val cf = CompletableFuture<Unit>()
    this.databaseExecutor.execute {
      for (attempt in 1..10) {
        try {
          cf.complete(f.invoke())
          return@execute
        } catch (e: SQLiteException) {
          logger.debug("executeDatabase: ", e)
          if (e.resultCode != SQLiteErrorCode.SQLITE_BUSY || attempt == 10) {
            cf.completeExceptionally(e)
          }
          Thread.sleep((Math.random() * 1_000L).roundToLong())
        } catch (e: Throwable) {
          logger.debug("executeDatabase: ", e)
          cf.completeExceptionally(e)
          return@execute
        }
      }
    }
    return cf
  }

  override fun close() {
    this.resources.close()
  }

  override val state: AttributeReadableType<EFState> =
    this.stateSource

  override val buckets: AttributeReadableType<List<EFBucketConfiguration>> =
    this.bucketsSource

  override val bucketsSelected: AttributeReadableType<Set<EFBucketReferenceName>> =
    this.bucketsSelectedSource

  override fun bucketEditBegin(): CompletableFuture<*> {
    return this.executeCommand {
      this.stateSource.set(EFStateBucketEditing())
    }
  }

  override fun bucketEditCancel(): CompletableFuture<*> {
    return this.executeCommand { this.stateSource.set(EFStateReady()) }
  }

  override fun bucketEditConfirm(
    bucket: EFBucketConfiguration
  ): CompletableFuture<*> {
    return this.executeDatabase {
      this.database?.openTransaction()?.use { transaction ->
        transaction.query(EFQBucketPutType::class.java).execute(bucket)
        transaction.commit()

        this.bucketsSource.set(
          transaction.query(EFQBucketListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
        this.stateSource.set(EFStateReady())
      }
    }
  }

  override fun bucketsDelete(
    names: Set<EFBucketReferenceName>
  ): CompletableFuture<*> {
    return this.executeDatabase {
      this.database?.openTransaction()?.use { transaction ->
        transaction.query(EFQBucketDeleteType::class.java).execute(names)
        transaction.commit()

        this.bucketsSelectedSource.set(setOf())
        this.bucketsSource.set(
          transaction.query(EFQBucketListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
        this.uploadsSource.set(
          transaction.query(EFQUploadConfigurationListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
      }
    }
  }

  override fun bucketExists(
    name: EFBucketReferenceName
  ): Boolean {
    return this.buckets.get()
      .any { c -> c.referenceName == name }
  }

  override fun bucketSelectionAdd(
    name: EFBucketReferenceName
  ): CompletableFuture<*> {
    return this.executeCommand {
      this.bucketsSelectedSource.set(this.bucketsSelectedSource.get().plus(name))
    }
  }

  override fun bucketSelectionRemove(
    name: EFBucketReferenceName
  ): CompletableFuture<*> {
    return this.executeCommand {
      this.bucketsSelectedSource.set(this.bucketsSelectedSource.get().minus(name))
    }
  }

  override fun bucketSelectionClear(): CompletableFuture<*> {
    return this.executeCommand {
      this.bucketsSelectedSource.set(setOf())
    }
  }

  override fun bucketSelectionContains(
    name: EFBucketReferenceName
  ): Boolean {
    return this.bucketsSelected.get().contains(name)
  }

  override val uploads: AttributeReadableType<List<EFUploadConfiguration>> =
    this.uploadsSource

  override val uploadsSelected: AttributeReadableType<Set<EFUploadName>> =
    this.uploadsSelectedSource

  override fun uploadEditBegin(): CompletableFuture<*> {
    return this.executeCommand {
      this.stateSource.set(EFStateUploadConfigurationEditing())
    }
  }

  override fun uploadEditCancel(): CompletableFuture<*> {
    return this.executeCommand { this.stateSource.set(EFStateReady()) }
  }

  override fun uploadEditConfirm(
    upload: EFUploadConfiguration
  ): CompletableFuture<*> {
    return this.executeDatabase {
      this.database?.openTransaction()?.use { transaction ->
        transaction.query(EFQUploadConfigurationPutType::class.java).execute(upload)
        transaction.commit()

        this.uploadsSource.set(
          transaction.query(EFQUploadConfigurationListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
        this.stateSource.set(EFStateReady())
      }
    }
  }

  override fun uploadsDelete(
    names: Set<EFUploadName>
  ): CompletableFuture<*> {
    return this.executeDatabase {
      this.database?.openTransaction()?.use { transaction ->
        transaction.query(EFQUploadConfigurationDeleteType::class.java).execute(names)
        transaction.commit()

        this.uploadsSelectedSource.set(setOf())
        this.uploadsSource.set(
          transaction.query(EFQUploadConfigurationListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
      }
    }
  }

  override fun uploadExists(
    name: EFUploadName
  ): Boolean {
    return this.uploads.get()
      .any { c -> c.name == name }
  }

  override fun uploadSelectionAdd(
    name: EFUploadName
  ): CompletableFuture<*> {
    return this.executeCommand {
      this.uploadsSelectedSource.set(this.uploadsSelectedSource.get().plus(name))
    }
  }

  override fun uploadSelectionRemove(
    name: EFUploadName
  ): CompletableFuture<*> {
    return this.executeCommand {
      this.uploadsSelectedSource.set(this.uploadsSelectedSource.get().minus(name))
    }
  }

  override fun uploadSelectionClear(): CompletableFuture<*> {
    return this.executeCommand {
      this.uploadsSelectedSource.set(setOf())
    }
  }

  override fun uploadSelectionContains(
    name: EFUploadName
  ): Boolean {
    return this.uploadsSelected.get().contains(name)
  }

  override val uploadStatus: AttributeReadableType<EFUploadStatusChanged> =
    this.statusChangedSource

  override fun uploadStatus(name: EFUploadName): EFUploadStatus {
    return this.uploadService?.status(name) ?: EFUploadStatusNone(name, null)
  }

  override fun uploadStart(
    name: EFUploadName,
    reason: EFUploadReason
  ): CompletableFuture<*> {
    val future =
      this.uploadService?.upload(name, reason)
        ?: CompletableFuture.completedFuture(Unit)

    future.thenRun { this.uploadsLastRan[name] = this.clock.now() }
    return future
  }

  override fun uploadCancel(name: EFUploadName) {
    this.uploadService?.cancel(name)
  }

  override fun uploadStartAllAsNecessary(
    reason: EFUploadReason
  ): CompletableFuture<*> {
    return this.executeCommand {
      for (upload in this.uploads.get()) {
        if (this.uploadShouldRun(upload, reason, this.uploadsLastRan[upload.name])) {
          this.uploadStart(upload.name, reason)
        }
      }
    }
  }

  override fun uploadViewSelect(
    uploadName: EFUploadName,
    uploadId: EFUploadID?
  ): CompletableFuture<*> {
    return this.executeDatabase {
      val mostRecent = this.uploadService?.mostRecent(uploadName)
      val uploadIdActual = uploadId ?: mostRecent?.id
      if (uploadIdActual == null) {
        return@executeDatabase
      }

      this.database?.openTransaction()?.use { t ->
        this.uploadViewRecordSource.set(Optional.of(mostRecent!!))
        this.uploadViewEventsSource.set(
          t.query(EFQUploadEventRecordListType::class.java)
            .execute(
              EFQUploadEventRecordListParameters(
                upload = uploadIdActual,
                timeStart = mostRecent.timeStart,
                limit = Integer.MAX_VALUE
              )
            )
        )
      }

      this.stateSource.set(EFStateUploadStatusViewing(uploadIdActual))
    }
  }

  override fun uploadViewCancel(): CompletableFuture<*> {
    return this.executeCommand {
      this.stateSource.set(EFStateReady())
      this.uploadViewRecordSource.set(Optional.empty())
      this.uploadViewEventsSource.set(listOf())
    }
  }

  override val uploadViewEvents: AttributeReadableType<List<EFUploadEventRecord>> =
    this.uploadViewEventsSource

  override val uploadViewRecord: AttributeReadableType<Optional<EFUploadRecord>> =
    this.uploadViewRecordSource

  override val networkStatus: AttributeReadableType<EFNetworkStatus> =
    this.networkStatusSource

  override fun networkStatusSet(status: EFNetworkStatus) {
    this.networkStatusSource.set(status)
  }

  override val settings: AttributeReadableType<EFSettings>
    get() = this.settingsSource

  override fun settingsUpdate(settings: EFSettings): CompletableFuture<*> {
    return this.executeDatabase {
      this.database?.openTransaction()?.use { transaction ->
        transaction.query(EFQSettingsPutType::class.java).execute(settings)
        transaction.commit()
        this.settingsSource.set(settings)
      }
    }
  }

  override fun settingsDocumentOpen(
    bundledURI: URI,
    externalURI: URI
  ) {
    this.stateSource.set(
      EFStateSettingsReadingDocument(
        bundledURI = bundledURI,
        externalURI = externalURI
      )
    )
  }

  override fun settingsDocumentClose() {
    this.stateSource.set(EFStateReady())
  }

  override fun settingsDumpLogs(output: Path): CompletableFuture<*> {
    return this.executeCommand {
      val logPackFile =
        output.toFile()
      val logDirectory =
        this.cacheDirectory.resolve("log")
      val logFiles =
        Files.list(logDirectory)
          .sorted()
          .collect(Collectors.toList())

      FileOutputStream(logPackFile).use { fileOut ->
        BufferedOutputStream(fileOut).use { bufferedOut ->
          ZipOutputStream(bufferedOut).use { zipOut ->
            for (file in logFiles) {
              val entryName = file.fileName.toString()
              val entry = ZipEntry(entryName)
              zipOut.putNextEntry(entry)
              Files.copy(file, zipOut)
              zipOut.closeEntry()
            }
            zipOut.finish()
          }
        }
      }
      output
    }
  }

  private fun uploadPermittedByNetworkStatus(): Boolean {
    val networkNow =
      this.networkStatus.get()
    val settingsNow =
      this.settings.get()

    return when (networkNow) {
      EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE -> {
        false
      }

      EFNetworkStatus.NETWORK_STATUS_CELLULAR -> {
        settingsNow.networking.uploadOnCellular
      }

      EFNetworkStatus.NETWORK_STATUS_WIFI -> {
        settingsNow.networking.uploadOnWifi
      }
    }
  }

  private fun uploadShouldRun(
    upload: EFUploadConfiguration,
    reason: EFUploadReason,
    timeLast: OffsetDateTime?
  ): Boolean {
    if (reason == EFUploadReasonManual) {
      return true
    }

    if (this.settings.get().paused) {
      return false
    }

    if (!uploadPermittedByNetworkStatus()) {
      return false
    }

    return when (reason) {
      EFUploadReasonManual -> true
      EFUploadReasonTime -> {
        when (upload.policy.schedule) {
          EVERY_FIVE_MINUTES ->
            this.timeSatisfies(timeLast, Duration.of(5L, ChronoUnit.MINUTES))

          EVERY_TEN_MINUTES ->
            this.timeSatisfies(timeLast, Duration.of(10L, ChronoUnit.MINUTES))

          EVERY_TWENTY_MINUTES ->
            this.timeSatisfies(timeLast, Duration.of(20L, ChronoUnit.MINUTES))

          EVERY_THIRTY_MINUTES ->
            this.timeSatisfies(timeLast, Duration.of(30L, ChronoUnit.MINUTES))

          EVERY_HOUR ->
            this.timeSatisfies(timeLast, Duration.of(60L, ChronoUnit.MINUTES))

          ONLY_ON_TRIGGERS,
          ONLY_MANUALLY -> false

          EVERY_TWO_HOURS ->
            this.timeSatisfies(timeLast, Duration.of(2L, ChronoUnit.HOURS))

          EVERY_FOUR_HOURS ->
            this.timeSatisfies(timeLast, Duration.of(4L, ChronoUnit.HOURS))

          EVERY_EIGHT_HOURS ->
            this.timeSatisfies(timeLast, Duration.of(8L, ChronoUnit.HOURS))

          EVERY_TWELVE_HOURS ->
            this.timeSatisfies(timeLast, Duration.of(12L, ChronoUnit.HOURS))

          EVERY_TWENTY_FOUR_HOURS ->
            this.timeSatisfies(timeLast, Duration.of(24L, ChronoUnit.HOURS))
        }
      }

      is EFUploadReasonTrigger -> {
        upload.policy.triggers.contains(reason.trigger)
      }
    }
  }

  private fun timeSatisfies(
    timeLast: OffsetDateTime?,
    duration: Duration
  ): Boolean {
    if (timeLast != null) {
      val timeNow = this.clock.now()
      return timeLast.isBefore(timeNow.minus(duration))
    }
    return true
  }

  private fun onNetworkStatusChanged(
    oldValue: EFNetworkStatus,
    newValue: EFNetworkStatus
  ) {
    when (oldValue) {
      /*
       * If the network was unavailable, then moving to any network type is considered
       * "becoming available".
       */

      EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE -> {
        when (newValue) {
          EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE -> Unit
          EFNetworkStatus.NETWORK_STATUS_CELLULAR,
          EFNetworkStatus.NETWORK_STATUS_WIFI -> {
            this.uploadStartAllAsNecessary(
              EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
            )
          }
        }
      }

      /*
       * If the network was cellular, then moving to wifi is considered "becoming available".
       */

      EFNetworkStatus.NETWORK_STATUS_CELLULAR -> {
        when (newValue) {
          EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE -> Unit
          EFNetworkStatus.NETWORK_STATUS_CELLULAR -> Unit
          EFNetworkStatus.NETWORK_STATUS_WIFI -> {
            this.uploadStartAllAsNecessary(
              EFUploadReasonTrigger(EFUploadTrigger.TRIGGER_WHEN_NETWORK_AVAILABLE)
            )
          }
        }
      }

      /*
       * If the network was wifi, then there are no state changes considered "becoming available".
       */

      EFNetworkStatus.NETWORK_STATUS_WIFI -> {
        Unit
      }
    }
  }
}
