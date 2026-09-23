package com.intellij.bazel.python.backend

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PyNames
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.python.lang.extractPythonBuildTarget
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.target.isWorkspace
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.isGenerated
import org.jetbrains.bsp.protocol.isSource
import org.jetbrains.bsp.protocol.isUserCode
import org.jetbrains.bsp.protocol.relativeNioPath
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.relativeToOrNull
import kotlin.io.path.writer

private const val PYINDEX_STORAGE_VERSION: Int = 2
private fun Project.pyIndexStoragePath(): Path = getProjectDataPath("bazel-pyindex-v$PYINDEX_STORAGE_VERSION.db")

private class ResolveIndexSnapshot(
  val qualifiedNamesToPaths: Map<QualifiedName, Path>,
  val shortestQualifiedNamesByPath: Map<Path, QualifiedName>,
  val directChildrenByQualifiedName: Map<QualifiedName, Map<String, Path>>,
  computeStubScope: () -> GlobalSearchScope,
) {
  val stubScope: GlobalSearchScope by lazy(computeStubScope) // search scope for Python stubs
}

private data class IndexContribution(
  val qualifiedNamesToPaths: Map<QualifiedName, Path>,
  val sharedImports: List<Path>?,
)

@Service(Service.Level.PROJECT)
internal class PythonResolveIndexService(private val project: Project) {
  private val resolveIndexSnapshotRef = AtomicReference(newSnapshot(load(project.pyIndexStoragePath())))

  val resolveIndex: Map<QualifiedName, Path>
    get() = resolveIndexSnapshotRef.get().qualifiedNamesToPaths

  fun findShortestQualifiedName(path: Path): QualifiedName? {
    val shortestQualifiedNamesByPath = resolveIndexSnapshotRef.get().shortestQualifiedNamesByPath
    return shortestQualifiedNamesByPath[path]
           ?: path.takeIf { it.isPackageInitFile() }?.parent?.let { shortestQualifiedNamesByPath[it] }
  }

  fun findShortestQualifiedName(file: VirtualFile): QualifiedName? =
    file.toNioPathOrNull()?.let { findShortestQualifiedName(it) }

  fun findDirectChildren(qualifiedName: QualifiedName): Map<String, Path> =
    resolveIndexSnapshotRef.get().directChildrenByQualifiedName[qualifiedName].orEmpty()

  fun getStubScope(): GlobalSearchScope = resolveIndexSnapshotRef.get().stubScope

  suspend fun updatePythonResolveIndex(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    pythonTargets: List<BuildTarget>,
    generatedSourcePaths: Map<Path, Path>,
  ) {
    val nameToPathIndex = buildIndex(context, snapshot, pythonTargets, generatedSourcePaths)

    updateResolveIndexSnapshot(nameToPathIndex)
    store(project.pyIndexStoragePath(), nameToPathIndex)
  }

  @VisibleForTesting
  fun updateResolveIndexSnapshot(newNameToPathIndex: Map<QualifiedName, Path>) {
    resolveIndexSnapshotRef.set(newSnapshot(newNameToPathIndex))
  }

  private fun newSnapshot(resolveIndex: Map<QualifiedName, Path>): ResolveIndexSnapshot =
    ResolveIndexSnapshot(
      resolveIndex,
      buildShortestQualifiedNamesByPath(resolveIndex),
      buildDirectChildrenByQualifiedName(resolveIndex),
    ) { buildStubScope(resolveIndex.values) }

  private fun buildStubScope(paths: Collection<Path>): GlobalSearchScope {
    val virtualFileManager = VirtualFileManager.getInstance()
    val files = paths.asSequence()
      .mapNotNull { virtualFileManager.findFileByNioPath(it) }
      .flatMap { if (it.isDirectory) it.findPackageInitFiles() else listOf(it) }
      .toSet()
    return if (files.isEmpty()) GlobalSearchScope.EMPTY_SCOPE else GlobalSearchScope.filesWithLibrariesScope(project, files)
  }

