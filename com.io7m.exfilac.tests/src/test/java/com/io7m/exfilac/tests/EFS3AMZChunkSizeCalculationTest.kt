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

import com.io7m.exfilac.s3_uploader.amazon.EFS3AMZChunkSizeCalculation
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.LongRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class EFS3AMZChunkSizeCalculationTest {

  private val logger =
    LoggerFactory.getLogger(EFS3AMZChunkSizeCalculationTest::class.java)

  private fun megabytes(count: Long): Long {
    return count * 1_000_000
  }

  /**
   * A 10mb file would be optimally divided into 1 10mb chunk.
   */

  @Test
  fun testChunkSize0() {
    val size = megabytes(10L)
    val r =
      EFS3AMZChunkSizeCalculation.calculate(
        size = size,
        minimumChunkSize = megabytes(10),
        maximumChunkCount = 100
      )
    logger.debug("{}", r)
    assertEquals(1, r.chunkCount)
    assertEquals(megabytes(10L), r.chunkSize)
  }

  /**
   * A 100mb file would be optimally divided into 10 10mb chunks.
   */

  @Test
  fun testChunkSize1() {
    val size = megabytes(100L)
    val r =
      EFS3AMZChunkSizeCalculation.calculate(
        size = size,
        minimumChunkSize = megabytes(10),
        maximumChunkCount = 100
      )
    logger.debug("{}", r)
    assertEquals(10, r.chunkCount)
    assertEquals(megabytes(10L), r.chunkSize)
  }

  /**
   * A 1000mb file would be optimally divided into 100 10mb chunks.
   */

  @Test
  fun testChunkSize2() {
    val size = megabytes(1000L)
    val r =
      EFS3AMZChunkSizeCalculation.calculate(
        size = size,
        minimumChunkSize = megabytes(10),
        maximumChunkCount = 100
      )
    logger.debug("{}", r)
    assertEquals(100, r.chunkCount)
    assertEquals(megabytes(10L), r.chunkSize)
  }

  /**
   * A 1000mb file would be optimally divided into 100 10mb chunks.
   */

  @Test
  fun testChunkSize3() {
    val size = megabytes(1000L)
    val r =
      EFS3AMZChunkSizeCalculation.calculate(
        size = size,
        minimumChunkSize = megabytes(10),
        maximumChunkCount = 100
      )
    logger.debug("{}", r)
    assertEquals(100, r.chunkCount)
    assertEquals(megabytes(10L), r.chunkSize)
  }

  /**
   * A 1000mb file would be optimally divided into 200 5mb chunks.
   */

  @Test
  fun testChunkSize4() {
    val size = megabytes(1000L)
    val r =
      EFS3AMZChunkSizeCalculation.calculate(
        size = size,
        minimumChunkSize = megabytes(5),
        maximumChunkCount = 200
      )
    logger.debug("{}", r)
    assertEquals(200, r.chunkCount)
    assertEquals(megabytes(5L), r.chunkSize)
  }

  @Property
  fun testChunkSizeObeyed(
    @ForAll @LongRange(min = 10_000_000L, max = 10_000_000_000L) size: Long,
    @ForAll @LongRange(min = 10_000_000L, max = 100_000_000L) minChunkSize: Long,
    @ForAll @LongRange(min = 1L, max = 1000L) maxChunkCount: Long
  ) {
    Assumptions.assumeTrue(minChunkSize <= size)

    val r = EFS3AMZChunkSizeCalculation.calculate(
      size = size,
      minimumChunkSize = minChunkSize,
      maximumChunkCount = maxChunkCount
    )

    logger.debug("{} {} {} -> {} {}", size, minChunkSize, maxChunkCount, r.chunkSize, r.chunkCount)

    assertTrue(
      r.chunkSize >= minChunkSize,
      "Chunk size ${r.chunkSize} must be >= $minChunkSize"
    )
    assertTrue(
      r.chunkCount <= maxChunkCount,
      "Chunk count ${r.chunkCount} must be <= $maxChunkCount"
    )
    assertTrue(
      (r.chunkCount * r.chunkSize) + r.chunkSizeLast == size,
      "(${r.chunkCount} * ${r.chunkSize}) + ${r.chunkSizeLast} == $size"
    )
  }
}
