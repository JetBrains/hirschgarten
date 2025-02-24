package org.jetbrains.bsp.protocol

object RustPackageOrigin {
  const val Dependency = "dependency"
  const val Stdlib = "stdlib"
  const val StdlibDependency = "stdlib-dependency"
  const val Workspace = "workspace"
}
