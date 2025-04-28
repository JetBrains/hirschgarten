/*
 * Copyright 2016-2024 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.commons

/** Language classes.  */
enum class LanguageClass(val languageName: String, private val recognizedFilenameExtensions: Set<String>) {
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
  THRIFT("thrift", setOf("thrift")),
  ;

  override fun toString(): String = name

  companion object {
    private val RECOGNIZED_EXTENSIONS: Map<String, LanguageClass> =
      extensionToClassMap()

    private fun extensionToClassMap(): Map<String, LanguageClass> {
      val result = mutableMapOf<String, LanguageClass>()
      LanguageClass.entries.forEach { lang ->
        lang.recognizedFilenameExtensions.forEach { ext ->
          result[ext] = lang
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

    /**
     * This is temporarily for converting from [Language] to [LanguageClass].
     * TODO(andrzej): merge [Language] and [LanguageClass] into one
     */
    fun fromLanguage(language: Language): LanguageClass? =
      when (language) {
        Language.JAVA -> JAVA
        Language.GO -> GO
        Language.SCALA -> SCALA
        Language.CPP -> C
        Language.KOTLIN -> KOTLIN
        Language.PYTHON -> PYTHON
        Language.THRIFT -> THRIFT
        Language.ANDROID -> ANDROID
      }

    /** Returns the LanguageClass associated with the given filename extension, if it's recognized.  */
    fun fromExtension(filenameExtension: String): LanguageClass? = RECOGNIZED_EXTENSIONS[filenameExtension]
  }
}