  private suspend fun buildIndex(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    pythonTargets: List<BuildTarget>,
    generatedSourcePaths: Map<Path, Path>,
  ): Map<QualifiedName, Path> {
    val rootDir = Path.of(project.rootDir.path)
    val localRepositories = snapshot.repoMapping.getLocalRepositories()
    val execrootResolver = DefaultOutputLocationResolver.createExecrootResolving(context.bazelInfo)

    fun resolve(location: OutputLocation): Path? = context.outputResolver.resolve(location, localRepositories)

    fun workspaceRelativePythonSources(target: BuildTarget): List<Path> =
      target.sources.getOutputLocations()
        .filter { it.isPythonLanguage() && it.isSource && it.isUserCode(snapshot.repoMapping) }
        .mapNotNull { resolve(it) }
        .filter { it.isPythonFile() }
        .mapNotNull { it.relativeToOrNull(rootDir) }
        .toList()

    val (importsByTarget, sharedSourcesByImports, sharedIndexesByImports) =
      BazelCoroutineService.getInstance(project).startAsync {
        val importsByTarget = pythonTargets.map { PythonImportUtils.assembleExplicitImportsPaths(it) }
        val workspaceImports = importsByTarget.indices
          .filter { pythonTargets[it].isWorkspace(snapshot.repoMapping) && importsByTarget[it].isNotEmpty() }
          .map { importsByTarget[it] }
          .distinct()
        val importRoots = workspaceImports.flatten().toSet()
        val sourcesByImportRoot = hashMapOf<Path, MutableList<Path>>()
        if (importRoots.isNotEmpty()) {
          pythonTargets.asSequence()
            .flatMap { workspaceRelativePythonSources(it) }
            .distinct()
            .forEach { source ->
              ensureActive()
              for (length in 1..source.nameCount) {
                val prefix = source.subpath(0, length)
                if (prefix in importRoots) {
                  sourcesByImportRoot.getOrPut(prefix) { mutableListOf() }.add(source)
                }
              }
            }
        }
        val sharedSourcesByImports = workspaceImports.associateWith { imports ->
          imports.flatMap { sourcesByImportRoot[it].orEmpty() }
        }
        val sharedIndexesByImports = sharedSourcesByImports.mapValues { (imports, sources) ->
          ensureActive()
          if (sources.isEmpty()) null
          else expandPathsToQualifiedNames(
            PythonImportUtils.assembleQualifiedNameImportPaths(imports),
            sources.associateWith { rootDir.resolve(it) },
          )
        }
        Triple(importsByTarget, sharedSourcesByImports, sharedIndexesByImports)
      }.await()

    val targetContributions: List<IndexContribution> = pythonTargets.mapIndexed { index, target ->
      BazelCoroutineService.getInstance(project).startAsync {
        val explicitImportsPaths = importsByTarget[index]
        val qualifiedNameImportPaths = PythonImportUtils.assembleQualifiedNameImportPaths(explicitImportsPaths)
        val generatedSourceFiles =
          target.sources.getOutputLocations().filter { it.isGenerated } +
          (extractPythonBuildTarget(target)?.generatedSources?.getOutputLocations() ?: emptySequence())
        val generatedPythonSources = generatedSourceFiles.distinct().filter { it.isPythonLanguage() }.toList()
        val sharedIndex = if (generatedPythonSources.isEmpty() && target.isWorkspace(snapshot.repoMapping)) {
          sharedIndexesByImports[explicitImportsPaths]
        }
        else null
        if (sharedIndex != null) {
          IndexContribution(sharedIndex, explicitImportsPaths)
        }
        else {
          val sourcesRelativePathToAbsolutePath: Map<Path, Path> =
            if (target.isWorkspace(snapshot.repoMapping)) {
              sharedSourcesByImports[explicitImportsPaths].orEmpty()
                .ifEmpty { workspaceRelativePythonSources(target) }
                .associateWith { path -> rootDir.resolve(path) }
            }
            else {
              target.sources.getOutputLocations()
                .filter { it.isPythonLanguage() }
                .mapNotNull { location ->
                  val relativePath = location.toRepoRelativePath() ?: return@mapNotNull null
                  val path = resolve(location)?.takeIf { it.isPythonFile() } ?: return@mapNotNull null
                  relativePath to path
                }
                .toMap()
            }
          val generatedSourcesRelativePathToAbsolutePath = generatedPythonSources.mapNotNull { location ->
            val relativePath = location.toRepoRelativePath() ?: return@mapNotNull null
            val path = execrootResolver.resolve(location, localRepositories) ?: return@mapNotNull null
            relativePath to (generatedSourcePaths[path] ?: path.toAbsolutePath())
          }.toMap()
          val allSourcesRelativePathToAbsolutePath = sourcesRelativePathToAbsolutePath + generatedSourcesRelativePathToAbsolutePath
          IndexContribution(
            expandPathsToQualifiedNames(qualifiedNameImportPaths, allSourcesRelativePathToAbsolutePath),
            null,
          )
        }
      }
    }.awaitAll()

    val lastSharedIndexByImports = targetContributions.withIndex().mapNotNull { (index, contribution) ->
      contribution.sharedImports?.let { it to index }
    }.toMap()
    val seenSharedImports = hashSetOf<List<Path>>()
    val qualifiedNamesResolverMap = hashMapOf<QualifiedName, Path>()
    // Keep the first write for map iteration order and the last write for target overwrite order.
    for ((index, contribution) in targetContributions.withIndex()) {
      val sharedImports = contribution.sharedImports
      if (sharedImports != null && !seenSharedImports.add(sharedImports) && lastSharedIndexByImports[sharedImports] != index) continue
      for ((qualifiedName, path) in contribution.qualifiedNamesToPaths) {
        qualifiedNamesResolverMap[qualifiedName] = path
      }
    }
    return qualifiedNamesResolverMap
  }

