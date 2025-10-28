package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.vfs.JarFileSystem
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bsp.protocol.LibraryItem
import java.util.Locale
import kotlin.io.path.invariantSeparatorsPathString

// https://youtrack.jetbrains.com/issue/BAZEL-1672
class CompiledSourceCodeInsideJarExcludeTransformer {
  fun transform(moduleDetails: Collection<ModuleDetails>, libraryItems: List<LibraryItem>): CompiledSourceCodeInsideJarExclude =
    CompiledSourceCodeInsideJarExclude(
      calculateRelativePathsInsideJarToExclude(moduleDetails),
      calculateLibrariesFromInternalTargetsUrls(libraryItems),
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

  private fun calculateLibrariesFromInternalTargetsUrls(libraryItems: List<LibraryItem>): Set<String> =
    libraryItems
      .asSequence()
      .filter { libraryItem -> libraryItem.isFromInternalTarget }
      .flatMap { libraryItem -> libraryItem.jars.asSequence() + libraryItem.ijars.asSequence() + libraryItem.sourceJars.asSequence() }
      .map { jarPath -> JarFileSystem.PROTOCOL_PREFIX + jarPath.invariantSeparatorsPathString + JarFileSystem.JAR_SEPARATOR }
      .toSet()

  private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
