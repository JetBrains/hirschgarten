package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.plugins.bsp.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.Module
import java.util.Locale
import kotlin.io.path.name

// https://youtrack.jetbrains.com/issue/BAZEL-1672
class ModulesToCompiledSourceCodeInsideJarExcludeTransformer {
  fun transform(modules: List<Module>): CompiledSourceCodeInsideJarExclude =
    CompiledSourceCodeInsideJarExclude(calculateRelativePathsInsideJarToExclude(modules))

  private fun calculateRelativePathsInsideJarToExclude(modules: List<Module>): Set<String> =
    modules
      .asSequence()
      .filterIsInstance<JavaModule>()
      .flatMap { javaModule -> javaModule.sourceRoots }
      .filterNot { sourceRoot -> sourceRoot.generated }
      .flatMap { sourceRoot ->
        val sourceName = sourceRoot.sourcePath.name

        val classNames =
          if (sourceName.endsWith(".java")) {
            listOf(sourceName, sourceName.removeSuffix(".java") + ".class")
          } else if (sourceName.endsWith(".kt")) {
            val withoutExtension = sourceName.removeSuffix(".kt")
            // E.g. main.kt -> MainKt.class
            val kotlinFileClassName = "${withoutExtension.capitalize()}Kt.class"
            listOf(sourceName, "$withoutExtension.class", kotlinFileClassName)
          } else {
            return@flatMap emptyList<String>()
          }

        val packagePrefix = sourceRoot.packagePrefix.replace(".", "/")

        classNames.map { className ->
          if (packagePrefix.isNotEmpty()) "$packagePrefix/$className" else className
        }
      }.toSet()

  private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