  /*
   * Expand the relative->absolute path map to include the parent paths
   * e.g. for an entry aaa/bbb/ccc.py -> /absolute/aaa/bbb/ccc.py,
   * this function will also add (aaa/bbb -> /absolute/aaa/bbb) and (aaa -> /absolute/aaa) into the new map,
   * so that intermediate directories are also tracked
   *
   * All intermediate relative paths are converted to QualifiedNames for the result map
   * */
  private fun expandPathsToQualifiedNames(
    importsPaths: List<PythonImportUtils.ImportsPath>,
    filePaths: Map<Path, Path>,
  ): Map<QualifiedName, Path> {
    val newMap = hashMapOf<QualifiedName, Path>()
    for ((rootRelativePath, absolutePath) in filePaths) {
      val qualifiedNames =
        if (importsPaths.isNotEmpty()) {
          importsPaths
            .mapNotNull { it.relativizePath(rootRelativePath)?.toQualifiedName() }
        }
        else {
          listOfNotNull(rootRelativePath.toQualifiedName())
        }

      for (qualifiedName in qualifiedNames) {
        var qName = qualifiedName

        // qualified name of `__init__` is the py package, so walk starts at the package directory
        var qNamePath = if (absolutePath.isPackageInitFile()) absolutePath.parent else absolutePath

        while (qName.componentCount > 0) {
          if (newMap.containsKey(qName))
            break

          newMap[qName] = qNamePath

          qName = qName.removeLastComponent()
          qNamePath = qNamePath.parent
        }
      }
    }

    return newMap
  }

  private fun buildShortestQualifiedNamesByPath(resolveIndex: Map<QualifiedName, Path>): Map<Path, QualifiedName> {
    val result = hashMapOf<Path, QualifiedName>()
    for ((qualifiedName, path) in resolveIndex) {
      val existingQualifiedName = result[path]
      if (existingQualifiedName == null || qualifiedName.componentCount < existingQualifiedName.componentCount) {
        // keep the shortest matching qualified name
        result[path] = qualifiedName
      }
    }
    return result
  }

