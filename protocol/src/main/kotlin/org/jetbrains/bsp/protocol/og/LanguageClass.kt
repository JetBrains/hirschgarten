package org.jetbrains.bsp.protocol.og

/** Language classes.  */
enum class LanguageClass(
  val languageName: String,
  private val recognizedFilenameExtensions: Set<String>
)  {
  GENERIC("generic", setOf()),
  C("c", setOf("c", "cc", "cpp", "h", "hh", "hpp")),
  JAVA("java", setOf("java")),
  ANDROID("android", setOf("aidl")),
  JAVASCRIPT("javascript", setOf("js", "applejs")),
  TYPESCRIPT("typescript", setOf("ts", "ats")),
  DART("dart", setOf("dart")),
  GO("go", setOf("go")),
  PYTHON("python", setOf("py", "pyw")),
  SCALA("scala", setOf("scala")),
  KOTLIN("kotlin", setOf("kt")),
  ;

  companion object {
    private val RECOGNIZED_EXTENSIONS: Map<String, LanguageClass> = extensionToClassMap()

    private fun extensionToClassMap(): Map<String, LanguageClass> {
      val result = mutableMapOf<String, LanguageClass>()
      for (lang in LanguageClass.entries) {
        for (ext in lang.recognizedFilenameExtensions) {
          result.put(ext, lang)
        }
      }
      return result
    }

    fun fromString(name: String): LanguageClass? {
      for (ruleClass in LanguageClass.entries) {
        if (ruleClass.name == name) {
          return ruleClass
        }
      }
      return null
    }

    /** Returns the LanguageClass associated with the given filename extension, if it's recognized.  */
    fun fromExtension(filenameExtension: String): LanguageClass? {
      return RECOGNIZED_EXTENSIONS[filenameExtension]
    }
  }
}
