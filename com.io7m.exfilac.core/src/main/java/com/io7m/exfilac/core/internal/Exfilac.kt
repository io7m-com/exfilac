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
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFState
import com.io7m.exfilac.core.EFStateBooting
import com.io7m.exfilac.core.EFStateBucketEditing
import com.io7m.exfilac.core.EFStateReady
import com.io7m.exfilac.core.EFStateUploadConfigurationEditing
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.ExfilacType
import com.io7m.exfilac.core.internal.repetoir.RPServiceDirectory
import com.io7m.exfilac.core.internal.repetoir.RPServiceDirectoryType
import com.io7m.exfilac.core.internal.repetoir.RPServiceType
import com.io7m.jattribute.core.AttributeReadableType
import com.io7m.jattribute.core.AttributeType
import com.io7m.jattribute.core.Attributes
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException
import com.io7m.taskrecorder.core.TRNoResult
import com.io7m.taskrecorder.core.TRTaskRecorder
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal class Exfilac private constructor(
  private val resources: CloseableCollectionType<ClosingResourceFailedException>,
  private val databaseExecutor: ExecutorService,
  private val commandExecutor: ExecutorService,
  private val stateSource: AttributeType<EFState>,
  private val bucketsSource: AttributeType<List<EFBucketConfiguration>>,
  private val bucketsSelectedSource: AttributeType<Set<EFBucketName>>,
  private val uploadsSource: AttributeType<List<EFUploadConfiguration>>,
  private val uploadsSelectedSource: AttributeType<Set<EFUploadName>>,
  private val dataDirectory: Path,
) : ExfilacType {

  @Volatile
  private var database: EFDatabaseType? = null

  @Volatile
  private var serviceDirectory: RPServiceDirectory? = null

  companion object {

    private val logger =
      LoggerFactory.getLogger(Exfilac::class.java)

    fun open(
      dataDirectory: Path,
    ): ExfilacType {
      val attributes =
        Attributes.create { e -> this.logger.debug("Uncaught attribute exception: ", e) }

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
        stateSource = attributes.withValue(EFStateBooting("", 0.0)),
        bucketsSource = attributes.withValue(listOf()),
        bucketsSelectedSource = attributes.withValue(setOf()),
        uploadsSource = attributes.withValue(listOf()),
        uploadsSelectedSource = attributes.withValue(setOf()),
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

    val bootContext =
      object : EFBootContextType {
        override val progress: Double
          get() = progress.get()
        override val services: RPServiceDirectoryType
          get() = services
        override val resources: CloseableCollectionType<*>
          get() = this@Exfilac.resources
      }

    val bootProcesses: List<EFBootServiceType<out RPServiceType>> =
      listOf(
        EFBootDatabase(this.dataDirectory.resolve("exfilac.db")),
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
    this.stateSource.set(EFStateReady())
    this.executeDatabase { this.loadData() }
  }

  private fun loadData() {
    this.database!!.openTransaction().use { transaction ->
      this.bucketsSource.set(
        transaction.query(EFQBucketListType::class.java)
          .execute(DDatabaseUnit.UNIT)
      )
      this.uploadsSource.set(
        transaction.query(EFQUploadConfigurationListType::class.java)
          .execute(DDatabaseUnit.UNIT),
      )
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

  private fun executeCommand(
    f: () -> Unit
  ): CompletableFuture<*> {
    val cf = CompletableFuture<Unit>()
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
      try {
        cf.complete(f.invoke())
      } catch (e: Throwable) {
        logger.debug("executeDatabase: ", e)
        cf.completeExceptionally(e)
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

  override val bucketsSelected: AttributeReadableType<Set<EFBucketName>> =
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
      this.database!!.openTransaction().use { transaction ->
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
    names: Set<EFBucketName>
  ): CompletableFuture<*> {
    return this.executeDatabase {
      this.database!!.openTransaction().use { transaction ->
        transaction.query(EFQBucketDeleteType::class.java).execute(names)
        transaction.commit()

        this.bucketsSelectedSource.set(setOf())
        this.bucketsSource.set(
          transaction.query(EFQBucketListType::class.java)
            .execute(DDatabaseUnit.UNIT)
        )
      }
    }
  }

  override fun bucketExists(
    name: EFBucketName
  ): Boolean {
    return this.buckets.get()
      .any { c -> c.name == name }
  }

  override fun bucketSelectionAdd(
    name: EFBucketName
  ): CompletableFuture<*> {
    return this.executeCommand {
      this.bucketsSelectedSource.set(this.bucketsSelectedSource.get().plus(name))
    }
  }

  override fun bucketSelectionRemove(
    name: EFBucketName
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
    name: EFBucketName
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
      this.database!!.openTransaction().use { transaction ->
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
      this.database!!.openTransaction().use { transaction ->
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
}
