package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.util.io.isAncestor
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.SourceRoot
import java.net.URI
import java.nio.file.Path

internal object SourcesItemToJavaSourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<SourcesItem, JavaSourceRoot> {

  override fun transform(inputEntities: List<SourcesItem>): List<JavaSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)

    return allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRoots) }
  }

  private fun isNotAChildOfAnySourceDir(sourceRoot: JavaSourceRoot, allSourceRoots: List<JavaSourceRoot>): Boolean =
    allSourceRoots.none { it.sourceDir.isAncestor(sourceRoot.sourceDir.parent) }

  override fun transform(inputEntity: SourcesItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRootsAsURIs(inputEntity)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sources)
      .map { toJavaSourceRoot(it, sourceRoots) }
  }

  private fun getSourceRootsAsURIs(sourcesItem: SourcesItem): List<URI> =
    // TODO?
    (sourcesItem.roots ?: ArrayList()).map(URI::create)

  private fun toJavaSourceRoot(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRoot {
    val packagePrefix = calculatePackagePrefix(sourceRoot, sourceRoots)

    return JavaSourceRoot(
      sourceDir = sourceRoot.sourceDir,
      generated = sourceRoot.generated,
      packagePrefix = packagePrefix.packagePrefix
    )
  }

  private fun calculatePackagePrefix(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRootPackagePrefix {
    val packageDetails = JavaSourcePackageDetails(
      sourceDir = sourceRoot.sourceDir.toUri(),
      sourceRoots = sourceRoots
    )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}

internal object SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK :
  WorkspaceModelEntityPartitionTransformer<SourcesItem, JavaSourceRoot> {

  override fun transform(inputEntities: List<SourcesItem>): List<JavaSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)
    val realRoots = allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRoots) }

    return realRoots.map {
      addExcludedPathsFromSubDirectoriesToSourceRoot(it, allSourceRoots)
    }
  }

  private fun isNotAChildOfAnySourceDir(sourceRoot: JavaSourceRoot, allSourceRoots: List<JavaSourceRoot>): Boolean =
    allSourceRoots.none { it.sourceDir.isAncestor(sourceRoot.sourceDir.parent) }

  private fun addExcludedPathsFromSubDirectoriesToSourceRoot(
    sourceRoot: JavaSourceRoot,
    allSourceRoots: List<JavaSourceRoot>
  ): JavaSourceRoot {
    val excludedFromSubDirs = allSourceRoots
      .filter { sourceRoot.sourceDir.isAncestor(it.sourceDir) }
      .flatMap { it.excludedFiles }

    return sourceRoot.copy(excludedFiles = excludedFromSubDirs)
  }

  override fun transform(inputEntity: SourcesItem): List<JavaSourceRoot> {
    val notHackyTransformationResult = SourcesItemToJavaSourceRootTransformer.transform(inputEntity)

    val directoriesInSourcesItem = filterKindAndMapToUri(inputEntity, SourceItemKind.DIRECTORY)
    val filesInSourceItem = filterKindAndMapToUri(inputEntity, SourceItemKind.FILE)

    val hackyFilesWithExcludedFiles = notHackyTransformationResult
      .filterNot { directoriesInSourcesItem.contains(it.sourceDir.toUri()) }
      .map { calculateExcludedFilesAndUpdateJavaSourceRoot(it, filesInSourceItem) }

    val directoriesInNotHackyTransformationResult = notHackyTransformationResult
      .filter { directoriesInSourcesItem.contains(it.sourceDir.toUri()) }

    return hackyFilesWithExcludedFiles + directoriesInNotHackyTransformationResult
  }

  private fun filterKindAndMapToUri(inputEntity: SourcesItem, kind: SourceItemKind): Set<URI> =
    inputEntity.sources
      .filter { it.kind == kind }
      .map { URI.create(it.uri) }
      .toSet()

  private fun calculateExcludedFilesAndUpdateJavaSourceRoot(
    sourceRoot: JavaSourceRoot,
    filesInSourcesItem: Set<URI>
  ): JavaSourceRoot {
    val excludedFiles = calculateExcludedFiles(sourceRoot.sourceDir, filesInSourcesItem)

    return sourceRoot.copy(excludedFiles = excludedFiles)
  }

  private fun calculateExcludedFiles(path: Path, filesInSourcesItem: Set<URI>): List<Path> =
    path.toFile()
      .walk()
      .filter { it.isFile }
      .filterNot { filesInSourcesItem.contains(it.toURI()) }
      .filter { it.extension == "java" }
      .map { it.toPath() }
      .toList()
}
