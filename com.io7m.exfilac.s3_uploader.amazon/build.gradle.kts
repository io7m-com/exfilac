dependencies {
  implementation(project(":com.io7m.exfilac.clock.api"))
  implementation(project(":com.io7m.exfilac.s3_uploader.api"))
  implementation(project(":com.io7m.exfilac.service.api"))

  implementation(libs.apache.commons.io)
  implementation(libs.apache.commons.math3)
  implementation(libs.io7m.peixoto.sdk)
  implementation(libs.kotlin.stdlib)
  implementation(libs.slf4j)
  implementation(libs.stax)
  implementation(libs.stax.api)
}
