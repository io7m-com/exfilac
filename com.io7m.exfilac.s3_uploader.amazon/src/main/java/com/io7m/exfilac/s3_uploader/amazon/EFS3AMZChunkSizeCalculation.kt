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

package com.io7m.exfilac.s3_uploader.amazon

object EFS3AMZChunkSizeCalculation {

  fun calculate(
    size: Long,
    minimumChunkSize: Long,
    maximumChunkCount: Long
  ): List<EFS3AMZChunk> {
    require(maximumChunkCount >= 1) {
      "Maximum chunk count $maximumChunkCount must be >= 1"
    }
    require(minimumChunkSize <= size) {
      "Minimum chunk size $minimumChunkSize must be <= size $size"
    }

    val chunkSize =
      Math.max(size / maximumChunkCount, minimumChunkSize)
    val chunkCount =
      size / chunkSize

    val chunks = mutableListOf<EFS3AMZChunk>()
    var offset = 0L
    var index = 1

    while (index <= chunkCount) {
      chunks.add(
        EFS3AMZChunk(
          partNumber = index,
          chunkSize = chunkSize,
          chunkOffset = offset
        )
      )
      offset += chunkSize
      ++index
    }

    if (offset < size) {
      chunks.add(
        EFS3AMZChunk(
          partNumber = index,
          chunkSize = size - offset,
          chunkOffset = offset
        )
      )
    }

    val sizeSum = chunks.sumOf { c -> c.chunkSize }
    check(sizeSum == size) {
      "Sum of chunks $sizeSum must equal original size $size"
    }
    return chunks.toList()
  }
}
