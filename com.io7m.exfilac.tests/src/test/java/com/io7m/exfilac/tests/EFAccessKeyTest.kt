package com.io7m.exfilac.tests

import com.io7m.exfilac.core.EFAccessKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class EFAccessKeyTest {

  @TestFactory
  fun testValid() : Stream<DynamicTest>
  {
    return Stream.of(
      "192c8394c16bf80d464fddc12a7cecf08adf89f62a9b68fc81dcea4d213f76c0",
    ).map { name -> validTestOf(name) }
  }

  private fun validTestOf(
    name: String
  ): DynamicTest {
    return DynamicTest.dynamicTest("testValid_$name") {
      assertEquals(name, EFAccessKey(name).value)
      assertNotEquals(name, EFAccessKey(name).toString())
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
        EFAccessKey(name)
      }
    }
  }
}
