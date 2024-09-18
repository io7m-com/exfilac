import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

fun calculateVersionCode(): Int {
  val now = LocalDateTime.now(ZoneId.of("UTC"))
  val nowSeconds = now.toEpochSecond(ZoneOffset.UTC)
  // Seconds since 2024-09-18T15:20:00 UTC
  return (nowSeconds - 1726672800).toInt()
}

fun getGitHash(): String {
  val proc = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()

  proc.waitFor(10L, TimeUnit.SECONDS)
  return proc.inputStream.bufferedReader().readText().trim()
}

val io7mKeyStore =
  File("$rootDir/io7m.keystore")
val io7mKeyAlias =
  project.findProperty("com.io7m.keyAlias") as String?
val io7mKeyPassword =
  project.findProperty("com.io7m.keyPassword") as String?
val io7mStorePassword =
  project.findProperty("com.io7m.storePassword") as String?

val requiredSigningTask = task("CheckReleaseSigningInformation") {
  if (io7mKeyAlias == null) {
    throw GradleException("com.io7m.keyAlias is not specified.")
  }
  if (io7mKeyPassword == null) {
    throw GradleException("com.io7m.keyPassword is not specified.")
  }
  if (io7mStorePassword == null) {
    throw GradleException("com.io7m.storePassword is not specified.")
  }
}

android {
  this.buildFeatures {
    this.buildConfig = true
  }

  this.defaultConfig {
    this.versionName = rootProject.ext["VERSION_NAME"].toString()
    this.versionCode = calculateVersionCode()
    this.buildConfigField("String", "EXFILAC_GIT_COMMIT", "\"${getGitHash()}\"")
    this.buildConfigField("String", "EXFILAC_VERSION", "\"${rootProject.ext["VERSION_NAME"]}\"")
  }

  /*
   * Ensure that release builds are signed.
   */

  this.signingConfigs {
    this.create("release") {
      this.keyAlias = io7mKeyAlias
      this.keyPassword = io7mKeyPassword
      this.storeFile = io7mKeyStore
      this.storePassword = io7mStorePassword
    }
  }

  /*
   * Ensure that the right NDK ABIs are declared.
   */

  this.buildTypes {
    this.debug {
      this.ndk {
        this.abiFilters.add("x86")
        this.abiFilters.add("x86_64")
        this.abiFilters.add("arm64-v8a")
        this.abiFilters.add("armeabi")
      }
      this.versionNameSuffix = "-debug"
    }
    this.release {
      this.ndk {
        this.abiFilters.add("x86")
        this.abiFilters.add("x86_64")
        this.abiFilters.add("arm64-v8a")
        this.abiFilters.add("armeabi")
      }
      this.signingConfig = this@android.signingConfigs.getByName("release")
    }
  }

  /*
   * Release builds need extra checking.
   */

  this.applicationVariants.all {
    if (this.buildType.name == "release") {
      val preBuildTask = tasks.findByName("preReleaseBuild")
      preBuildTask?.dependsOn?.add(requiredSigningTask)
    }
  }
}

/*
 * Produce an AAB file whenever someone asks for "assemble".
 */

afterEvaluate {
  this.tasks.findByName("assemble")
    ?.dependsOn?.add(this.tasks.findByName("bundle"))
}

