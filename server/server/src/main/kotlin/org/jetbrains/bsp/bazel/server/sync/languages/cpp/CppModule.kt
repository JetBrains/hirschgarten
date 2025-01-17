package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import org.jetbrains.bsp.bazel.server.model.LanguageData
import java.net.URI

data class CppModule(
  val copts: List<String>,
  val headers: List<URI>,
  val textualHeaders: List<URI>,
  val transitiveIncludeDirectory: List<URI>,
  val transitiveQuoteIncludeDirectory: List<URI>,
  val transitiveDefine: List<String>,
  val transitiveSystemIncludeDirectory: List<URI>,
  val includePrefix: String,
  val stripIncludePrefix: String,
  val cToolchainInfo: CToolchainInfo?,
  val execRoot: String?,
) : LanguageData

data class CToolchainInfo(
  val builtInIncludeDirectories: List<URI>,
  val cOptions: List<String>,
  val cppOptions: List<String>,
  // Usually C Compiler and CPP Compiler are the same in a bazel toolchain
  // and bazel will use -x c++ / -lstdc++ to compiler cpp with a C compiler
  // therefore usually there should always be one identical compiler version.
  // If there are indeed two different compilers, the cpp compiler version has
  // a higher priority
  val cCompiler: URI,
  val cppCompiler: URI,
  val compilerVersion: String?,
)
