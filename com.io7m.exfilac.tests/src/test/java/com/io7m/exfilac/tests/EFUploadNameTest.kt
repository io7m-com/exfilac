package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFUploadName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class EFUploadNameTest {

  @TestFactory
  fun testValid() : Stream<DynamicTest>
  {
    return Stream.of(
      "upload-1",
      "Upload2"
    ).map { name -> validTestOf(name) }
  }

  private fun validTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testValid_$name") {
      assertEquals(name, EFUploadName(name).value)
      assertEquals(name, EFUploadName(name).toString())
    }
  }

  @TestFactory
  fun testInvalid() : Stream<DynamicTest>
  {
    return Stream.of(
      "",
    ).map { name -> invalidTestOf(name) }
  }

  private fun invalidTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testInvalid_$name") {
      assertThrows<IllegalArgumentException> {
        EFUploadName(name)
      }
    }
  }
}