dependencies {
  this.implementation(this.project(":com.io7m.exfilac.clock.api"))
  this.implementation(this.project(":com.io7m.exfilac.content_tree.api"))
  this.implementation(this.project(":com.io7m.exfilac.content_tree.device"))
  this.implementation(this.project(":com.io7m.exfilac.core"))
  this.implementation(this.project(":com.io7m.exfilac.s3_uploader.amazon"))
  this.implementation(this.project(":com.io7m.exfilac.s3_uploader.api"))
  this.implementation(this.project(":com.io7m.exfilac.service.api"))

  this.implementation(libs.io7m.anethum.api)
  this.implementation(libs.io7m.blackthorne.core)
  this.implementation(libs.io7m.blackthorne.jxe)
  this.implementation(libs.io7m.darco.api)
  this.implementation(libs.io7m.darco.sqlite)
  this.implementation(libs.io7m.jaffirm.core)
  this.implementation(libs.io7m.jattribute.core)
  this.implementation(libs.io7m.jlexing.core)
  this.implementation(libs.io7m.jmulticlose.core)
  this.implementation(libs.io7m.junreachable.core)
  this.implementation(libs.io7m.jxe.core)
  this.implementation(libs.io7m.lanark.core)
  this.implementation(libs.io7m.peixoto.sdk)
  this.implementation(libs.io7m.seltzer.api)
  this.implementation(libs.io7m.taskrecorder.core)
  this.implementation(libs.io7m.trasco.api)
  this.implementation(libs.io7m.trasco.vanilla)
  this.implementation(libs.io7m.trasco.xml.schemas)

  this.implementation(libs.apache.commons.io)
  this.implementation(libs.kotlin.stdlib)
  this.implementation(libs.logback.android)
  this.implementation(libs.opentelemetry.api)
  this.implementation(libs.opentelemetry.context)
  this.implementation(libs.slf4j)
  this.implementation(libs.stax)
  this.implementation(libs.stax.api)
  this.implementation(libs.xerces)
  this.implementation(libs.xerial.sqlite)

  // QR code reader.
  this.implementation(libs.zxing.embedded)
  this.implementation(libs.zxing.core)

  // Theme
  this.implementation(libs.google.material)

  this.implementation(libs.androidx.activity)
  this.implementation(libs.androidx.activity.ktx)
  this.implementation(libs.androidx.annotation)
  this.implementation(libs.androidx.appcompat)
  this.implementation(libs.androidx.appcompat.resources)
  this.implementation(libs.androidx.cardview)
  this.implementation(libs.androidx.collection)
  this.implementation(libs.androidx.constraintlayout)
  this.implementation(libs.androidx.constraintlayout.core)
  this.implementation(libs.androidx.constraintlayout.solver)
  this.implementation(libs.androidx.coordinatorlayout)
  this.implementation(libs.androidx.core)
  this.implementation(libs.androidx.core.common)
  this.implementation(libs.androidx.core.ktx)
  this.implementation(libs.androidx.core.runtime)
  this.implementation(libs.androidx.core.splashscreen)
  this.implementation(libs.androidx.cursoradapter)
  this.implementation(libs.androidx.customview)
  this.implementation(libs.androidx.customview.poolingcontainer)
  this.implementation(libs.androidx.documentfile)
  this.implementation(libs.androidx.drawerlayout)
  this.implementation(libs.androidx.emoji2)
  this.implementation(libs.androidx.emoji2.views)
  this.implementation(libs.androidx.emoji2.views.helper)
  this.implementation(libs.androidx.fragment)
  this.implementation(libs.androidx.fragment.ktx)
  this.implementation(libs.androidx.interpolator)
  this.implementation(libs.androidx.lifecycle.common)
  this.implementation(libs.androidx.lifecycle.extensions)
  this.implementation(libs.androidx.lifecycle.livedata)
  this.implementation(libs.androidx.lifecycle.livedata.core)
  this.implementation(libs.androidx.lifecycle.livedata.core.ktx)
  this.implementation(libs.androidx.lifecycle.livedata.ktx)
  this.implementation(libs.androidx.lifecycle.process)
  this.implementation(libs.androidx.lifecycle.runtime)
  this.implementation(libs.androidx.lifecycle.viewmodel)
  this.implementation(libs.androidx.lifecycle.viewmodel.ktx)
  this.implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  this.implementation(libs.androidx.loader)
  this.implementation(libs.androidx.multidex)
  this.implementation(libs.androidx.paging.common)
  this.implementation(libs.androidx.paging.common.ktx)
  this.implementation(libs.androidx.paging.runtime)
  this.implementation(libs.androidx.paging.runtime.ktx)
  this.implementation(libs.androidx.preference)
  this.implementation(libs.androidx.preference.ktx)
  this.implementation(libs.androidx.recyclerview)
  this.implementation(libs.androidx.savedstate)
  this.implementation(libs.androidx.startup.runtime)
  this.implementation(libs.androidx.tracing)
  this.implementation(libs.androidx.vectordrawable)
  this.implementation(libs.androidx.vectordrawable.animated)
  this.implementation(libs.androidx.viewpager)
  this.implementation(libs.androidx.viewpager2)

  this.implementation(libs.kotlinx.coroutines)
  this.implementation(libs.kotlinx.coroutines.android)
  this.implementation(libs.kotlinx.coroutines.core.jvm)
}
