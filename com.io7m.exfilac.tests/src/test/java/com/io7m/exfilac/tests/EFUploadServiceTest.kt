package com.io7m.exfilac.tests

import com.io7m.ervilla.api.EContainerSupervisorType
import com.io7m.ervilla.test_extension.ErvillaCloseAfterClass
import com.io7m.ervilla.test_extension.ErvillaConfiguration
import com.io7m.ervilla.test_extension.ErvillaExtension
import com.io7m.exfilac.content_tree.api.EFContentDirectoryType
import com.io7m.exfilac.content_tree.api.EFContentFileType
import com.io7m.exfilac.content_tree.api.EFContentPath
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.content_tree.api.EFContentTreeNodeType
import com.io7m.exfilac.core.EFAccessKey
import com.io7m.exfilac.core.EFBucketAccessStyle
import com.io7m.exfilac.core.EFBucketConfiguration
import com.io7m.exfilac.core.EFBucketName
import com.io7m.exfilac.core.EFBucketReferenceName
import com.io7m.exfilac.core.EFDeviceSource
import com.io7m.exfilac.core.EFRegion
import com.io7m.exfilac.core.EFSecretKey
import com.io7m.exfilac.core.EFUploadConfiguration
import com.io7m.exfilac.core.EFUploadName
import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadReason
import com.io7m.exfilac.core.EFUploadReasonManual
import com.io7m.exfilac.core.EFUploadResult
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.EFUploadStatusChanged
import com.io7m.exfilac.core.internal.EFUploadEventRecord
import com.io7m.exfilac.core.internal.EFUploadRecord
import com.io7m.exfilac.core.internal.database.EFDatabaseConfiguration
import com.io7m.exfilac.core.internal.database.EFDatabaseFactory
import com.io7m.exfilac.core.internal.database.EFDatabaseType
import com.io7m.exfilac.core.internal.database.EFQBucketPutType
import com.io7m.exfilac.core.internal.database.EFQUploadConfigurationPutType
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordListParameters
import com.io7m.exfilac.core.internal.database.EFQUploadEventRecordListType
import com.io7m.exfilac.core.internal.database.EFQUploadRecordListParameters
import com.io7m.exfilac.core.internal.database.EFQUploadRecordListType
import com.io7m.exfilac.core.internal.uploads.EFUploadService
import com.io7m.exfilac.s3_uploader.amazon.EFS3AMZUploaders
import com.io7m.exfilac.s3_uploader.api.EFS3UploaderType
import com.io7m.jattribute.core.AttributeType
import com.io7m.jattribute.core.Attributes
import org.apache.commons.io.input.BoundedInputStream
import org.apache.commons.io.input.InfiniteCircularInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

@ExtendWith(ErvillaExtension::class)
@ErvillaConfiguration(
  projectName = "com.io7m.exfilac",
  podmanExecutable = "podman",
  disabledIfUnsupported = true
)
class EFUploadServiceTest {

  private val logger =
    LoggerFactory.getLogger(EFUploadServiceTest::class.java)

  private val BUCKET_0 =
    EFBucketConfiguration(
      referenceName = EFBucketReferenceName("bucket"),
      name = EFBucketName("bucket0"),
      region = EFRegion("us-east-1"),
      accessKey = EFAccessKey("9aa1e66547ca16b7"),
      secret = EFSecretKey("d4c73cd8dfe5532966f26cad3e397652394d8030"),
      accessStyle = EFBucketAccessStyle.PATH_STYLE,
      endpoint = URI.create("http://localhost:9000")
    )

  private val BUCKET_1 =
    EFBucketConfiguration(
      referenceName = EFBucketReferenceName("bucket"),
      name = EFBucketName("bucket1"),
      region = EFRegion("us-east-1"),
      accessKey = EFAccessKey("9aa1e66547ca16b7"),
      secret = EFSecretKey("d4c73cd8dfe5532966f26cad3e397652394d8030"),
      accessStyle = EFBucketAccessStyle.PATH_STYLE,
      endpoint = URI.create("http://localhost:9000")
    )

  private val BUCKET_2 =
    EFBucketConfiguration(
      referenceName = EFBucketReferenceName("bucket"),
      name = EFBucketName("bucket2"),
      region = EFRegion("us-east-1"),
      accessKey = EFAccessKey("9aa1e66547ca16b7"),
      secret = EFSecretKey("d4c73cd8dfe5532966f26cad3e397652394d8030"),
      accessStyle = EFBucketAccessStyle.PATH_STYLE,
      endpoint = URI.create("http://localhost:9000")
    )

