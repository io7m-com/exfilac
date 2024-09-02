package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFBucketName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class EFBucketNameTest {

  @TestFactory
  fun testValid() : Stream<DynamicTest>
  {
    return Stream.of(
      "bucket",
      "bucket3"
    ).map { name -> validTestOf(name) }
  }

  private fun validTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testValid_$name") {
      assertEquals(name, EFBucketName(name).value)
      assertEquals(name, EFBucketName(name).toString())
    }
  }

  @TestFactory
  fun testInvalid() : Stream<DynamicTest>
  {
    return Stream.of(
      "",
      "a",
      "aa",
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "@@@",
      "_aaa",
      "aaa_",
      "aa..aa",
      "127.0.0.1",
      "xn--aaa",
      "sthree--aaa",
      "sthree-configuratoraaa",
      "amzn-s3-demo-aaa",
      "aaa-s3alias",
      "aaa--ol-s3",
      "aaa.mrap",
      "aa--x-s3"
    ).map { name -> invalidTestOf(name) }
  }

  private fun invalidTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testInvalid_$name") {
      assertThrows<IllegalArgumentException> {
        EFBucketName(name)
      }
    }
  }
}
