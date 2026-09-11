@file:Suppress("IO_FILE_USAGE") // forced by underlying implementation

package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.BasicCompilerCommandLineShortener
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompiler
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompiler
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerCommandLineShortener
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKindProvider
import com.jetbrains.cidr.lang.workspace.compiler.TempFilesPool
import com.jetbrains.cidr.lang.workspace.compiler.resolver.OCCompilerResolverCache
import org.jetbrains.annotations.ApiStatus
import java.io.File

/**
 * An [OCCompilerKind] wrapper that disables response file usage.
 *
 * Bazel's compiler wrappers cannot read the response files, CLion writes. This kind delegates
 * all behavior to the underlying [delegate] kind, but overrides [getCompilerInstance] to return
 * a [CcGccCompiler], which reports a no-op command line shortener.
 */
@ApiStatus.Internal
abstract class CcCompilerKind private constructor(private val delegate: OCCompilerKind) : OCCompilerKind by delegate {

  object GCC : CcCompilerKind(GCCCompilerKind)

  object CLANG : CcCompilerKind(ClangCompilerKind)

  override fun getId(): OCCompilerId? = delegate.getId()

  override fun toString(): String = "Cc($delegate)"

  override fun skipLanguageNotRelatedSwitches(switches: List<String?>): List<String?> = delegate.skipLanguageNotRelatedSwitches(switches)

  override fun fixPchSwitches(switches: List<String?>): List<String?> = delegate.fixPchSwitches(switches)

  override fun getCommandLineShortener(): OCCompilerCommandLineShortener = BasicCompilerCommandLineShortener()

  override fun getCompilerInstance(
    project: Project,
    compilerExecutable: File,
    compilerWorkingDirectory: File,
    environment: CidrToolEnvironment,
    tempFilesPool: TempFilesPool,
  ): OCCompiler = CcGccCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool)

  override fun getCompilerInstance(
    project: Project,
    compilerExecutable: File,
    compilerWorkingDirectory: File,
    environment: CidrToolEnvironment,
    tempFilesPool: TempFilesPool,
    cache: OCCompilerResolverCache,
  ): OCCompiler = CcGccCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool)
}

private class CcGccCompiler(
  compilerExecutable: File,
  compilerWorkingDirectory: File,
  environment: CidrToolEnvironment,
  tempFilesPool: TempFilesPool,
) : GCCCompiler(compilerExecutable, compilerWorkingDirectory, environment, tempFilesPool) {

  // TODO: can we remove this duplicated entry point on the GCCCompiler?
  override fun getCommandLineShortener(): OCCompilerCommandLineShortener = BasicCompilerCommandLineShortener()
}

internal class CcCompilerKindProvider : OCCompilerKindProvider {

  override fun getCompilerKinds(): List<OCCompilerKind> = listOf(CcCompilerKind.GCC, CcCompilerKind.CLANG)
}
