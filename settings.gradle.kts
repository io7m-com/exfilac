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

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("$rootDir/libraries.toml"))
    }
  }

  /*
   * The set of repositories used to resolve library dependencies. The order is significant!
   */

  repositories {
    mavenLocal()
    mavenCentral()
    google()

    /*
     * Allow access to the Sonatype snapshots repository.
     */

    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}

rootProject.name = "com.io7m.exfilac"

include(":com.io7m.exfilac.content_tree.api")
include(":com.io7m.exfilac.content_tree.device")
include(":com.io7m.exfilac.core")
include(":com.io7m.exfilac.main")
include(":com.io7m.exfilac.s3_uploader.amazon")
include(":com.io7m.exfilac.s3_uploader.api")
include(":com.io7m.exfilac.service.api")
include(":com.io7m.exfilac.tests")
include(":com.io7m.exfilac.tests.device")
