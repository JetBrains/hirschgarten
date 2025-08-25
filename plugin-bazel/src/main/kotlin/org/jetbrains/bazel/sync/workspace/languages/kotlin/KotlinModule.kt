package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.java.JavaModule
import org.jetbrains.bazel.sync.workspace.model.LanguageData

data class KotlinModule(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
  val associates: List<Label>,
  val javaModule: JavaModule?,
) : LanguageData
