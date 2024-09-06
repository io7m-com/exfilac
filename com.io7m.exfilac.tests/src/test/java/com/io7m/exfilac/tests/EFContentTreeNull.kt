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

package com.io7m.exfilac.tests

import com.io7m.exfilac.content_tree.api.EFContentDirectoryType
import com.io7m.exfilac.content_tree.api.EFContentPath
import com.io7m.exfilac.content_tree.api.EFContentTreeFactoryType
import com.io7m.exfilac.content_tree.api.EFContentTreeNodeType
import java.net.URI
import java.time.OffsetDateTime

object EFContentTreeNull : EFContentTreeFactoryType {

  private class Node : EFContentDirectoryType {
    override val parent: EFContentDirectoryType?
      get() = null
    override val path: EFContentPath
      get() = EFContentPath(URI.create(""), listOf())
    override val lastModified: OffsetDateTime
      get() = OffsetDateTime.now()
    override val children: List<EFContentTreeNodeType>
      get() = listOf()
  }

  override fun create(
    source: URI
  ): EFContentTreeNodeType {
    return Node()
  }
}
