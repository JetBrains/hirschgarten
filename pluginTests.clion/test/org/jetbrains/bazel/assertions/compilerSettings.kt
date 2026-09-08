package org.jetbrains.bazel.assertions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import kotlin.collections.orEmpty

internal fun Project.findCompilerSettings(relativePath: String, language: OCLanguageKind = CLanguageKind.CPP): OCCompilerSettings {
  val file = rootDir.findFileByRelativePath(relativePath)
  file.shouldNotBeNull()

  val configurations = OCWorkspace.getInstance(this).getConfigurationsForFile(file)
  configurations.shouldHaveSize(1)

  return configurations.single().getCompilerSettings(language, file)
}

private fun OCCompilerSettings.resolveHeader(name: String): VirtualFile? {
  return headersSearchRoots.allRoots.asSequence()
    .mapNotNull { it.virtualFile }
    .mapNotNull { it.findFileByRelativePath(name) }
    .firstOrNull { it.exists() }
}

internal fun OCCompilerSettings.shouldContainHeaders(vararg expected: String) {
  val resolved = expected.filter { resolveHeader(it) != null }
  resolved.shouldContainAll(*expected)
}

internal fun OCCompilerSettings.shouldContainSwitches(vararg expected: String) {
  val actual = compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty()
  actual.shouldContainAll(*expected)
}

internal fun OCCompilerSettings.shouldNotContainSwitches(vararg expected: String) {
  val actual = compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty()
  expected.forEach { actual.shouldNotContain(it) }
}

internal fun OCCompilerSettings.shouldHaveCompiler(expected: OCCompilerId) {
  compilerKind?.id.shouldBe(expected)
}

private fun OCCompilerSettings.resolveDefine(needle: String): String? {
  for (define in preprocessorDefines) {
    val definition = define.removePrefix("#define ").trimStart()
    val name = definition.takeWhile { !it.isWhitespace() }

    if (name == needle) {
      return definition.drop(name.length).trimStart()
    }
  }

  return null
}

internal fun OCCompilerSettings.shouldContainDefines(vararg expected: String) {
  val resolved = expected.mapNotNull { definition ->
    val name = definition.substringBefore('=')
    resolveDefine(name)?.let { value -> "$name=$value" }
  }

  resolved.shouldContainAll(*expected)
}
