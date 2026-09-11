package org.jetbrains.bazel.assertions

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.jetbrains.bazel.clion.workspace.CcCompilerKind
import org.jetbrains.bazel.resolveCompilerSwitches
import kotlin.jvm.java

internal fun assertThat(actual: OCCompilerSettings?) = CompilerSettingsAssert(actual)

internal class CompilerSettingsAssert(actual: OCCompilerSettings?) :
  AbstractObjectAssert<CompilerSettingsAssert, OCCompilerSettings>(actual, CompilerSettingsAssert::class.java) {

  fun hasCompiler(expected: OCCompilerId): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.compilerKind?.id).isEqualTo(expected)
    return this
  }

  fun hasCompilerKindWrapper(): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.compilerKind).isInstanceOf(CcCompilerKind::class.java)
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
    assertThat(actual.resolveCompilerSwitches()).contains(*expected)
    return this
  }

  fun doesNotContainSwitches(vararg expected: String): CompilerSettingsAssert {
    isNotNull()
    assertThat(actual.resolveCompilerSwitches()).doesNotContain(*expected)
    return this
  }

  /** Asserts that the preprocessor define [name] has exactly the value [value]. */
  fun hasDefine(name: String, value: String): CompilerSettingsAssert {
    return hasDefine(name, condition("exactly '$value'") { it == value })
  }

  /** Asserts that the preprocessor define [name] exists, and that [value] matches its value. */
  fun hasDefine(name: String, value: Condition<in String>): CompilerSettingsAssert {
    isNotNull()

    assertThat(actual.resolveDefine(name))
      .describedAs("the value of the preprocessor define '%s'", name)
      .isNotNull()
      .has(value)

    return this
  }
}

private fun OCCompilerSettings.resolveHeader(name: String): VirtualFile? {
  return headersSearchRoots.allRoots.asSequence()
    .mapNotNull { it.virtualFile }
    .mapNotNull { it.findFileByRelativePath(name) }
    .firstOrNull { it.exists() }
}

private fun OCCompilerSettings.resolveDefine(needle: String): String? {
  for (define in preprocessorDefines) {
    val definition = define.removePrefix("#define ").trimStart()
    val name = definition.takeWhile { !it.isWhitespace() }

    if (name == needle) {
      return definition.drop(name.length).trim()
    }
  }

  return null
}
