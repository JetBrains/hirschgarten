package org.jetbrains.bazel.cpp.sync.flag

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Processes compiler flags learned from the build system before passing them on to the IDE, in case
 * there is a mismatch between the build system's compiler and clangd bundled with the IDE.
 * See com.google.idea.blaze.cpp. BlazeCompilerFlagsProcessor
 */
interface BazelCompilerFlagsProcessorProvider {
  interface BazelCompilerFlagsProcessor {
    fun processFlags(flags: List<String>): List<String>
  }

  fun getProcessor(project: Project): BazelCompilerFlagsProcessor?

  companion object {
    val ep =
      ExtensionPointName.Companion.create<BazelCompilerFlagsProcessorProvider>(
        "org.jetbrains.bazel.cpp.sync.flag.BazelCompilerFlagsProcessorProvider",
      )

    fun process(project: Project, flags: List<String>): List<String> {
      var result = flags
      val processors = ep.extensionList.mapNotNull { it.getProcessor(project) }
      for (processor in processors) {
        result = processor.processFlags(result)
      }
      return result
    }
  }
}
