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

import com.io7m.exfilac.core.internal.boot.EFBootContextType
import com.io7m.exfilac.core.internal.boot.EFBootServiceType
import com.io7m.jxe.core.JXEHardenedSAXParsers
import org.apache.xerces.jaxp.SAXParserFactoryImpl
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.Optional

class EFBootDatabase(
  val file: Path
) : EFBootServiceType<EFDatabaseType> {

  private val logger =
    LoggerFactory.getLogger(EFBootDatabase::class.java)

  override fun execute(
    context: EFBootContextType
  ): EFDatabaseType {
    return context.resources.add(
      EFDatabaseFactory()
        .open(
          EFDatabaseConfiguration(
            saxParsersOpt = Optional.of(
              JXEHardenedSAXParsers { SAXParserFactoryImpl() },
            ),
            filePath = file,
            concurrency = 1
          ),
        ) { message -> logger.debug("Database: {}", message) },
    )
  }

  override val description: String
    get() = "Database"

  override val serviceClass: Class<EFDatabaseType>
    get() = EFDatabaseType::class.java
}
