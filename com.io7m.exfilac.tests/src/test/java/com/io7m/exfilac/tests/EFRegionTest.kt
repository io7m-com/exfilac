package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class EFRegionTest {

  @TestFactory
  fun testValid() : Stream<DynamicTest>
  {
    return Stream.of(
      "us-gov-east-1",
      "us-gov-west-1",
      "af-south-1",
      "ap-east-1",
      "ap-south-2",
      "ap-southeast-3",
      "ap-southeast-5",
      "ap-southeast-4",
      "ap-south-1",
      "ap-northeast-3",
      "ap-northeast-2",
      "ap-southeast-1",
      "ap-southeast-2",
      "ap-northeast-1",
      "ca-central-1",
      "ca-west-1",
      "eu-central-1",
      "eu-west-1",
      "eu-west-2",
      "eu-south-1",
      "eu-west-3",
      "eu-south-2",
      "eu-north-1",
      "eu-central-2",
      "il-central-1",
      "me-south-1",
      "me-central-1",
      "sa-east-1",
      "us-east-1",
      "us-east-2",
      "us-west-1",
      "us-west-2",
    ).map { name -> validTestOf(name) }
  }

  private fun validTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testValid_$name") {
      assertEquals(name, EFRegion(name).value)
      assertEquals(name, EFRegion(name).toString())
    }
  }

  @TestFactory
  fun testInvalid() : Stream<DynamicTest>
  {
    return Stream.of(
      "",
      "1",
      "A",
      "_",
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    ).map { name -> invalidTestOf(name) }
  }

  private fun invalidTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testInvalid_$name") {
      assertThrows<IllegalArgumentException> {
        EFRegion(name)
      }
    }
  }
}