  private fun buildDirectChildrenByQualifiedName(resolveIndex: Map<QualifiedName, Path>): Map<QualifiedName, Map<String, Path>> {
    val result = hashMapOf<QualifiedName, MutableMap<String, Path>>()
    for ((qualifiedName, path) in resolveIndex) {
      val childName = qualifiedName.lastComponent ?: continue
      result.getOrPut(qualifiedName.removeLastComponent()) { linkedMapOf() }.putIfAbsent(childName, path)
    }
    return result
  }

  companion object {
    private val logger = logger<PythonResolveIndexService>()

    private fun store(storagePath: Path, map: Map<QualifiedName, Path>) {
      if (map.isEmpty()) {
        storagePath.deleteIfExists()
        return
      }

      try {
        storagePath.createParentDirectories()
        storagePath.writer().use { writer ->
          for ((qName, path) in map) {
            writer.appendLine("$qName:${path.pathString}")
          }
        }
      }
      catch (ex: Throwable) {
        logger.warn("Failed to store Python resolve index to $storagePath", ex)
        storagePath.deleteIfExists()
      }
    }

    private fun load(storagePath: Path): Map<QualifiedName, Path> {
      if (!storagePath.exists())
        return emptyMap()

      try {
        val result = mutableMapOf<QualifiedName, Path>()
        storagePath.reader().use { reader ->
          reader.forEachLine { line ->
            val (qName, path) = line.split(":", limit = 2)
            result[QualifiedName.fromDottedString(qName)] = Path.of(path)
          }
        }
        return result
      }
      catch (ex: Throwable) {
        logger.warn("Failed to load Python resolve index from $storagePath", ex)
        storagePath.deleteIfExists()
        return emptyMap()
      }
    }
  }
}

private fun Path.toQualifiedName(): QualifiedName? {
  val separator = FileSystems.getDefault().separator
  if (!isPythonLanguage()) return null

  val relativePath =
    toString()
      .substringBeforeLast(".") // remove extension
      .removeSuffix(separator + PyNames.INIT)

  val relativePathParts = relativePath.split(separator)
  if (relativePathParts.any { it.contains(".") }) return null
  return QualifiedName.fromComponents(relativePathParts.flatMap { it.split(".") }.filter { it.isNotEmpty() })
}

private fun Path.isPackageInitFile(): Boolean =
  nameWithoutExtension == PyNames.INIT && isPythonLanguage()

internal fun VirtualFile.findPackageInitFiles(): List<VirtualFile> =
  listOfNotNull(findChild(PyNames.INIT_DOT_PY), findChild(PyNames.INIT_DOT_PYI))

private fun Path.isPythonFile(): Boolean =
  this.isRegularFile() && this.isPythonLanguage()

private fun Path.isPythonLanguage(): Boolean =
  LanguageClass.fromExtension(this.extension) == PythonLanguageClass.PYTHON

// we only care about the extension, so the relative path of the location is enough
private fun OutputLocation.isPythonLanguage(): Boolean = relativeNioPath.isPythonLanguage()

// the path inside the repository, without the output root and the `external/<repo>` prefix
private fun OutputLocation.toRepoRelativePath(): Path? = when (this) {
  is OutputLocation.Workspace -> relativeNioPath
  is OutputLocation.External -> relativeNioPath
  is OutputLocation.Output -> relativeNioPath.let { if (it.startsWith("external") && it.nameCount > 2) it.subpath(2, it.nameCount) else it }
  is OutputLocation.Host -> null
}

@ApiStatus.Internal
@TestOnly
fun Project.findBazelPythonShortestQualifiedName(path: Path): String? =
  service<PythonResolveIndexService>().findShortestQualifiedName(path)?.toString()

@ApiStatus.Internal
@TestOnly
fun Project.hasBazelPythonQualifiedName(qualifiedName: String): Boolean =
  service<PythonResolveIndexService>().resolveIndex.keys.any { it.toString() == qualifiedName }

@ApiStatus.Internal
@TestOnly
fun Project.updateBazelPythonResolveIndex(resolveIndex: Map<QualifiedName, Path>) {
  service<PythonResolveIndexService>().updateResolveIndexSnapshot(resolveIndex)
}
