package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PyNames
import kotlinx.coroutines.awaitAll
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.mapper.normal.BazelOutputFileHardLinks
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.writer

private const val PYINDEX_STORAGE_VERSION: Int = 1
private fun Project.pyIndexStoragePath(): Path = getProjectDataPath("bazel-pyindex-v$PYINDEX_STORAGE_VERSION.db")

@Service(Service.Level.PROJECT)
internal class PythonResolveIndexService(private val project: Project) {
  private val resolveIndexRef = AtomicReference<Map<QualifiedName, Path>>(emptyMap())

  val resolveIndex: Map<QualifiedName, Path>
    get() = resolveIndexRef.get()

  private val outputFilesCache = BazelOutputFileHardLinks.getInstance(project)

  init {
    resolveIndexRef.set(load(project.pyIndexStoragePath()))
  }

  suspend fun updatePythonResolveIndex(pythonTargets: List<RawBuildTarget>) {
    val cache = buildIndex(pythonTargets)

    resolveIndexRef.set(cache)
    store(project.pyIndexStoragePath(), cache)
  }

  private suspend fun buildIndex(pythonTargets: List<RawBuildTarget>): Map<QualifiedName, Path> {
    val executionRoot = project.projectCtx.bazelExecPath?.let { Path.of(it) } ?: return emptyMap()
    val rootDir = Path.of(project.rootDir.path)
    val bazelBin = project.projectCtx.bazelBinPath?.let { Path.of(it) } ?: return emptyMap()

    fun Path.toExecRootRelativePath(): Path {
      if (this.startsWith(bazelBin)) {
        val relativePath = this.relativeTo(bazelBin)
        if (relativePath.startsWith("external"))
          return relativePath.subpath(2, relativePath.nameCount)
        return relativePath
      }

      return this
        .relativeTo(executionRoot)
        // if this is an absolute path pointing to $install_base/external, then we remove this prefix and point it to the same copy under execution root
        .removeParentParentPrefix()
        .let {
          if (it.startsWith("external")) {
            // if this is a file under external/, then we trim the "external/{repo_name}" part
            Path.of(it.subpath(2, it.nameCount).toString())
          }
          else {
            it
          }
        }
    }

    val allPYSourcesInMainWorkspace =
      pythonTargets
        .flatMap {
          it.sources
        }
        .filter { it.path.isPythonFile() && it.path.startsWith(rootDir) }
        .map { it.path.relativeTo(rootDir) }

    val targetNames: List<Map<QualifiedName, Path>> = pythonTargets.map { target ->
      BazelCoroutineService.getInstance(project).startAsync {
        val importsPaths = assembleImportsPaths(target)
        val sourcesRelativePathToAbsolutePath: Map<Path, Path> =
          if (target.id.isMainWorkspace) {
            importsPaths
              .flatMap { importsPath -> allPYSourcesInMainWorkspace.filter { it.startsWith(importsPath) } }
              .ifEmpty {
                target.sources
                  .filter { it.path.isPythonFile() && it.path.startsWith(rootDir) }
                  .map { it.path.relativeTo(rootDir) }
              }
              .associateWith { path -> rootDir.resolve(path) }
          }
          else {
            target.sources
              .filter { it.path.isPythonFile() }
              .associate { sourceItem ->
                sourceItem.path.toExecRootRelativePath() to sourceItem.path
              }
          }
        val getSourcesRelativePathToAbsolutePath: Map<Path, Path> =
          extractPythonBuildTarget(target)?.generatedSources
              ?.associate { path ->
                path.toExecRootRelativePath() to (outputFilesCache.createOutputFileHardLink(path) ?: path.toAbsolutePath())
              }
          ?: emptyMap()
        expandPathsToQualifiedNames(importsPaths, sourcesRelativePathToAbsolutePath + getSourcesRelativePathToAbsolutePath)
      }
    }.awaitAll()

    val qualifiedNamesResolverMap = hashMapOf<QualifiedName, Path>()
    for (targetData in targetNames) {
      for ((qualifiedName, path) in targetData) {
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
  private fun expandPathsToQualifiedNames(importsPaths: List<Path>, filePaths: Map<Path, Path>): Map<QualifiedName, Path> {
    val newMap = hashMapOf<QualifiedName, Path>()
    for ((relativePath, absolutePath) in filePaths) {
      val qualifiedNames = if (importsPaths.isNotEmpty()) {
        importsPaths
          .filter { it != relativePath && relativePath.startsWith(it) }
          .map { relativePath.subpath(it.nameCount, relativePath.nameCount) }
          .filter { it.nameCount > 0 }
          .mapNotNull { it.toQualifiedName() }
      }
      else {
        listOfNotNull(relativePath.toQualifiedName())
      }

      for (qualifiedName in qualifiedNames) {
        var qName = qualifiedName
        var qNamePath = absolutePath

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

  // assembleImportRoots convert "imports" attributes of a bazel python rule to actual imported paths
  private fun assembleImportsPaths(target: BuildTarget): List<Path> {
    val label = target.id as? ResolvedLabel ?: return listOf()
    val ideInfo = extractPythonBuildTarget(target) ?: return listOf()
    var buildParentPath = label.packagePath.toString().let { Path.of(it) }

    // In the case of an external repo the build path could be `/BUILD.bazel`
    // which has a basedir of `/`. In this case we translate this to `.` so
    // that it works in the sub file-system.
    if (0 == buildParentPath.nameCount) {
      buildParentPath = Path.of(".")
    }
    return ideInfo.imports.map {
      buildParentPath.resolve(it).normalize()
    }
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
      } catch (ex: Throwable) {
        logger.warn("Failed to store Python resolve index to $storagePath", ex)
        storagePath.deleteIfExists()
      }
    }

    private fun load(storagePath: Path): Map<QualifiedName, Path> {
      if (!storagePath.exists())
        return emptyMap()

      try {
        val result = mutableMapOf<QualifiedName, Path>()
        storagePath.toFile().reader().use { reader ->
          reader.forEachLine { line ->
            val (qName, path) = line.split(":", limit = 2)
            result[QualifiedName.fromDottedString(qName)] = Path.of(path)
          }
        }
        return result
      } catch (ex: Throwable) {
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

private fun Path.isPythonFile(): Boolean =
  this.isRegularFile() && this.isPythonLanguage()

private fun Path.isPythonLanguage(): Boolean =
  LanguageClass.fromExtension(this.extension) == LanguageClass.PYTHON

private val parentPath = Paths.get("..")

// Drop "../../" from beginning
private fun Path.removeParentParentPrefix(): Path {
  if (this.nameCount > 2 && this.getName(0) == parentPath && this.getName(1) == parentPath) {
    return this.subpath(2, this.nameCount)
  }
  return this
}
