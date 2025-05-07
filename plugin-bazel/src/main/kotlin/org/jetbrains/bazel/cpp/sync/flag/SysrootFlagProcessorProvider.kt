package org.jetbrains.bazel.cpp.sync.flag

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.WorkspaceRoot
import org.jetbrains.bazel.sync.task.bazelProject
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

// See com.google.idea.blaze.cpp.SysrootFlagProcessor
class SysrootFlagProcessorProvider : BazelCompilerFlagsProcessorProvider {
  class SysrootFlagProcessor internal constructor(val workspaceRoot: WorkspaceRoot) :
    BazelCompilerFlagsProcessorProvider.BazelCompilerFlagsProcessor {
      // There's also multiple flag ["--sysroot", "value"] version, but we're only handling the
      // single flag version for now.
      val pattern: Pattern = Pattern.compile("^(--sysroot=)(.*)$")

      override fun processFlags(flags: List<String>): List<String> = flags.map { map(it) }

      private fun map(flag: String): String {
        // For some reasons sysroot needs to be an absolute path for clangd to find the headers,
        // even if clangd's CWD is the workspace root, and the flag is relative to the workspace root.
        // clang by itself seems to work okay with relative path.
        //
        // Given a --sysroot, the compiler should then know about the directories present in
        // CToolchainIdeInfo#builtInIncludeDirectories()
        //
        // So, either
        //  * Make the sysroot an absolute path so that the built-in include directories are found
        //  * Or, explicitly pass flags for each of the CToolchainIdeInfo#builtInIncludeDirectories().
        //    The "normal" flags that the driver uses are internal like "-internal-externc-isystem",
        //    so the closest thing we could pass in externally would be "-isystem".
        val m: Matcher = pattern.matcher(flag)
        if (m.matches()) {
          val flagPrefix: String? = m.group(1)
          val path: String? = m.group(2)
          if (File(path).isAbsolute) {
            return flag
          }
          val workspacePath: File = workspaceRoot.path().resolve(path).toFile()
          return flagPrefix + workspacePath.absolutePath
        } else {
          return flag
        }
      }
    }

  override fun getProcessor(project: Project): BazelCompilerFlagsProcessorProvider.BazelCompilerFlagsProcessor? =
    SysrootFlagProcessor(WorkspaceRoot(project.bazelProject.bazelInfo.workspaceRoot))
}
