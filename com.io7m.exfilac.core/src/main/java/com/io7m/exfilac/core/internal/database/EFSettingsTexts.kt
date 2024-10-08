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

package com.io7m.exfilac.core.internal.database

import com.io7m.exfilac.core.EFSettings
import com.io7m.exfilac.core.EFSettingsNetworking
import com.io7m.exfilac.core.EFSettingsNotifications
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Properties

object EFSettingsTexts {

  @Throws(IOException::class)
  fun deserializeFromBytes(
    data: ByteArray
  ): EFSettings {
    val p = Properties()
    val stream = ByteArrayInputStream(data)
    p.loadFromXML(stream)
    return deserialize(p)
  }

  @Throws(IOException::class)
  fun deserialize(
    p: Properties
  ): EFSettings {
    return when (val t = p.getProperty("@type")) {
      "com.io7m.exfilac.settings" -> {
        when (val v = p.getProperty("@version")) {
          "1" -> {
            deserialize1(p)
          }

          else -> {
            throw IOException("Unrecognized property serialization version: $v")
          }
        }
      }

      else -> {
        throw IOException("Unrecognized property serialization type: $t")
      }
    }
  }

  private fun deserialize1(
    p: Properties
  ): EFSettings {
    val uploadOnWifi =
      p.getProperty("networking.uploadOnWifi")?.toBoolean() ?: true
    val uploadOnCellular =
      p.getProperty("networking.uploadOnCellular")?.toBoolean() ?: true
    val paused =
      p.getProperty("paused")?.toBoolean() ?: false
    val hasSeenNotificationsNagScreen =
      p.getProperty("notifications.hasSeenNotificationsNagScreen")?.toBoolean() ?: false

    return EFSettings(
      networking = EFSettingsNetworking(
        uploadOnWifi = uploadOnWifi,
        uploadOnCellular = uploadOnCellular,
      ),
      paused = paused,
      notifications = EFSettingsNotifications(
        hasSeenNotificationsNagScreen = hasSeenNotificationsNagScreen
      )
    )
  }

  fun serialize(
    u: EFSettings
  ): Properties {
    val p = Properties()
    p.setProperty("@type", "com.io7m.exfilac.settings")
    p.setProperty("@version", "1")
    p.setProperty(
      "networking.uploadOnWifi",
      u.networking.uploadOnWifi.toString()
    )
    p.setProperty(
      "networking.uploadOnCellular",
      u.networking.uploadOnCellular.toString()
    )
    p.setProperty(
      "notifications.hasSeenNotificationsNagScreen",
      u.notifications.hasSeenNotificationsNagScreen.toString()
    )
    p.setProperty("paused", u.paused.toString())
    return p
  }

  @Throws(IOException::class)
  fun serializeToBytes(
    u: EFSettings
  ): ByteArray {
    val p = serialize(u)
    val out = ByteArrayOutputStream()
    p.storeToXML(out, "", "UTF-8")
    out.flush()
    return out.toByteArray()
  }
}
