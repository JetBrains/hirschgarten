package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface JVMPackagePrefixResolver {
  fun resolveJvmPackagePrefix(source: Path): String? = null
}
