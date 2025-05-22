package org.jetbrains.bazel.server.sync.languages.scala

import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import java.nio.file.Path

data class ScalaSdk(
  val version: String,
  val compilerJars: List<Path>,
)

data class ScalaModule(
  val sdk: ScalaSdk,
  val scalacOpts: List<String>,
  val javaModule: JavaModule?,
) : LanguageData
