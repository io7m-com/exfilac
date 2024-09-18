fun getGitHash(): String {
  val proc = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()

  proc.waitFor(10L, TimeUnit.SECONDS)
  return proc.inputStream.bufferedReader().readText().trim()
}

android {
  this.buildFeatures {
    this.buildConfig = true
  }

  this.defaultConfig {
    this.versionName = rootProject.ext["VERSION_NAME"].toString()
    this.versionCode = rootProject.ext["VERSION_CODE"].toInt()
    this.buildConfigField("String", "EXFILAC_GIT_COMMIT", "\"${getGitHash()}\"")
    this.buildConfigField("String", "EXFILAC_VERSION", "\"${rootProject.ext["VERSION_NAME"]}\"")
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
    }
  }
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
