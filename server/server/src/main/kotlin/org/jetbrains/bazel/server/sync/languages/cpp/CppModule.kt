package org.jetbrains.bazel.server.sync.languages.cpp

import org.jetbrains.bazel.server.model.LanguageData

data class CppModule(
  val copts: List<String>,
  val defines: List<String>,
  val linkOpts: List<String>,
  val linkShared: Boolean,
) : LanguageData
