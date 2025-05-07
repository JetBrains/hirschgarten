package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.UnknownCompilerKind
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionUtil
import org.jetbrains.bazel.cpp.sync.flag.BazelCompilerFlagsProcessorProvider
import java.nio.file.Path

// See com.google.idea.blaze.cpp.BlazeCompilerSettings
data class BazelCompilerSettings(
  val project: Project,
  val cCompiler: Path?,
  val cppCompiler: Path?,
  val cFlags: List<String>,
  val cppFlags: List<String>,
  val compilerVersion: String,
  val compilerEnvironment: Map<String, String>,
  val builtInIncludes: List<ExecutionRootPath>,
) {
  val cCompilerSwitches = getCompilerSwitches(project, cFlags)
  val cppCompilerSwitches = getCompilerSwitches(project, cppFlags)

  fun getCompiler(languageKind: OCLanguageKind?): OCCompilerKind {
    if (languageKind !== CLanguageKind.C && languageKind !== CLanguageKind.CPP) {
      return UnknownCompilerKind
    }

    if (CompilerVersionUtil.isMSVC(compilerVersion)) {
      return MSVCCompilerKind
    }

    if (CompilerVersionUtil.isClang(compilerVersion)) {
      return ClangCompilerKind
    }

    // default to gcc
    return GCCCompilerKind
  }

  fun getCompilerExecutable(lang: OCLanguageKind): Path? {
    if (lang === CLanguageKind.C) {
      return cCompiler
    } else if (lang === CLanguageKind.CPP) {
      return cppCompiler
    }
    // We don't support Objective-C
    return null
  }

  fun getCompilerSwitches(lang: OCLanguageKind, sourceFile: VirtualFile?): List<String> {
    if (lang === CLanguageKind.C) {
      return cCompilerSwitches
    }
    if (lang === CLanguageKind.CPP) {
      return cppCompilerSwitches
    }
    return listOf()
  }

  private fun getCompilerSwitches(project: Project, allCompilerFlags: List<String>): List<String> =
    BazelCompilerFlagsProcessorProvider.process(project, allCompilerFlags)

  fun getCompilerEnvironment(variable: String): String? = compilerEnvironment[variable]
}
