package org.jetbrains.bazel.clion.workspace.environment

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.util.system.OS
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.cpp.toolchains.MSVC
import com.jetbrains.cidr.cpp.toolchains.msvc.MSVCCompilerToVersionCache
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.compiler.ClangClCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo

private val LOG = fileLogger()
private const val MSVC_TOOLCHAIN_NAME = "synthetic_msvc_bazel"
private const val CLANG_CL_TOOLCHAIN_NAME = "synthetic_clang_cl_bazel"

/**
 * Creates and configures the environment for a specific compiler. At the moment, only the MSVC compiler requires a
 * custom configuration.
 */
fun createToolEnvironment(settings: CcCompilerInfo): CidrToolEnvironment {
  if (settings.cCompilerKind != settings.cppCompilerKind) {
    LOG.warn("C and Cpp compiler mismatch. Defaulting to Cpp compiler.")
  }

  val environment = when (settings.cppCompilerKind) {
    MSVCCompilerKind -> createMsvcEnvironment(settings)
    ClangClCompilerKind -> createClangClEnvironment(settings)
    else -> null
  }

  return environment ?: createToolEnvironment(settings.environment)
}

private fun createMsvcEnvironment(settings: CcCompilerInfo): CidrToolEnvironment? {
  val toolSetPath = MSVCEnvironmentUtil.getToolSetPath(settings.cppCompiler)
  if (toolSetPath == null) {
    LOG.warn("Could not find tool set path.")
    return null
  }

  val version = MSVCCompilerToVersionCache.getInstance().getCompilerVersion(settings.cppCompiler.toAbsolutePath().toString())

  if (version == null) {
    LOG.warn("Could not derive arch and version.")
    return null
  }

  val toolchain = CPPToolchains.Toolchain(OS.CURRENT)
  toolchain.setName(MSVC_TOOLCHAIN_NAME)
  toolchain.setToolSetKind(CPPToolSet.Kind.MSVC)
  toolchain.setToolSetPath(toolSetPath)

  val environment = createToolEnvironment(settings, toolchain)
  (environment.toolSet as MSVC).setToolsVersion(version)

  return environment
}

private fun createClangClEnvironment(settings: CcCompilerInfo): CidrToolEnvironment? {
  val toolSetPath = MSVCEnvironmentUtil.getToolSetPath(null)
  if (toolSetPath == null) {
    LOG.warn("Could not find tool set path.")
    return null
  }

  val toolchain = CPPToolchains.Toolchain(OS.CURRENT)
  toolchain.setName(CLANG_CL_TOOLCHAIN_NAME)
  toolchain.setToolSetKind(CPPToolSet.Kind.MSVC)
  toolchain.setToolSetPath(toolSetPath)

  return createToolEnvironment(settings, toolchain)
}

/**
 * Creates a CPPEnvironment that forwards the environment variables set on the Bazel compiler settings.
 */
private fun createToolEnvironment(settings: CcCompilerInfo, toolchain: CPPToolchains.Toolchain): CPPEnvironment {
  return object : CPPEnvironment(toolchain) {
    override fun prepare(cl: GeneralCommandLine, prepareFor: PrepareFor) {
      super.prepare(cl, prepareFor)
      cl.environment.putAll(settings.environment)
    }
  }
}

internal fun createToolEnvironment(environment: Map<String, String>): CidrToolEnvironment {
  return if (environment.isEmpty()) {
    CidrToolEnvironment()
  }
  else {
    object : CidrToolEnvironment() {
      override fun prepare(cl: GeneralCommandLine, prepareFor: PrepareFor) {
        super.prepare(cl, prepareFor)
        cl.environment.putAll(environment)
      }
    }
  }
}
