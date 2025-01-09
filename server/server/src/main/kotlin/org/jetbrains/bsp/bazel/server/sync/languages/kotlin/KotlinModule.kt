package org.jetbrains.bsp.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.model.LanguageData
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaModule

data class KotlinModule(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  val javaModule: JavaModule?,
) : LanguageData
