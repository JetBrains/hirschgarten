package org.jetbrains.bazel.server.sync.languages.kotlin

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bazel.server.sync.languages.java.JavaModule

data class KotlinModule(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<CanonicalLabel>,
  val javaModule: JavaModule?,
) : LanguageData
