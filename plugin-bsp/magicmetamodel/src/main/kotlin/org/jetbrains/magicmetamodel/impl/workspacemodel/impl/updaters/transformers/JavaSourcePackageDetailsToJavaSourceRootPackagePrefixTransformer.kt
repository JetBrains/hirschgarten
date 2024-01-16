package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.magicmetamodel.extensions.allSubdirectoriesSequence
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.toPath

internal data class JavaSourcePackageDetails(
  val sourceURI: URI,
  val sourceRoots: List<URI>,
)

public data class JavaSourceRootPackagePrefix(
  val packagePrefix: String,
)

internal object JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer :
  WorkspaceModelEntityBaseTransformer<JavaSourcePackageDetails, JavaSourceRootPackagePrefix> {

  override fun transform(inputEntity: JavaSourcePackageDetails): JavaSourceRootPackagePrefix {
    val packagePrefix = JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformerHelper.calculateRawPackagePrefix(inputEntity.sourceURI, inputEntity.sourceRoots)

    return JavaSourceRootPackagePrefix(packagePrefix)
  }

}

public object JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformerHelper {
  private const val PACKAGE_DELIMITER = '.'

  public fun calculateRawPackagePrefix(sourceDir: URI, sourceRoots: List<URI>): String {
    val sourceDirRawPath = sourceDir.toPath().pathString
    val matchingRootRawPath = calculateMatchingRootPath(sourceDir, sourceRoots)?.pathString

    val packagePrefixAsRawPath = removeRootRawPathFromSourceRawPath(sourceDirRawPath, matchingRootRawPath)

    return mapPackageAsRawPathToPackageRepresentation(packagePrefixAsRawPath)
  }

  private fun calculateMatchingRootPath(sourceDir: URI, sourceRoots: List<URI>): Path? =
    sourceDir.toPath().allSubdirectoriesSequence()
      .firstOrNull { doRootsContainDir(it, sourceRoots) }

  private fun doRootsContainDir(sourceDir: Path, sourceRoots: List<URI>): Boolean =
    sourceRoots.any { it.toPath() == sourceDir }

  private fun removeRootRawPathFromSourceRawPath(sourceDirRawPath: String, sourceRootRawPath: String?): String {
    val rootRawPathToRemove = sourceRootRawPath ?: ""

    return sourceDirRawPath.removePrefix(rootRawPathToRemove)
  }

  private fun mapPackageAsRawPathToPackageRepresentation(packageAsRawPath: String): String =
    trimSlashes(packageAsRawPath).replace('/', PACKAGE_DELIMITER)

  private fun trimSlashes(pathToTrim: String): String =
    pathToTrim.trim { it == '/' }
}
