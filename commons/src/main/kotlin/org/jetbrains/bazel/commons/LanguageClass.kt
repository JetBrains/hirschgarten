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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/** Language classes.
 * serialId is being used in serialization - keep it unchanged
 * */
enum class LanguageClass(
  val serialId: Int,
  val languageName: String,
  private val recognizedFilenameExtensions: Set<String>,
) {
  GENERIC(0, "generic", setOf()),
  JAVA(2, "java", setOf("java")),
  JAVASCRIPT(4, "javascript", setOf("js", "applejs")),
  TYPESCRIPT(5, "typescript", setOf("ts", "ats")),
  DART(6, "dart", setOf("dart")),
  GO(7, "go", setOf("go")),
  PYTHON(8, "python", setOf("py", "pyw")),
  SCALA(9, "scala", setOf("scala")),
  KOTLIN(10, "kotlin", setOf("kt")),
  THRIFT(11, "thrift", setOf("thrift")),
  PROTOBUF(12, "protobuf", setOf("proto", "protodevel")),
  ULTIMATE(13, "ultimate", setOf())
  ;

  override fun toString(): String = name

  companion object {
    private val VALUE_BY_SERIAL_ID: Int2ObjectOpenHashMap<LanguageClass> =
      Int2ObjectOpenHashMap(LanguageClass.entries.associateBy { it.serialId })
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
     * TODO(andrzej): https://youtrack.jetbrains.com/issue/BAZEL-1964
     */
    fun fromLanguage(language: Language): LanguageClass? =
      when (language) {
        Language.JAVA -> JAVA
        Language.GO -> GO
        Language.SCALA -> SCALA
        Language.KOTLIN -> KOTLIN
        Language.PYTHON -> PYTHON
        Language.THRIFT -> THRIFT
        Language.PROTOBUF -> PROTOBUF
      }

    /** Returns the LanguageClass associated with the given filename extension, if it's recognized.  */
    fun fromExtension(filenameExtension: String): LanguageClass? = RECOGNIZED_EXTENSIONS[filenameExtension]

    fun fromSerialId(id: Int): LanguageClass? = VALUE_BY_SERIAL_ID.get(id)
  }
}
