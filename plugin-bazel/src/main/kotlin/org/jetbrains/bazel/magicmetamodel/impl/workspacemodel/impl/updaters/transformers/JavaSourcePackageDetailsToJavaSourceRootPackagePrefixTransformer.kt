package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.utils.allAncestorsSequence
import java.nio.file.Path
import kotlin.io.path.pathString

internal data class JavaSourcePackageDetails(val source: Path, val sourceRoots: Set<Path>)

internal data class JavaSourceRootPackagePrefix(val packagePrefix: String)

internal object JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer :
  WorkspaceModelEntityBaseTransformer<JavaSourcePackageDetails, JavaSourceRootPackagePrefix> {
  private const val PACKAGE_DELIMITER = '.'

  override fun transform(inputEntity: JavaSourcePackageDetails): JavaSourceRootPackagePrefix {
    val packagePrefix = calculateRawPackagePrefix(inputEntity.source, inputEntity.sourceRoots)

    return JavaSourceRootPackagePrefix(packagePrefix)
  }

  private fun calculateRawPackagePrefix(sourceDir: Path, sourceRoots: Set<Path>): String {
    val sourceDirRawPath = sourceDir.pathString
    val matchingRootRawPath = calculateMatchingRootPath(sourceDir, sourceRoots)?.pathString

    val packagePrefixAsRawPath = removeRootRawPathFromSourceRawPath(sourceDirRawPath, matchingRootRawPath)

    return mapPackageAsRawPathToPackageRepresentation(packagePrefixAsRawPath)
  }

  private fun calculateMatchingRootPath(sourceDir: Path, sourceRoots: Set<Path>): Path? =
    sourceDir
      .allAncestorsSequence()
      .firstOrNull { it in sourceRoots }

  private fun removeRootRawPathFromSourceRawPath(sourceDirRawPath: String, sourceRootRawPath: String?): String {
    val rootRawPathToRemove = sourceRootRawPath ?: ""

    if (rootRawPathToRemove.isEmpty()) return ""

    return sourceDirRawPath.removePrefix(rootRawPathToRemove)
  }

  private fun mapPackageAsRawPathToPackageRepresentation(packageAsRawPath: String): String =
    trimSlashes(packageAsRawPath).replace('/', PACKAGE_DELIMITER)

  private fun trimSlashes(pathToTrim: String): String = pathToTrim.trim { it == '/' }
}
