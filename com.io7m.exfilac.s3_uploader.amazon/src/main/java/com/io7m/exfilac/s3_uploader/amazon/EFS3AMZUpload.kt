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

package com.io7m.exfilac.s3_uploader.amazon

import com.io7m.exfilac.s3_uploader.api.EFS3TransferStatistics
import com.io7m.exfilac.s3_uploader.api.EFS3UploadRequest
import com.io7m.exfilac.s3_uploader.api.EFS3UploadType
import com.io7m.peixoto.sdk.org.apache.commons.codec.binary.Base64
import com.io7m.peixoto.sdk.software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import com.io7m.peixoto.sdk.software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import com.io7m.peixoto.sdk.software.amazon.awssdk.core.sync.RequestBody
import com.io7m.peixoto.sdk.software.amazon.awssdk.http.apache.ApacheHttpClient
import com.io7m.peixoto.sdk.software.amazon.awssdk.regions.Region
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.S3Client
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.ChecksumMode
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.HeadObjectRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.NoSuchKeyException
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.PutObjectRequest
import org.apache.commons.io.input.BoundedInputStream
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EFS3AMZUpload(
  private val upload: EFS3UploadRequest
) : EFS3UploadType {

  private val logger =
    LoggerFactory.getLogger(EFS3AMZUpload::class.java)

  @Volatile
  private var octetsTransferred = 0L

  override fun execute() {
    val credentials =
      StaticCredentialsProvider.create(
        AwsBasicCredentials.create(this.upload.accessKey, this.upload.secretKey)
      )

    val clientBuilder = S3Client.builder()
    clientBuilder.credentialsProvider(credentials)
    clientBuilder.httpClient(ApacheHttpClient.create())
    clientBuilder.region(Region.of(this.upload.region))
    clientBuilder.region(Region.of(this.upload.region))
    clientBuilder.forcePathStyle(this.upload.pathStyle)
    clientBuilder.endpointOverride(this.upload.endpoint)
    val client = clientBuilder.build()

    return client.use { c ->
      if (this.upload.size >= 10_000_000L) {
        this.executeUploadMultiPart(c)
      } else {
        this.executeUploadSimple(c)
      }
    }
  }

  private fun executeUploadSimple(
    client: S3Client
  ) {
    BoundedInputStream.builder()
      .setInputStream(this.upload.streams.invoke())
      .get()
      .use { inputStream ->
        val executor =
          Executors.newCachedThreadPool { r ->
            val thread = Thread(r)
            thread.name = "com.io7m.exfilac.s3.upload[${thread.id}]"
            thread.priority = Thread.MIN_PRIORITY
            thread
          }

        /*
         * Start an upload thread and a supervisor thread. The supervisor thread observes
         * data passing through the input stream and uses it to determine transfer speeds.
         */

        try {
          val future = CompletableFuture<Unit>()
          executor.execute {
            this.executeIOThreadSuperviseStream(inputStream, future)
          }
          executor.execute {
            try {
              future.complete(this.executeIOThreadPutObject(inputStream, client))
            } catch (e: Throwable) {
              this.upload.onError(e)
              future.completeExceptionally(e)
            }
          }
          future.join()
        } finally {
          executor.shutdown()
          executor.awaitTermination(5L, TimeUnit.SECONDS)
        }
      }
  }

  private fun executeIOThreadSuperviseStream(
    inputStream: BoundedInputStream,
    future: CompletableFuture<*>
  ) {
    while (true) {
      if (future.isDone || future.isCancelled || future.isCompletedExceptionally) {
        return
      }

      try {
        val thisPeriod = inputStream.count
        this.octetsTransferred += thisPeriod
        this.upload.onStatistics.invoke(
          EFS3TransferStatistics(
            time = OffsetDateTime.now(),
            octetsTransferred = this.octetsTransferred,
            octetsExpected = this.upload.size,
            octetsThisPeriod = thisPeriod
          )
        )
      } catch (e: Throwable) {
        this.logger.debug("Uncaught statistics subscriber exception: ", e)
      }

      try {
        Thread.sleep(250L)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }
  }

  private fun executeIOThreadPutObject(
    inputStream: BoundedInputStream,
    client: S3Client
  ) {
    this.upload.onInformativeEvent("Calculating local content hash.")
    val contentSHA256 = this.sha256()
    this.upload.onInformativeEvent("Local content hash: $contentSHA256")

    try {
      this.upload.onInformativeEvent("Fetching remote content hash.")
      val head =
        HeadObjectRequest.builder()
          .bucket(this.upload.bucket)
          .key(this.upload.path)
          .checksumMode(ChecksumMode.ENABLED)
          .build()

      val r = client.headObject(head)
      this.upload.onInformativeEvent("Remote content hash: ${r.checksumSHA256()}")
      if (r.checksumSHA256() == contentSHA256) {
        this.upload.onInformativeEvent("Hashes match, no upload is required.")
        this.upload.onFileSkipped()
        return
      }
    } catch (e: NoSuchKeyException) {
      this.upload.onInformativeEvent("Remote file does not exist. Upload is required.")
    }

    val put =
      PutObjectRequest.builder()
        .bucket(this.upload.bucket)
        .checksumSHA256(contentSHA256)
        .contentLength(this.upload.size)
        .contentType(this.upload.contentType)
        .key(this.upload.path)
        .build()

    this.upload.onInformativeEvent("Uploading file.")
    val body = RequestBody.fromInputStream(inputStream, this.upload.size)
    client.putObject(put, body)
    this.upload.onInformativeEvent("Uploading completed.")
    this.upload.onFileSuccessfullyUploaded()
  }

  private fun sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    this.upload.streams.invoke()
      .use { stream ->
        val buffer = ByteArray(65536)
        while (true) {
          val r = stream.read(buffer)
          if (r == -1) {
            break
          }
          digest.update(buffer, 0, r)
        }
      }

    return Base64.encodeBase64String(digest.digest())
  }

  private fun executeUploadMultiPart(
    client: S3Client
  ) {
    throw IllegalStateException("Not implemented.")
  }

  override fun close() {
    // Nothing
  }
}
