package org.jetbrains.bazel.sync.workspace.languages.scala

import org.jetbrains.bazel.sync.workspace.languages.java.JavaModule
import org.jetbrains.bazel.sync.workspace.model.LanguageData
import java.nio.file.Path

data class ScalaSdk(val version: String, val compilerJars: List<Path>)

data class ScalaModule(
  val sdk: ScalaSdk,
  val scalacOpts: List<String>,
  val javaModule: JavaModule?,
) : LanguageData
