package org.jetbrains.bazel.assertions

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat

internal fun assertThat(actual: OCCompilerSettings?) = CompilerSettingsAssert(actual)

internal class CompilerSettingsAssert(actual: OCCompilerSettings?) :
  AbstractObjectAssert<CompilerSettingsAssert, OCCompilerSettings>(actual, CompilerSettingsAssert::class.java) {

  fun hasCompiler(expected: OCCompilerId): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.compilerKind?.id).isEqualTo(expected)
    return this
  }

  fun containsHeaders(vararg expected: String): CompilerSettingsAssert {
    isNotNull()
    val resolved = expected.filter { actual.resolveHeader(it) != null }
    assertThat(resolved).contains(*expected)
    return this
  }

  fun containsSwitches(vararg expected: String): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.compilerSwitchesList()).contains(*expected)
    return this
  }

  fun doesNotContainSwitches(vararg expected: String): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.compilerSwitchesList()).doesNotContain(*expected)
    return this
  }

  /** Asserts that the preprocessor defines contain every `name=value` pair in [expected]. */
  fun containsDefines(vararg expected: String): CompilerSettingsAssert {
    isNotNull()
    val resolved = expected.mapNotNull { definition ->
      val name = definition.substringBefore('=')
      actual.resolveDefine(name)?.let { value -> "$name=$value" }
    }
    assertThat(resolved).contains(*expected)
    return this
  }
}

private fun OCCompilerSettings.resolveHeader(name: String): VirtualFile? {
  return headersSearchRoots.allRoots.asSequence()
    .mapNotNull { it.virtualFile }
    .mapNotNull { it.findFileByRelativePath(name) }
    .firstOrNull { it.exists() }
}

private fun OCCompilerSettings.compilerSwitchesList(): List<String> {
  return compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty()
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
