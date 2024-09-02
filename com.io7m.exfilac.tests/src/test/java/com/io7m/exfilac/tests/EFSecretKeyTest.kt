package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFSecretKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class EFSecretKeyTest {

  @TestFactory
  fun testValid() : Stream<DynamicTest>
  {
    return Stream.of(
      "b73b7bf2f13a00d43908de5f6b4cb603ae292653de915d2cf91240394dee80ad",
    ).map { name -> validTestOf(name) }
  }

  private fun validTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testValid_$name") {
      assertEquals(name, EFSecretKey(name).value)
      assertEquals(name, EFSecretKey(name).toString())
    }
  }

  @TestFactory
  fun testInvalid() : Stream<DynamicTest>
  {
    return Stream.of(
      "",
      " ",
    ).map { name -> invalidTestOf(name) }
  }

  private fun invalidTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testInvalid_$name") {
      assertThrows<IllegalArgumentException> {
        EFSecretKey(name)
      }
    }
  }
}
