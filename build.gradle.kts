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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

val gradleVersionRequired = "8.10"
val gradleVersionReceived = gradle.gradleVersion

if (gradleVersionRequired != gradleVersionReceived) {
  throw GradleException(
    "Gradle version $gradleVersionRequired is required to run this build. You are using Gradle $gradleVersionReceived",
  )
}

plugins {
  signing

  id("org.jetbrains.kotlin.jvm")
    .version("2.0.20")
    .apply(false)

  id("org.jetbrains.kotlin.android")
    .version("2.0.20")
    .apply(false)

  id("com.android.library")
    .version("8.5.0")
    .apply(false)

  id("com.android.application")
    .version("8.5.0")
    .apply(false)

  /*
   * Download plugin. Used to fetch artifacts such as Scando during the build.
   *
   * https://plugins.gradle.org/plugin/de.undercouch.download
   */

  id("de.undercouch.download")
    .version("5.6.0")
    .apply(false)

  id("maven-publish")
}

/*
 * The various paths used during the build.
 */

val io7mRootBuildDirectory =
  "$rootDir/build"
val io7mDeployDirectory =
  "$io7mRootBuildDirectory/maven"
val io7mScandoJarFile =
  "$rootDir/scando.jar"
val io7mKtlintJarFile =
  "$rootDir/ktlint.jar"

/**
 * Convenience functions to read strongly-typed values from property files.
 */

fun property(
  project: Project,
  name: String,
): String {
  return project.extra[name] as String
}

fun propertyOptional(project: Project, name: String): String? {
  val map = project.extra
  if (map.has(name)) {
    return map[name] as String?
  }
  return null
}

fun propertyInt(
  project: Project,
  name: String,
): Int {
  val text = property(project, name)
  return text.toInt()
}

fun propertyBoolean(
  project: Project,
  name: String,
): Boolean {
  val text = property(project, name)
  return text.toBooleanStrict()
}

fun propertyBooleanOptional(
  project: Project,
  name: String,
  defaultValue: Boolean,
): Boolean {
  val value = propertyOptional(project, name) ?: return defaultValue
  return value.toBooleanStrict()
}

/**
 * Configure Maven publishing. Artifacts are published to a local directory
 * so that they can be pushed to Maven Central in one step using brooklime.
 */

fun configurePublishingFor(project: Project) {
  val mavenCentralUsername =
    (project.findProperty("mavenCentralUsername") ?: "") as String
  val mavenCentralPassword =
    (project.findProperty("mavenCentralPassword") ?: "") as String

  val versionName =
    property(project, "VERSION_NAME")
  val packaging =
    property(project, "POM_PACKAGING")

  val publishSources =
    propertyBoolean(project, "com.io7m.build.publishSources")
  val enableSigning =
    propertyBooleanOptional(project, "com.io7m.build.enableSigning", true)

  /*
   * Create an empty JavaDoc jar. Required for Maven Central deployments.
   */

  val taskJavadocEmpty =
    project.task("JavadocEmptyJar", org.gradle.jvm.tasks.Jar::class) {
      this.archiveClassifier = "javadoc"
    }

  /*
   * Create a publication. Note that the name of the publication must be unique across all
   * modules, because the broken Gradle signing plugin will create a signing task for each
   * one that, in the case of a name conflict, will silently overwrite the previous signing
   * task.
   */

  project.publishing {
    publications {
      create<MavenPublication>("_${project.name}_MavenPublication") {
        groupId = property(project, "GROUP")
        artifactId = property(project, "POM_ARTIFACT_ID")
        version = versionName

        /*
         * https://central.sonatype.org/publish/requirements/#sufficient-metadata
         */

        pom {
          name.set(property(project, "POM_NAME"))
          description.set(property(project, "POM_DESCRIPTION"))
          url.set(property(project, "POM_URL"))

          scm {
            connection.set(property(project, "POM_SCM_CONNECTION"))
            developerConnection.set(property(project, "POM_SCM_DEV_CONNECTION"))
            url.set(property(project, "POM_SCM_URL"))
          }

          licenses {
            license {
              name.set(property(project, "POM_LICENCE_NAME"))
              url.set(property(project, "POM_LICENCE_URL"))
            }
          }

          developers {
            developer {
              name.set("The Palace Project")
              email.set("info@theio7mproject.org")
              organization.set("The Palace Project")
              organizationUrl.set("https://theio7mproject.org/")
            }
          }
        }

        artifact(taskJavadocEmpty)

        from(
          when (packaging) {
            "jar" -> {
              project.components["java"]
            }

            "aar" -> {
              project.components["release"]
            }

            "apk" -> {
              project.components["release"]
            }

            else -> {
              throw java.lang.IllegalArgumentException(
                "Cannot set up publishing for packaging type $packaging",
              )
            }
          },
        )
      }
    }

    repositories {
      maven {
        name = "Directory"
        url = uri(io7mDeployDirectory)
      }

      /*
       * Only deploy to the Sonatype snapshots repository if the current version is a
       * snapshot version.
       */

      if (versionName.endsWith("-SNAPSHOT")) {
        maven {
          name = "SonatypeCentralSnapshots"
          url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

          credentials {
            username = mavenCentralUsername
            password = mavenCentralPassword
          }
        }
      }
    }
  }

  /*
   * If source publications are disabled in the project properties, it seems that the only
   * way to stop the Android plugins from publishing sources is to manually "disable" the
   * publication tasks by deleting all of the actions within the tasks, and then specifying
   * a dependency on our own task that produces an empty jar file.
   */

  if (!publishSources) {
    logger.info("com.io7m.build.publishSources is false, so source jars are disabled.")

    val taskSourcesEmpty =
      project.task("SourcesEmptyJar", org.gradle.jvm.tasks.Jar::class) {
        this.archiveClassifier = "sources"
      }

    project.tasks.matching { task -> task.name.endsWith("SourcesJar") }
      .forEach { task ->
        task.actions.clear()
        task.dependsOn.add(taskSourcesEmpty)
      }
  }

  /*
   * Configure signing.
   */

  if (enableSigning) {
    signing {
      useGpgCmd()
      sign(project.publishing.publications)
    }
  }
}