  private val BUCKET_3 =
    EFBucketConfiguration(
      referenceName = EFBucketReferenceName("bucket"),
      name = EFBucketName("bucket3"),
      region = EFRegion("us-east-1"),
      accessKey = EFAccessKey("9aa1e66547ca16b7"),
      secret = EFSecretKey("d4c73cd8dfe5532966f26cad3e397652394d8030"),
      accessStyle = EFBucketAccessStyle.PATH_STYLE,
      endpoint = URI.create("http://localhost:9000")
    )

  private val UPLOAD_0 =
    EFUploadConfiguration(
      name = EFUploadName("upload-0"),
      source = EFDeviceSource(URI.create("content://example")),
      bucket = this.BUCKET_0.referenceName,
      policy = EFUploadPolicy(
        schedule = EFUploadSchedule.EVERY_HOUR,
        triggers = setOf()
      )
    )

  private val UPLOAD_1 =
    EFUploadConfiguration(
      name = EFUploadName("upload-1"),
      source = EFDeviceSource(URI.create("content://example")),
      bucket = this.BUCKET_1.referenceName,
      policy = EFUploadPolicy(
        schedule = EFUploadSchedule.EVERY_HOUR,
        triggers = setOf()
      )
    )

  private val UPLOAD_2 =
    EFUploadConfiguration(
      name = EFUploadName("upload-2"),
      source = EFDeviceSource(URI.create("content://example")),
      bucket = this.BUCKET_2.referenceName,
      policy = EFUploadPolicy(
        schedule = EFUploadSchedule.EVERY_HOUR,
        triggers = setOf()
      )
    )

  private val UPLOAD_3 =
    EFUploadConfiguration(
      name = EFUploadName("upload-3"),
      source = EFDeviceSource(URI.create("content://example")),
      bucket = this.BUCKET_3.referenceName,
      policy = EFUploadPolicy(
        schedule = EFUploadSchedule.EVERY_HOUR,
        triggers = setOf()
      )
    )

  private val EMPTY_CONTENT_ROOT =
    ContentTreeDirectory(
      OffsetDateTime.now(),
      EFContentPath(
        root = this.UPLOAD_0.source.value,
        path = listOf("Example")
      )
    )

  private lateinit var attributes: Attributes
  private lateinit var contentTrees: ContentTreesStaged
  private lateinit var database: EFDatabaseType
  private lateinit var databaseFile: Path
  private lateinit var directory: Path
  private lateinit var status: AttributeType<EFUploadStatusChanged>
  private lateinit var uploader: EFS3UploaderType
  private lateinit var uploads: EFUploadService

  companion object {
    private lateinit var minio: EFMinIOFixture

    @BeforeAll
    @JvmStatic
    fun setupAll(
      @ErvillaCloseAfterClass supervisor: EContainerSupervisorType
    ) {
      this.minio = EFFixtures.minio(supervisor)
    }
  }

  @BeforeEach
  fun setup(
    @TempDir directory: Path
  ) {
    minio.reset()

    this.directory = directory
    this.databaseFile = directory.resolve("exfilac.db")
    this.database =
      EFDatabaseFactory()
        .open(
          EFDatabaseConfiguration(
            Optional.empty(),
            this.databaseFile
          )
        ) {

        }

    this.attributes =
      Attributes.create { e -> this.logger.error("Uncaught exception: ", e) }
    this.status =
      this.attributes.withValue(EFUploadStatusChanged())
    this.contentTrees =
      ContentTreesStaged()
    this.uploader =
      EFS3AMZUploaders()
        .create()

    this.uploads =
      EFUploadService(
        database = this.database,
        statusChangedSource = this.status,
        contentTrees = this.contentTrees,
        uploader = this.uploader,
        clock = EFClockMock
      )
  }

  class ContentTreeFileBig(
    override val lastModified: OffsetDateTime,
    override val path: EFContentPath,
    override val contentURI: URI
  ) : EFContentFileType {
    override val parent: EFContentDirectoryType?
      get() = null
    override val size: Long
      get() = (3 * 8_388_608L) + (8_388_608L / 8)

    override fun read(): InputStream {
      return BoundedInputStream.builder()
        .setInputStream(InfiniteCircularInputStream("HELLO!".toByteArray()))
        .setMaxCount(this.size)
        .get()
    }
  }

  class ContentTreeFile(
    override val lastModified: OffsetDateTime,
    override val path: EFContentPath,
    override val contentURI: URI
  ) : EFContentFileType {
    override val parent: EFContentDirectoryType?
      get() = null
    override val size: Long
      get() = 6L

    override fun read(): InputStream {
      return ByteArrayInputStream("Hello.".toByteArray())
    }
  }

