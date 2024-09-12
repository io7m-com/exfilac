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

import com.io7m.exfilac.clock.api.EFClockServiceType
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadReason
import com.io7m.exfilac.core.EFUploadStatus
import com.io7m.exfilac.core.EFUploadStatusCancelling
import com.io7m.exfilac.core.EFUploadStatusChanged
import com.io7m.exfilac.core.EFUploadStatusNone
import com.io7m.exfilac.core.internal.EFUploadRecord
import com.io7m.exfilac.core.internal.database.EFDatabaseType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordMostRecentType
import com.io7m.exfilac.s3_uploader.api.EFS3UploaderType
import com.io7m.jattribute.core.AttributeReadableType
import com.io7m.jattribute.core.AttributeType
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.optionals.getOrNull

class EFUploadService(
  val database: EFDatabaseType,
  val statusChangedSource: AttributeType<EFUploadStatusChanged>,
  val contentTrees: EFContentTreeFactoryType,
  val uploader: EFS3UploaderType,
  val clock: EFClockServiceType,
) : EFUploadServiceType {

  private val logger =
    LoggerFactory.getLogger(EFUploadService::class.java)

  private val closed = AtomicBoolean()
  private val executor: ExecutorService =
    Executors.newCachedThreadPool { r ->
      val thread = Thread(r)
      thread.priority = Thread.MIN_PRIORITY
      thread.name = "com.io7m.exfilac.upload[${thread.id}]"
      thread
    }

  private val taskLock =
    ReentrantLock()
  private val tasks =
    mutableMapOf<EFUploadName, EFUploadTask>()
  private val taskStatuses =
    ConcurrentHashMap<EFUploadName, EFUploadStatus>()

  override val statusChanged: AttributeReadableType<EFUploadStatusChanged>
    get() = this.statusChangedSource

  override fun upload(
    name: EFUploadName,
    reason: EFUploadReason
  ): CompletableFuture<*> {
    val future = CompletableFuture<Unit>()

    this.taskLock.withLock {
      val existing = this.tasks.get(name)
      if (existing != null) {
        future.complete(Unit)
        return future
      }

      val task = EFUploadTask(
        database = this.database,
        statusChangedSource = this.statusChangedSource,
        onStatusChanged = { status -> this.taskStatuses[status.name] = status },
        contentTrees = this.contentTrees,
        s3Uploader = this.uploader,
        reason = reason,
        name = name,
        clock = this.clock
      )

      this.tasks.put(name, task)
      this.executor.execute {
        try {
          future.complete(task.execute())
        } catch (e: Throwable) {
          this.logger.debug("Upload failure: ", e)
          future.completeExceptionally(e)
        } finally {
          this.taskLock.withLock {
            this.tasks.remove(name)
          }
        }
      }
    }

    return future
  }

  override fun cancel(name: EFUploadName) {
    this.taskStatuses[name] = EFUploadStatusCancelling(name, null)
    this.taskLock.withLock { this.tasks.get(name) }?.cancel()
  }

  override fun status(
    name: EFUploadName
  ): EFUploadStatus {
    return this.taskLock.withLock { this.taskStatuses.get(name) }
      ?: EFUploadStatusNone(name, null)
  }

  override fun mostRecent(
    name: EFUploadName
  ): EFUploadRecord? {
    return this.database.openTransaction().use { t ->
      t.query(EFQUploadRecordMostRecentType::class.java)
        .execute(name)
        .getOrNull()
    }
  }

  override fun description(): String {
    return "Upload service."
  }

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.taskLock.withLock {
        for (e in this.tasks) {
          e.value.cancel()
        }
      }
      this.executor.shutdown()
    }
  }
}
