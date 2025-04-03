package org.jetbrains.bazel.jpsCompilation.utils

import java.nio.file.Path

const val JPS_COMPILED_BASE_DIRECTORY: String = ".jps-compiled"

object JpsPaths {
  private fun getJpsCompiledBasePath(projectBasePath: Path): Path = projectBasePath.resolve(JPS_COMPILED_BASE_DIRECTORY)

  fun getJpsCompiledProductionPath(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBasePath(projectBasePath)
      .resolve("production")
      .resolve("classes")
      .resolve(moduleName)

  fun getJpsCompiledTestPath(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBasePath(projectBasePath)
      .resolve("test")
      .resolve("classes")
      .resolve(moduleName)

  fun getJpsImlModulesPath(projectBasePath: Path): Path = projectBasePath.resolve(".idea").resolve("modules")
}
