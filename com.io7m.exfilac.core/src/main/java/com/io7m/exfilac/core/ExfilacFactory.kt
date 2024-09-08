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

package com.io7m.exfilac.core

import com.io7m.exfilac.clock.api.EFClockServiceType
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.core.internal.Exfilac
import com.io7m.exfilac.s3_uploader.api.EFS3UploaderFactoryType
import java.nio.file.Path

object ExfilacFactory : ExfilacFactoryType {
  override fun open(
    contentTrees: EFContentTreeFactoryType,
    s3Uploaders: EFS3UploaderFactoryType,
    clock: EFClockServiceType,
    dataDirectory: Path
  ): ExfilacType {
    return Exfilac.open(
      contentTrees = contentTrees,
      s3Uploaders = s3Uploaders,
      clock = clock,
      dataDirectory = dataDirectory
    )
  }
}
