package org.jetbrains.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import java.nio.file.Path

data class KotlinModule(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  val javaModule: JavaModule?,
  val outputJar: List<Path>
) : LanguageData
