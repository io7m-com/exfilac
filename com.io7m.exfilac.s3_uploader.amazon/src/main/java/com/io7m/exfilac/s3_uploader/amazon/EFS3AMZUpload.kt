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

import com.io7m.exfilac.clock.api.EFClockServiceType
import com.io7m.exfilac.s3_uploader.api.EFS3TransferStatistics
import com.io7m.exfilac.s3_uploader.api.EFS3UploadRequest
import com.io7m.exfilac.s3_uploader.api.EFS3UploadType
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.peixoto.sdk.org.apache.commons.codec.binary.Base64
import com.io7m.peixoto.sdk.software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import com.io7m.peixoto.sdk.software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import com.io7m.peixoto.sdk.software.amazon.awssdk.awscore.retry.AwsRetryStrategy
import com.io7m.peixoto.sdk.software.amazon.awssdk.core.sync.RequestBody
import com.io7m.peixoto.sdk.software.amazon.awssdk.http.SdkHttpClient
import com.io7m.peixoto.sdk.software.amazon.awssdk.http.apache.ApacheHttpClient
import com.io7m.peixoto.sdk.software.amazon.awssdk.regions.Region
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.S3Client
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.CompletedPart
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.HeadObjectRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.NoSuchKeyException
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.PutObjectRequest
import com.io7m.peixoto.sdk.software.amazon.awssdk.services.s3.model.UploadPartRequest
import org.apache.commons.io.input.BoundedInputStream
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class EFS3AMZUpload(
  private val upload: EFS3UploadRequest,
  private val clock: EFClockServiceType,
) : EFS3UploadType {

  private val logger =
    LoggerFactory.getLogger(EFS3AMZUpload::class.java)

  private val exfilacSHA256Header =
    "exfilac-sha256"
  private val multipartThreshold =
    16_777_216L
  private val minimumChunkSize =
    8_388_608L

  private val resources =
    CloseableCollection.create()

  private lateinit var executor: ExecutorService
  private lateinit var httpClient: SdkHttpClient
  private lateinit var s3client: S3Client

  private val done =
    AtomicBoolean()
  private val streamSupervised: AtomicReference<BoundedInputStream> =
    AtomicReference()

  @Volatile
  private var octetsThen = 0L

  override fun execute() {
    this.executor =
      Executors.newSingleThreadExecutor { r ->
        val thread = Thread(r)
        thread.name = "com.io7m.exfilac.s3.upload_supervisor[${thread.id}]"
        thread.priority = Thread.MIN_PRIORITY
        thread
      }

    this.resources.add(AutoCloseable { this.executor.shutdown() })

    try {
      this.executor.execute(this::executeStreamSupervisor)

      val credentials =
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(this.upload.accessKey, this.upload.secretKey)
        )

      this.httpClient =
        this.resources.add(
          ApacheHttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(60L))
            .socketTimeout(Duration.ofSeconds(60L))
            .build()
        )

      val clientBuilder = S3Client.builder()
      clientBuilder.credentialsProvider(credentials)
      clientBuilder.httpClient(this.httpClient)
      clientBuilder.region(Region.of(this.upload.region))
      clientBuilder.region(Region.of(this.upload.region))
      clientBuilder.forcePathStyle(this.upload.pathStyle)
      clientBuilder.endpointOverride(this.upload.endpoint)

      val strategy =
        AwsRetryStrategy.standardRetryStrategy()
          .toBuilder()
          .maxAttempts(5)
          .build()

      clientBuilder.overrideConfiguration { o -> o.retryStrategy(strategy) }

      return clientBuilder.build().use { c ->
        this.s3client = this.resources.add(c)
        if (this.upload.size >= this.multipartThreshold) {
          this.executeUploadMultiPart(c)
        } else {
          this.executeUploadSimple(c)
        }
      }
    } catch (e: Throwable) {
      this.upload.onError(e)
      throw e
    } finally {
      this.done.set(true)
      this.executor.shutdown()
      this.executor.awaitTermination(5L, TimeUnit.SECONDS)
    }
  }

  private fun executeUploadSimple(
    client: S3Client
  ) {
    BoundedInputStream.builder()
      .setInputStream(this.upload.streams.invoke())
      .get()
      .use { inputStream ->
        this.streamSupervised.set(inputStream)

        /*
         * Start an upload thread and a supervisor thread. The supervisor thread observes
         * data passing through the input stream and uses it to determine transfer speeds.
         */

        this.executePutObject(inputStream, client)
      }
  }

  /*
   * The stream supervisor function. This runs on a dedicated thread and periodically examines
   * the current stream to see how much data is being transferred.
   */

  private fun executeStreamSupervisor() {
    while (!this.done.get()) {
      val stream = this.streamSupervised.get()
      if (stream != null) {
        try {
          val octetsTransferredNow = stream.count
          val octetsInPeriod = octetsTransferredNow - this.octetsThen
          this.octetsThen += octetsInPeriod
          this.upload.onStatistics.invoke(
            EFS3TransferStatistics(
              time = this.clock.now(),
              octetsTransferred = this.octetsThen,
              octetsExpected = this.upload.size,
              octetsThisPeriod = octetsInPeriod
            )
          )
        } catch (e: Throwable) {
          this.logger.debug("Uncaught statistics subscriber exception: ", e)
        }
      }

      try {
        Thread.sleep(100L)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }
  }

  private fun executePutObject(
    inputStream: BoundedInputStream,
    client: S3Client
  ) {
    this.upload.onInformativeEvent("Calculating local content hash.")
    val contentSHA256 = this.sha256()
    this.upload.onInformativeEvent("Local content hash: $contentSHA256")

    if (!this.isUploadNecessary(client, contentSHA256)) {
      return
    }

    val metadata =
      mapOf(Pair(this.exfilacSHA256Header, this.sha256()))

    val put =
      PutObjectRequest.builder()
        .bucket(this.upload.bucket)
        .checksumSHA256(contentSHA256)
        .contentLength(this.upload.size)
        .contentType(this.upload.contentType)
        .metadata(metadata)
        .key(this.upload.path)
        .build()

    this.upload.onInformativeEvent("Uploading file.")
    val body = RequestBody.fromInputStream(inputStream, this.upload.size)
    client.putObject(put, body)

    if (this.isUploadNecessary(client, contentSHA256)) {
      throw IOException("After uploading, the size or hash does not match!")
    }

    this.upload.onInformativeEvent("Uploading completed.")
    this.upload.onFileSuccessfullyUploaded()
  }

  private fun isUploadNecessary(
    client: S3Client,
    contentSHA256: String
  ): Boolean {
    try {
      this.upload.onInformativeEvent("Fetching remote content hash.")
      val head =
        HeadObjectRequest.builder()
          .bucket(this.upload.bucket)
          .key(this.upload.path)
          .build()

      val response = client.headObject(head)
      val remoteSize = response.contentLength()
      val remoteHash = response.metadata()[this.exfilacSHA256Header]
      this.upload.onInformativeEvent("Remote content hash: $remoteHash")
      if (remoteHash == contentSHA256 && this.upload.size == remoteSize) {
        this.upload.onInformativeEvent("Hashes and size match, no upload is required.")
        this.upload.onFileSkipped()
        return false
      }
    } catch (e: NoSuchKeyException) {
      this.upload.onInformativeEvent("Remote file does not exist. Upload is required.")
    }
    return true
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

  private data class Part(
    val partNumber: Int,
    val offset: Long,
    val size: Long
  )

  private fun executeUploadMultiPart(
    client: S3Client
  ) {
    this.upload.onInformativeEvent("Calculating local content hash.")
    val contentSHA256 = this.sha256()
    this.upload.onInformativeEvent("Local content hash: $contentSHA256")

    if (!this.isUploadNecessary(client, contentSHA256)) {
      return
    }

    val chunks =
      EFS3AMZChunkSizeCalculation.calculate(
        size = this.upload.size,
        minimumChunkSize = this.minimumChunkSize,
        maximumChunkCount = 900
      )

    this.upload.onInformativeEvent("Uploading as ${chunks.size} chunks.")

    val metadata =
      mapOf(Pair(this.exfilacSHA256Header, this.sha256()))

    val upload =
      CreateMultipartUploadRequest.builder()
        .bucket(this.upload.bucket)
        .contentType(this.upload.contentType)
        .key(this.upload.path)
        .metadata(metadata)
        .build()

    this.upload.onInformativeEvent("Requesting multi-part upload…")
    val uploadResponse = client.createMultipartUpload(upload)

    try {
      val completedParts =
        mutableMapOf<Int, CompletedPart>()

      val parts = this.createParts(chunks)
      for (part in parts.values) {
        this.upload.onInformativeEvent("Uploading part ${part.partNumber} (Size ${part.size})…")
        this.upload.streams.invoke()
          .use { stream ->
            stream.skip(part.offset)
            BoundedInputStream.builder()
              .setInputStream(stream)
              .setMaxCount(part.size)
              .get()
              .use { boundedStream ->
                this.streamSupervised.set(boundedStream)

                val uploadPartResponse =
                  client.uploadPart(
                    UploadPartRequest.builder()
                      .bucket(this.upload.bucket)
                      .contentLength(part.size)
                      .key(this.upload.path)
                      .partNumber(part.partNumber)
                      .uploadId(uploadResponse.uploadId())
                      .build(),
                    RequestBody.fromInputStream(
                      boundedStream,
                      part.size
                    )
                  )

                completedParts[part.partNumber] =
                  CompletedPart.builder()
                    .partNumber(part.partNumber)
                    .eTag(uploadPartResponse.eTag())
                    .build()
              }
          }
      }

      this.upload.onInformativeEvent("Completing multi-part upload…")
      val completedUpload =
        CompletedMultipartUpload.builder()
          .parts(completedParts.values.sortedBy { p -> p.partNumber() })
          .build()

      client.completeMultipartUpload(
        CompleteMultipartUploadRequest.builder()
          .multipartUpload(completedUpload)
          .uploadId(uploadResponse.uploadId())
          .bucket(this.upload.bucket)
          .key(this.upload.path)
          .build()
      )

      if (this.isUploadNecessary(client, contentSHA256)) {
        throw IOException("After uploading, the size or hash does not match!")
      }

      this.upload.onInformativeEvent("Uploading completed.")
      this.upload.onFileSuccessfullyUploaded()
    } catch (e: Throwable) {
      client.abortMultipartUpload(
        AbortMultipartUploadRequest.builder()
          .bucket(this.upload.bucket)
          .key(this.upload.path)
          .uploadId(uploadResponse.uploadId())
          .build()
      )
      throw e
    }
  }

  private fun createParts(
    chunks: List<EFS3AMZChunk>
  ): MutableMap<Int, Part> {
    val parts = mutableMapOf<Int, Part>()
    for (chunk in chunks) {
      parts[chunk.partNumber] = Part(
        partNumber = chunk.partNumber,
        offset = chunk.chunkOffset,
        size = chunk.chunkSize
      )
    }
    return parts
  }

  override fun close() {
    this.done.set(true)

    try {
      this.resources.close()
    } catch (e: Throwable) {
      this.logger.debug("Failed to close S3 client: ", e)
    }
  }
}