/*
 * A task that cleans up the Maven deployment directory. The "clean" tasks of
 * each project are configured to depend upon this task. This prevents any
 * deployment of stale artifacts to remote repositories.
 */

val cleanTask = task("CleanMavenDeployDirectory", Delete::class) {
  this.delete.add(io7mDeployDirectory)
}

/**
 * A function to download and verify the Scando jar file.
 *
 * @return The verification task
 */

fun createScandoDownloadTask(project: Project): Task {
  val scandoVersion =
    "1.0.0"
  val scandoSHA256 =
    "08fba5fc4bc3b5a49d205a4c38356dc8c7e01f4963adb661b67f9d2ed23751ae"
  val scandoSource =
    "https://repo1.maven.org/maven2/com/io7m/scando/com.io7m.scando.cmdline/$scandoVersion/com.io7m.scando.cmdline-$scandoVersion-main.jar"

  val scandoMakeDirectory =
    project.task("ScandoMakeDirectory") {
      mkdir(io7mRootBuildDirectory)
    }

  val scandoDownload =
    project.task("ScandoDownload", Download::class) {
      src(scandoSource)
      dest(file(io7mScandoJarFile))
      overwrite(true)
      this.dependsOn.add(scandoMakeDirectory)
    }

  return project.task("ScandoDownloadVerify", Verify::class) {
    src(file(io7mScandoJarFile))
    checksum(scandoSHA256)
    algorithm("SHA-256")
    this.dependsOn(scandoDownload)
  }
}

/**
 * A task to execute Scando to analyze semantic versioning.
 */

fun createScandoAnalyzeTask(project: Project): Task {
  val group =
    property(project, "GROUP")
  val artifactId =
    property(project, "POM_ARTIFACT_ID")
  val versionCurrent =
    property(project, "VERSION_NAME")
  val versionPrevious =
    property(project, "VERSION_PREVIOUS")

  val oldGroup =
    group.replace('.', '/')
  val oldPath =
    "$oldGroup/$artifactId/$versionPrevious/$artifactId-$versionPrevious.aar"
  val oldUrl =
    "https://repo1.maven.org/maven2/$oldPath"

  val commandLineArguments: List<String> = arrayListOf(
    "java",
    "-jar",
    io7mScandoJarFile,
    "--excludeList",
    "${project.rootDir}/VERSIONING.txt",
    "--oldJarUri",
    oldUrl,
    "--oldJarVersion",
    versionPrevious,
    "--ignoreMissingOld",
    "--newJar",
    "${project.buildDir}/outputs/aar/$artifactId-debug.aar",
    "--newJarVersion",
    versionCurrent,
    "--textReport",
    "${project.buildDir}/scando-report.txt",
    "--htmlReport",
    "${project.buildDir}/scando-report.html",
  )

  return project.task("ScandoAnalyze", Exec::class) {
    commandLine = commandLineArguments
  }
}

