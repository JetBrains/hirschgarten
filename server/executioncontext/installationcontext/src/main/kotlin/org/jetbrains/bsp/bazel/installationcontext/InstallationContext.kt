package org.jetbrains.bsp.bazel.installationcontext

import java.nio.file.Path
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContext

data class InstallationContext(
    val javaPath: InstallationContextJavaPathEntity,
    val debuggerAddress: InstallationContextDebuggerAddressEntity?,
    val projectViewFilePath: Path,
    val bazelWorkspaceRootDir: Path,
) : ExecutionContext()
