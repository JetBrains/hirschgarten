package org.jetbrains.bsp.protocol


data class CppModule(
  val copts: List<String>,
  val sources: List<String>,
  val headers: List<String>,

  val textualHeaders: List<String>,
  val transitiveIncludeDirectory: List<String>,
  val transitiveQuoteIncludeDirectory: List<String>,
  val transitiveDefine: List<String>,
  val transitiveSystemIncludeDirectory: List<String>,
  val includePrefix: String,
  val stripIncludePrefix: String,
  val cToolchainInfo: CToolchainInfo?,
  val execRoot : String?,
)

data class CToolchainInfo(
  val builtInIncludeDirectory: List<String>,
  val cOptions: List<String>,
  val cppOptions: List<String>,
  val cCompiler: String,
  val cppCompiler:String,
  val compilerVersion: String,

)
