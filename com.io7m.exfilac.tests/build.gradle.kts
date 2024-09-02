dependencies {
  testImplementation(project(":com.io7m.exfilac.core"))

  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.junit.platform.commons)
  testImplementation(libs.junit.platform.engine)
  testImplementation(libs.junit.platform.launcher)
  testImplementation(libs.opentest4j)
  testImplementation(libs.logback.core)
  testImplementation(libs.logback.classic)

  testImplementation(libs.io7m.anethum.api)
  testImplementation(libs.io7m.blackthorne.core)
  testImplementation(libs.io7m.blackthorne.jxe)
  testImplementation(libs.io7m.darco.api)
  testImplementation(libs.io7m.darco.sqlite)
  testImplementation(libs.io7m.jaffirm.core)
  testImplementation(libs.io7m.jattribute.core)
  testImplementation(libs.io7m.jlexing.core)
  testImplementation(libs.io7m.jmulticlose.core)
  testImplementation(libs.io7m.junreachable.core)
  testImplementation(libs.io7m.jxe.core)
  testImplementation(libs.io7m.lanark.core)
  testImplementation(libs.io7m.seltzer.api)
  testImplementation(libs.io7m.taskrecorder.core)
  testImplementation(libs.io7m.trasco.api)
  testImplementation(libs.io7m.trasco.vanilla)
  testImplementation(libs.io7m.trasco.xml.schemas)

  testImplementation(libs.kotlin.stdlib)
  testImplementation(libs.logback.android)
  testImplementation(libs.opentelemetry.api)
  testImplementation(libs.opentelemetry.context)
  testImplementation(libs.slf4j)
  testImplementation(libs.xerces)
  testImplementation(libs.xerial.sqlite)
}

afterEvaluate {
  tasks.matching { task -> task.name.contains("UnitTest") }
    .forEach { task -> task.enabled = true }
}
