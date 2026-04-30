package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.vfs.JarFileSystem
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bsp.protocol.LibraryItem
import java.util.Locale
import kotlin.io.path.invariantSeparatorsPathString

// https://youtrack.jetbrains.com/issue/BAZEL-1672
@ApiStatus.Internal
class CompiledSourceCodeInsideJarExcludeTransformer {
  fun transform(moduleDetails: Collection<ModuleDetails>,
                libraryItems: List<LibraryItem>,
                packagePrefixes: JvmPackagePrefixCalculator
  ): CompiledSourceCodeInsideJarExclude? {
    val librariesFromInternalTargetsUrls = calculateLibrariesFromInternalTargetsUrls(libraryItems)
    if (librariesFromInternalTargetsUrls.isEmpty()) return null
    return CompiledSourceCodeInsideJarExclude(
      calculateRelativePathsInsideJarToExclude(moduleDetails, packagePrefixes),
      librariesFromInternalTargetsUrls,
    )
  }

  private fun calculateRelativePathsInsideJarToExclude(
    moduleDetails: Collection<ModuleDetails>,
    packagePrefixes: JvmPackagePrefixCalculator
  ): Set<String> {
    val result = HashSet<String>()
    for (module in moduleDetails) {
      val jvmPackagePrefixes = packagePrefixes.get(module.target)
      val sourceRoots = module.target.sources
      for (sourceRoot in sourceRoots) {
        if (sourceRoot.generated)
          continue

        val sourceName = sourceRoot.path.fileName.toString()

        val classNames =
          if (sourceName.endsWith(".java")) {
            listOf(sourceName, sourceName.removeSuffix(".java") + ".class")
          }
          else if (sourceName.endsWith(".kt")) {
            val withoutExtension = sourceName.removeSuffix(".kt")
            // E.g. main.kt -> MainKt.class
            val kotlinFileClassName = "${withoutExtension.capitalize()}Kt.class"
            listOf(sourceName, "$withoutExtension.class", kotlinFileClassName)
          }
          else {
            continue
          }

        val packagePrefix = jvmPackagePrefixes[sourceRoot.path]?.replace(".", "/") ?: ""
        classNames.forEach { className ->
          result.add(if (packagePrefix.isNotEmpty()) "$packagePrefix/$className" else className)
        }
      }
    }

    return result
  }

  private fun calculateLibrariesFromInternalTargetsUrls(libraryItems: List<LibraryItem>): Set<String> =
    libraryItems
      .asSequence()
      .filter { libraryItem -> libraryItem.containsInternalJars }
      .flatMap { libraryItem -> libraryItem.jars.asSequence() + libraryItem.ijars.asSequence() + libraryItem.sourceJars.asSequence() }
      .map { jarPath -> JarFileSystem.PROTOCOL_PREFIX + jarPath.invariantSeparatorsPathString + JarFileSystem.JAR_SEPARATOR }
      .toSet()

  private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
