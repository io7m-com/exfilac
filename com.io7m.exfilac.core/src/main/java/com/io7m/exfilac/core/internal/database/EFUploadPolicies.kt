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

import com.io7m.exfilac.core.EFUploadPolicy
import com.io7m.exfilac.core.EFUploadSchedule
import com.io7m.exfilac.core.EFUploadTrigger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Properties

object EFUploadPolicies {

  @Throws(IOException::class)
  fun deserializeFromBytes(
    data: ByteArray
  ): EFUploadPolicy {
    val p = Properties()
    val stream = ByteArrayInputStream(data)
    p.loadFromXML(stream)
    return deserialize(p)
  }

  @Throws(IOException::class)
  fun deserialize(
    p: Properties
  ): EFUploadPolicy {
    return when (val t = p.getProperty("@type")) {
      "com.io7m.exfilac.upload_policy" -> {
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
  ): EFUploadPolicy {
    val triggers = mutableSetOf<EFUploadTrigger>()
    for (index in 0 until Integer.MAX_VALUE) {
      if (p.containsKey("trigger.$index")) {
        triggers.add(EFUploadTrigger.valueOf(p.getProperty("trigger.$index")))
      } else {
        break
      }
    }
    return EFUploadPolicy(
      EFUploadSchedule.valueOf(p.getProperty("schedule")),
      triggers.toSet()
    )
  }

  fun serialize(
    u: EFUploadPolicy
  ): Properties {
    val p = Properties()
    p.setProperty("@type", "com.io7m.exfilac.upload_policy")
    p.setProperty("@version", "1")
    p.setProperty("schedule", u.schedule.name)
    for ((index, trigger) in u.triggers.withIndex()) {
      p.setProperty("trigger.$index", trigger.name)
    }
    return p
  }

  @Throws(IOException::class)
  fun serializeToBytes(
    u: EFUploadPolicy
  ): ByteArray {
    val p = serialize(u)
    val out = ByteArrayOutputStream()
    p.storeToXML(out, "", "UTF-8")
    out.flush()
    return out.toByteArray()
  }
}
