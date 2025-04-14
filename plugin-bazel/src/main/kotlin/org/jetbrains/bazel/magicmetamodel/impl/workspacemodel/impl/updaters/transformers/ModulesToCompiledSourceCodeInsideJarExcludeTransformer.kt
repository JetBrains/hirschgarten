package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import java.util.Locale
import kotlin.io.path.name

// https://youtrack.jetbrains.com/issue/BAZEL-1672
class ModulesToCompiledSourceCodeInsideJarExcludeTransformer {
  fun transform(moduleDetails: Collection<ModuleDetails>): CompiledSourceCodeInsideJarExclude =
    CompiledSourceCodeInsideJarExclude(
      calculateRelativePathsInsideJarToExclude(moduleDetails),
      calculateNamesInsideJarToExclude(moduleDetails),
    )

  private fun calculateRelativePathsInsideJarToExclude(moduleDetails: Collection<ModuleDetails>): Set<String> =
    moduleDetails
      .asSequence()
      .flatMap { moduleDetails -> moduleDetails.target.sources }
      .filterNot { sourceRoot -> sourceRoot.generated }
      .flatMap { sourceRoot ->
        val sourceName =
          sourceRoot.path.fileName.toString()

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

        val packagePrefix = sourceRoot.jvmPackagePrefix?.replace(".", "/") ?: ""

        classNames.map { className ->
          if (packagePrefix.isNotEmpty()) "$packagePrefix/$className" else className
        }
      }.toSet()

  private fun calculateNamesInsideJarToExclude(moduleDetails: Collection<ModuleDetails>): Set<String> =
    moduleDetails
      .asSequence()
      .flatMap { moduleDetails -> moduleDetails.target.resources }
      .map { resource -> resource.name }
      // IntelliJ plugins use XML files like plugin.xml, and we want Java/Kotlin resolve to prefer actual files over files inside JARs.
      // TODO: check if this helps with other projects (that use, e.g., Spring or generated GraphQL) and expand this list when needed
      .filter { name -> name.endsWith(".xml") }
      .toSet()

  private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