  class ContentTreeDirectory(
    override val lastModified: OffsetDateTime,
    override val path: EFContentPath
  ) : EFContentDirectoryType {
    val childrenField: MutableList<EFContentTreeNodeType> =
      mutableListOf()
    override val parent: EFContentDirectoryType?
      get() = null
    override val children: List<EFContentTreeNodeType>
      get() = this.childrenField
  }

  class ContentTreesStaged : EFContentTreeFactoryType {
    var next: EFContentTreeNodeType? = null

    override fun create(
      source: URI
    ): EFContentTreeNodeType {
      return this.next!!
    }
  }

  /**
   * If the configuration doesn't exist, the upload fails.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testUploadConfigurationDoesNotExist() {
    val ex = assertThrows<Exception> {
      this.uploads.upload(EFUploadName("nonexistent"), EFUploadReasonManual)
        .get()
    }
    assertTrue(ex.message!!.contains("No such upload configuration"))
  }

  /**
   * If the content resolver returns no files, then nothing is uploaded and the task trivially
   * succeeds.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testNoFiles() {
    this.contentTrees.next = this.EMPTY_CONTENT_ROOT
    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(this.BUCKET_0)
      t.query(EFQUploadConfigurationPutType::class.java).execute(this.UPLOAD_0)
      t.commit()
    }

    this.uploads.upload(this.UPLOAD_0.name, EFUploadReasonManual).get()

    val records = mutableListOf<EFUploadRecord>()
    val events = mutableListOf<EFUploadEventRecord>()
    this.fetchUploadRecordsAndEvents(records, 0, events)

    assertEquals("Upload was triggered manually.", records.get(0).reason)
    assertEquals(0, records.get(0).filesRequired)
    assertEquals(0, records.get(0).filesFailed)
    assertEquals(0, records.get(0).filesSkipped)
    assertEquals(0, records.get(0).filesUploaded)
    assertEquals(EFUploadResult.SUCCEEDED, records.get(0).result)
    assertEquals(mutableListOf<EFUploadEventRecord>(), events)
  }

  /**
   * If the content resolver returns a file, but the remote rejects it, the task ultimately fails.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOneFileRejected() {
    minio.createBucket(BUCKET_0.name)

    val directory =
      ContentTreeDirectory(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example"))
      )
    val file =
      ContentTreeFile(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example", "File.txt")),
        URI.create("content://xyx/File.txt")
      )
    directory.childrenField.add(file)
    this.contentTrees.next = directory

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java)
        .execute(
          this.BUCKET_0.copy(
            accessKey = EFAccessKey("invalidinvalid"),
            secret = EFSecretKey("invalidinvalid")
          )
        )
      t.query(EFQUploadConfigurationPutType::class.java).execute(this.UPLOAD_0)
      t.commit()
    }

    this.uploads.upload(this.UPLOAD_0.name, EFUploadReasonManual).get()

    val records = mutableListOf<EFUploadRecord>()
    val events = mutableListOf<EFUploadEventRecord>()
    this.fetchUploadRecordsAndEvents(records, 0, events)

    assertEquals("Upload was triggered manually.", records.get(0).reason)
    assertEquals(1, records.get(0).filesRequired)
    assertEquals(1, records.get(0).filesFailed)
    assertEquals(0, records.get(0).filesSkipped)
    assertEquals(0, records.get(0).filesUploaded)
    assertEquals(EFUploadResult.FAILED, records.get(0).result)

    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Calculating local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Fetching remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Forbidden"))
    }
  }

  /**
   * If the content resolver returns a file, and the remote accepts it, the task ultimately succeeds.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOneFileAccepted() {
    minio.createUser(
      "someone",
      "12345678",
      this.BUCKET_1.accessKey,
      this.BUCKET_1.secret
    )
    minio.createBucket(BUCKET_1.name)

    val directory =
      ContentTreeDirectory(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example"))
      )
    val file =
      ContentTreeFile(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example", "File.txt")),
        URI.create("content://xyx/File.txt")
      )
    directory.childrenField.add(file)
    this.contentTrees.next = directory

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(this.BUCKET_1)
      t.query(EFQUploadConfigurationPutType::class.java).execute(this.UPLOAD_1)
      t.commit()
    }

    this.uploads.upload(this.UPLOAD_1.name, EFUploadReasonManual).get()

    val records = mutableListOf<EFUploadRecord>()
    val events = mutableListOf<EFUploadEventRecord>()
    this.fetchUploadRecordsAndEvents(records, 0, events)

    assertEquals("Upload was triggered manually.", records.get(0).reason)
    assertEquals(1, records.get(0).filesRequired)
    assertEquals(0, records.get(0).filesFailed)
    assertEquals(0, records.get(0).filesSkipped)
    assertEquals(1, records.get(0).filesUploaded)
    assertEquals(EFUploadResult.SUCCEEDED, records.get(0).result)

    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Calculating local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Fetching remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Remote file does not exist"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Uploading file"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Uploading completed"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("File was successfully uploaded"))
    }
  }

  /**
   * If remote already contains a given file, it is not uploaded twice.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOneFileRedundant() {
    minio.createUser(
      "someone",
      "12345678",
      this.BUCKET_2.accessKey,
      this.BUCKET_2.secret
    )
    minio.createBucket(BUCKET_2.name)

    val directory =
      ContentTreeDirectory(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example"))
      )
    val file =
      ContentTreeFile(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example", "File.txt")),
        URI.create("content://xyx/File.txt")
      )
    directory.childrenField.add(file)
    this.contentTrees.next = directory

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(this.BUCKET_2)
      t.query(EFQUploadConfigurationPutType::class.java).execute(this.UPLOAD_2)
      t.commit()
    }

    this.uploads.upload(this.UPLOAD_2.name, EFUploadReasonManual).get()
    this.uploads.upload(this.UPLOAD_2.name, EFUploadReasonManual).get()

    val records = mutableListOf<EFUploadRecord>()
    val events = mutableListOf<EFUploadEventRecord>()
    this.fetchUploadRecordsAndEvents(records, 1, events)

    assertEquals("Upload was triggered manually.", records.get(0).reason)
    assertEquals(1, records.get(1).filesRequired)
    assertEquals(0, records.get(1).filesFailed)
    assertEquals(1, records.get(1).filesSkipped)
    assertEquals(0, records.get(1).filesUploaded)
    assertEquals(EFUploadResult.SUCCEEDED, records.get(1).result)

    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Calculating local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Fetching remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Hashes match, no upload is required"))
    }
  }

  /**
   * If remote already contains a given file, it is not uploaded twice.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testOneFileRedundantMultiPart() {
    minio.createUser(
      "someone",
      "12345678",
      this.BUCKET_3.accessKey,
      this.BUCKET_3.secret
    )
    minio.createBucket(BUCKET_3.name)

    val directory =
      ContentTreeDirectory(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example"))
      )
    val file =
      ContentTreeFileBig(
        OffsetDateTime.now(),
        EFContentPath(URI.create("content://xyx"), listOf("Example", "File.txt")),
        URI.create("content://xyx/File.txt")
      )
    directory.childrenField.add(file)
    this.contentTrees.next = directory

    this.database.openTransaction().use { t ->
      t.query(EFQBucketPutType::class.java).execute(this.BUCKET_3)
      t.query(EFQUploadConfigurationPutType::class.java).execute(this.UPLOAD_3)
      t.commit()
    }

    this.uploads.upload(this.UPLOAD_3.name, EFUploadReasonManual).get()
    this.uploads.upload(this.UPLOAD_3.name, EFUploadReasonManual).get()

    val records = mutableListOf<EFUploadRecord>()
    val events = mutableListOf<EFUploadEventRecord>()
    this.fetchUploadRecordsAndEvents(records, 1, events)

    assertEquals("Upload was triggered manually.", records.get(0).reason)
    assertEquals(1, records.get(1).filesRequired)
    assertEquals(0, records.get(1).filesFailed)
    assertEquals(1, records.get(1).filesSkipped)
    assertEquals(0, records.get(1).filesUploaded)
    assertEquals(EFUploadResult.SUCCEEDED, records.get(1).result)

    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Calculating local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Local content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Fetching remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Remote content hash"))
    }
    run {
      val e = events.removeAt(0)
      logger.debug("{}", e)
      assertTrue(e.message.startsWith("Hashes match, no upload is required"))
    }
  }

  private fun fetchUploadRecordsAndEvents(
    records: MutableList<EFUploadRecord>,
    recordForEvents: Int,
    events: MutableList<EFUploadEventRecord>
  ) {
    this.database.openTransaction().use { t ->
      records.addAll(
        t.query(EFQUploadRecordListType::class.java)
          .execute(
            EFQUploadRecordListParameters(
              EFClockMock.now().minusYears(1),
              1000
            )
          )
      )
      events.addAll(
        t.query(EFQUploadEventRecordListType::class.java)
          .execute(
            EFQUploadEventRecordListParameters(
              records.get(recordForEvents).id,
              EFClockMock.now().minusYears(1),
              1000
            )
          )
      )
    }
  }
}
