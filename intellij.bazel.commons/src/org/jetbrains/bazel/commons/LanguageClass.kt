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

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * Supported languages/technologies
 *
 * NOTE: serialId is being used in serialization - keep it unchanged
 *
 */
@ApiStatus.Internal
data class LanguageClass(
  val languageName: String,
  val recognizedFilenameExtensions: Set<String>,

  // When collection FUS hide the overridden language to not to duplicate statistics
  // TODO: develop more elegant solution
  val fusOverride: LanguageClass? = null
) {
  override fun toString(): String = languageName

  companion object {
    /** Returns the LanguageClass associated with the given filename extension, if it's recognized.  */
    fun fromExtension(filenameExtension: String): LanguageClass? = LanguageClassService.getInstance().fromExtension(filenameExtension)
  }
}

/**
 * Provides a set of recognized bazel language classes.
 * Individual language-specific sub-plugins can use
 * this EP to register new language class
 */
@ApiStatus.Internal
interface LanguageClassProvider {
  val languages: List<LanguageClass>

  companion object {
    val ep: ExtensionPointName<LanguageClassProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.languageClassProvider")
  }
}

@Service(Service.Level.APP)
@ApiStatus.Internal
class LanguageClassService {

  private val languages =
    LanguageClassProvider.ep.extensionList.flatMap { it.languages }

  private val RECOGNIZED_EXTENSIONS: Map<String, LanguageClass> =
    buildMap {
      languages.forEach { lang ->
        lang.recognizedFilenameExtensions.forEach { ext ->
          if (get(ext) != null)
            error("Duplicated recognized filename extension '${ext}'")
          put(ext, lang)
        }
      }
    }

  fun fromName(languageName: String): LanguageClass? = languages.find { it.languageName == languageName }
  fun fromExtension(filenameExtension: String): LanguageClass? = RECOGNIZED_EXTENSIONS[filenameExtension]
  fun fromPath(path: Path): LanguageClass? = RECOGNIZED_EXTENSIONS[path.extension]
  fun fromPath(path: String): LanguageClass? = RECOGNIZED_EXTENSIONS[path.substringAfterLast(".", missingDelimiterValue = "")]

  @ApiStatus.Internal
  companion object {
    fun getInstance(): LanguageClassService = service()
  }
}