/*
 * Create a task in the root project that downloads Scando.
 */

lateinit var scandoDownloadTask: Task

rootProject.afterEvaluate {
  apply(plugin = "de.undercouch.download")
  scandoDownloadTask = createScandoDownloadTask(this)
}

/**
 * A function to download and verify the ktlint jar file.
 *
 * @return The verification task
 */

fun createKtlintDownloadTask(project: Project): Task {
  val ktlintVersion =
    "0.50.0"
  val ktlintSHA256 =
    "c704fbc28305bb472511a1e98a7e0b014aa13378a571b716bbcf9d99d59a5092"
  val ktlintSource =
    "https://repo1.maven.org/maven2/com/pinterest/ktlint/$ktlintVersion/ktlint-$ktlintVersion-all.jar"

  val ktlintMakeDirectory =
    project.task("KtlintMakeDirectory") {
      mkdir(io7mRootBuildDirectory)
    }

  val ktlintDownload =
    project.task("KtlintDownload", Download::class) {
      src(ktlintSource)
      dest(file(io7mKtlintJarFile))
      overwrite(true)
      onlyIfModified(true)
      this.dependsOn.add(ktlintMakeDirectory)
    }

  return project.task("KtlintDownloadVerify", Verify::class) {
    src(file(io7mKtlintJarFile))
    checksum(ktlintSHA256)
    algorithm("SHA-256")
    this.dependsOn(ktlintDownload)
  }
}

/**
 * A task to execute ktlint to check sources.
 */

val ktlintPatterns: List<String> = arrayListOf(
  "*/src/**/*.kt",
  "*/build.gradle.kts",
  "build.gradle.kts",
  "!*/src/test/**",
)

fun createKtlintCheckTask(project: Project): Task {
  val commandLineArguments: ArrayList<String> = arrayListOf(
    "java",
    "-jar",
    io7mKtlintJarFile,
  )
  commandLineArguments.addAll(ktlintPatterns)

  return project.task("KtlintCheck", Exec::class) {
    commandLine = commandLineArguments
  }
}

/**
 * A task to execute ktlint to reformat sources.
 */

fun createKtlintFormatTask(project: Project): Task {
  val commandLineArguments: ArrayList<String> = arrayListOf(
    "java",
    "-jar",
    io7mKtlintJarFile,
    "-F",
  )
  commandLineArguments.addAll(ktlintPatterns)

  return project.task("KtlintFormat", Exec::class) {
    commandLine = commandLineArguments
  }
}

/*
 * Create a task in the root project that downloads ktlint.
 */

lateinit var ktlintDownloadTask: Task

rootProject.afterEvaluate {
  apply(plugin = "de.undercouch.download")
  ktlintDownloadTask = createKtlintDownloadTask(this)

  val enableKtlintChecks =
    propertyBoolean(this, "com.io7m.build.enableKtLint")

  if (enableKtlintChecks) {
    val checkActual = createKtlintCheckTask(this)
    checkActual.dependsOn.add(ktlintDownloadTask)
    cleanTask.dependsOn.add(checkActual)
  }

  /*
   * Create a task that can be used to reformat sources. This is purely for manual execution
   * from the command-line, and is not executed otherwise.
   */

  val formatTask = createKtlintFormatTask(this)
  formatTask.dependsOn.add(ktlintDownloadTask)
}

