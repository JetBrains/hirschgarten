package org.jetbrains.bazel.assertions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.system.OS
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.jetbrains.bazel.clion.workspace.CcCompilerKind
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.resolveCompilerSwitches
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import java.nio.file.Files

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

  suspend fun containsCachedHeaders(fileName: String, project: Project, symlink: Boolean): CompilerSettingsAssert {
    isNotNull()
    val header = actual.resolveHeader(fileName)
    checkNotNull(header)

    val hardLinks = project.connection.runWithServer { server ->
      server.outFileHardLinks as DefaultBazelOutputFileHardLinks
    }
    assertThat(header.toNioPath().startsWith(hardLinks.cacheDir))
      .withFailMessage("file does not reside in the include cache: ${header.path}")
      .isTrue()

    // symlinks do not work on Windows CI runners
    if (OS.CURRENT != OS.Windows) {
      assertThat(Files.isSymbolicLink(header.toNioPath()))
        .withFailMessage("Symlink status of cached file ${header.path} did not match expectation.")
        .isEqualTo(symlink)
    }
    return this
  }

  fun containsWorkspaceHeader(fileName: String, project: Project): CompilerSettingsAssert {
    isNotNull()
    val header = actual.resolveHeader(fileName)
    checkNotNull(header)

    assertThat(header.toNioPath().startsWith(project.rootDir.toNioPath()))
      .withFailMessage(": ${header.path}")
      .isTrue()

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
