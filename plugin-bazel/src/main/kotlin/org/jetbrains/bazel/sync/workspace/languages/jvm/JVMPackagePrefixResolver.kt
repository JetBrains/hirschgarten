package org.jetbrains.bazel.sync.workspace.languages.jvm

import java.nio.file.Path

interface JVMPackagePrefixResolver {
  fun resolveJvmPackagePrefix(source: Path): String? = null
}
