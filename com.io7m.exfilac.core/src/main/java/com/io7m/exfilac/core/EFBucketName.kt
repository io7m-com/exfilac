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

import java.util.regex.Pattern

/**
 * See: https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html
 */

data class EFBucketName(
  val value: String
) {
  override fun toString(): String {
    return this.value
  }

  companion object {
    private val lowercaseNumbersDotsHyphens =
      Pattern.compile("[.0123456789abcdefghijklmnopqrstuvwxyz\\-]+")

    private val ipAddress =
      Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")

    private fun checkValidity(
      name: String
    ) {
      require(name.length in 3..63) {
        "Bucket names must be between 3 (min) and 63 (max) characters long."
      }
      require(lowercaseNumbersDotsHyphens.matcher(name).matches()) {
        "Bucket names can consist only of lowercase letters, numbers, dots (.), and hyphens (-)."
      }
      require(name.first().isLetter() or name.first().isDigit()) {
        "Bucket names must begin and end with a letter or number."
      }
      require(name.last().isLetter() or name.last().isDigit()) {
        "Bucket names must begin and end with a letter or number."
      }
      require(!name.contains("..")) {
        "Bucket names must not contain two adjacent periods."
      }
      require(!ipAddress.matcher(name).matches()) {
        "Bucket names must not be formatted as an IP address."
      }
      require(!name.startsWith("xn--")) {
        "Bucket names must not start with the prefix xn--."
      }
      require(!name.startsWith("sthree-")) {
        "Bucket names must not start with the prefix sthree-."
      }
      require(!name.startsWith("sthree-configurator")) {
        "Bucket names must not start with the prefix sthree-configurator."
      }
      require(!name.startsWith("amzn-s3-demo-")) {
        "Bucket names must not start with the prefix amzn-s3-demo-."
      }
      require(!name.endsWith("-s3alias")) {
        "Bucket names must not end with the suffix -s3alias."
      }
      require(!name.endsWith("--ol-s3")) {
        "Bucket names must not end with the suffix --ol-s3."
      }
      require(!name.endsWith(".mrap")) {
        "Bucket names must not end with the suffix .mrap."
      }
      require(!name.endsWith("--x-s3")) {
        "Bucket names must not end with the suffix --x-s3."
      }
    }
  }

  init {
    checkValidity(this.value)
  }
}
