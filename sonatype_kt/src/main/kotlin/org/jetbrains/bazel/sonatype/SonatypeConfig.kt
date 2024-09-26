package org.jetbrains.bazel.sonatype

data class SonatypeConfig(
  val username: String,
  val password: String,
  val repositoryUrl: String,
  val profileName: String,
  val sessionName: String?,
  val coordinates: SonatypeCoordinates,
  val timeoutMillis: Int,
  val logLevel: String,
  val projectJar: String,
  val projectSourcesJar: String,
  val projectDocsJar: String,
  val projectPom: String
)