allprojects {

  /*
   * Configure the project metadata.
   */

  this.group =
    property(this, "GROUP")
  this.version =
    property(this, "VERSION_NAME")

  val jdkBuild =
    propertyInt(this, "com.io7m.build.jdkBuild")
  val jdkBytecodeTarget =
    propertyInt(this, "com.io7m.build.jdkBytecodeTarget")

  /*
   * Configure builds and tests for various project types.
   */

  when (extra["POM_PACKAGING"]) {
    "pom" -> {
      logger.info("Configuring ${this.project} $version as a pom project")
    }

    "apk" -> {
      logger.info("Configuring ${this.project} $version as an apk project")

      apply(plugin = "com.android.application")
      apply(plugin = "org.jetbrains.kotlin.android")

      /*
       * Configure the JVM toolchain version that we want to use for Kotlin.
       */

      val kotlin: KotlinAndroidProjectExtension =
        this.extensions["kotlin"] as KotlinAndroidProjectExtension
      val java: JavaPluginExtension =
        this.extensions["java"] as JavaPluginExtension

      kotlin.jvmToolchain(jdkBuild)
      java.toolchain.languageVersion.set(JavaLanguageVersion.of(jdkBuild))

      /*
       * Configure the various required Android properties.
       */

      val android: ApplicationExtension =
        this.extensions["android"] as ApplicationExtension

      android.namespace =
        property(this, "POM_ARTIFACT_ID")
      android.compileSdk =
        propertyInt(this, "com.io7m.build.androidSDKCompile")

      android.defaultConfig {
        multiDexEnabled = true
        targetSdk =
          propertyInt(this@allprojects, "com.io7m.build.androidSDKTarget")
        minSdk =
          propertyInt(this@allprojects, "com.io7m.build.androidSDKMinimum")
      }

      /*
       * Produce JDK bytecode of the correct version.
       */

      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = jdkBytecodeTarget.toString()
      }
      java.sourceCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
      java.targetCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)

      android.compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
        targetCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
      }
    }

    "aar" -> {
      logger.info("Configuring ${this.project} $version as an aar project")

      apply(plugin = "com.android.library")
      apply(plugin = "org.jetbrains.kotlin.android")

      /*
       * Configure the JVM toolchain version that we want to use for Kotlin.
       */

      val kotlin: KotlinAndroidProjectExtension =
        this.extensions["kotlin"] as KotlinAndroidProjectExtension
      val java: JavaPluginExtension =
        this.extensions["java"] as JavaPluginExtension

      kotlin.jvmToolchain(jdkBuild)
      java.toolchain.languageVersion.set(JavaLanguageVersion.of(jdkBuild))

      /*
       * Configure the various required Android properties.
       */

      val android: LibraryExtension =
        this.extensions["android"] as LibraryExtension

      android.namespace =
        property(this, "POM_ARTIFACT_ID")
      android.compileSdk =
        propertyInt(this, "com.io7m.build.androidSDKCompile")

      android.defaultConfig {
        multiDexEnabled = true
        minSdk = propertyInt(this@allprojects, "com.io7m.build.androidSDKMinimum")
      }

      /*
       * Produce JDK bytecode of the correct version.
       */

      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = jdkBytecodeTarget.toString()
      }
      java.sourceCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
      java.targetCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)

      android.compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
        targetCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
      }

      /*
       * Configure semantic versioning analysis.
       */

      afterEvaluate {
        val enableSemanticVersionChecks =
          propertyBoolean(this, "com.io7m.build.checkSemanticVersioning")

        if (enableSemanticVersionChecks) {
          val verifyActual = createScandoAnalyzeTask(this)
          verifyActual.dependsOn.add(scandoDownloadTask)
          verifyActual.dependsOn.add("assembleDebug")

          val verifyTask = project.task("verifySemanticVersioning")
          verifyTask.dependsOn.add(verifyActual)
        } else {
          // Create a do-nothing task to keep interface compatibility.
          project.task("verifySemanticVersioning")
        }
      }
    }

    "jar" -> {
      logger.info("Configuring ${this.project} $version as a jar project")

      apply(plugin = "java-library")
      apply(plugin = "org.jetbrains.kotlin.jvm")

      /*
       * Configure the JVM toolchain versions that we want to use for Kotlin and Java.
       */

      val kotlin: KotlinProjectExtension =
        this.extensions["kotlin"] as KotlinProjectExtension
      val java: JavaPluginExtension =
        this.extensions["java"] as JavaPluginExtension

      kotlin.jvmToolchain(jdkBuild)
      java.toolchain.languageVersion.set(JavaLanguageVersion.of(jdkBuild))

      /*
       * Produce JDK bytecode of the correct version.
       */

      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = jdkBytecodeTarget.toString()
      }
      java.sourceCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)
      java.targetCompatibility = JavaVersion.toVersion(jdkBytecodeTarget)

      /*
       * Configure JUnit tests.
       */

      tasks.named<Test>("test") {
        useJUnitPlatform()

        // Required for the Mockito ByteBuddy agent on modern VMs.
        systemProperty("jdk.attach.allowAttachSelf", "true")

        testLogging {
          events("passed")
        }

        this.reports.html.required = true
        this.reports.junitXml.required = true
      }
    }
  }

  /*
   * Configure publishing.
   */

  when (extra["POM_PACKAGING"]) {
    "jar", "aar" -> {
      apply(plugin = "maven-publish")

      afterEvaluate {
        configurePublishingFor(this.project)
      }
    }
  }

  /*
   * Configure some aggressive version resolution behaviour. The listed configurations have
   * transitive dependency resolution enabled; all other configurations do not. This forces
   * projects to be extremely explicit about what is imported.
   */

  val transitiveConfigurations = setOf(
    "androidTestDebugImplementation",
    "androidTestDebugImplementationDependenciesMetadata",
    "androidTestImplementation",
    "androidTestImplementationDependenciesMetadata",
    "androidTestReleaseImplementation",
    "androidTestReleaseImplementationDependenciesMetadata",
    "annotationProcessor",
    "debugAndroidTestCompilationImplementation",
    "debugAndroidTestImplementation",
    "debugAndroidTestImplementationDependenciesMetadata",
    "debugAnnotationProcessor",
    "debugAnnotationProcessorClasspath",
    "debugUnitTestCompilationImplementation",
    "debugUnitTestImplementation",
    "debugUnitTestImplementationDependenciesMetadata",
    "kotlinBuildToolsApiClasspath",
    "kotlinCompilerClasspath",
    "kotlinCompilerPluginClasspath",
    "kotlinCompilerPluginClasspathDebug",
    "kotlinCompilerPluginClasspathDebugAndroidTest",
    "kotlinCompilerPluginClasspathDebugUnitTest",
    "kotlinCompilerPluginClasspathMain",
    "kotlinCompilerPluginClasspathRelease",
    "kotlinCompilerPluginClasspathReleaseUnitTest",
    "kotlinCompilerPluginClasspathTest",
    "kotlinKlibCommonizerClasspath",
    "kotlinNativeCompilerPluginClasspath",
    "kotlinScriptDef",
    "kotlinScriptDefExtensions",
    "mainSourceElements",
    "releaseAnnotationProcessor",
    "releaseAnnotationProcessorClasspath",
    "releaseUnitTestCompilationImplementation",
    "releaseUnitTestImplementation",
    "releaseUnitTestImplementationDependenciesMetadata",
    "testDebugImplementation",
    "testDebugImplementationDependenciesMetadata",
    "testFixturesDebugImplementation",
    "testFixturesDebugImplementationDependenciesMetadata",
    "testFixturesImplementation",
    "testFixturesImplementationDependenciesMetadata",
    "testFixturesReleaseImplementation",
    "testFixturesReleaseImplementationDependenciesMetadata",
    "testImplementation",
    "testImplementationDependenciesMetadata",
    "testReleaseImplementation",
    "testReleaseImplementationDependenciesMetadata",
  )

  /*
   * Write the set of available configurations to files, for debugging purposes. Plugins can
   * add new configurations at any time, and so it's nice to have a list of the available
   * configurations visible.
   */

  val configurationsActual = mutableSetOf<String>()
  afterEvaluate {
    configurations.all {
      configurationsActual.add(this.name)
    }
    // File("configurations.txt").writeText(configurationsActual.joinToString("\n"))
  }

  afterEvaluate {
    configurations.all {
      isTransitive = transitiveConfigurations.contains(name)
      // resolutionStrategy.failOnVersionConflict()
    }
  }

  /*
   * Configure all "clean" tasks to depend upon the global Maven deployment directory cleaning
   * task.
   */

  afterEvaluate {
    tasks.matching { task -> task.name == "clean" }
      .forEach { task -> task.dependsOn(cleanTask) }
  }

  /*
   * Configure all "test" tasks to be disabled. The tests are enabled only in those modules
   * that specifically ask for them. Why do this? Because the Android plugins do lots of
   * expensive per-module configuration for tests that don't exist.
   */

  afterEvaluate {
    tasks.matching { task -> task.name.contains("UnitTest") }
      .forEach { task -> task.enabled = false }
  }
}
