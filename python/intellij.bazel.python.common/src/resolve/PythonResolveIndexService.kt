package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PyNames
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.hasExtensionOf
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo

@Service(Service.Level.PROJECT)
@State(name = "PythonResolveIndexService", storages = [Storage("bazelPython.xml")], reportStatistic = true)
internal class PythonResolveIndexService(private val project: Project) : PersistentStateComponent<PythonResolveIndexService.State> {
  var resolveIndex: Map<QualifiedName, VirtualFile?> = emptyMap()
    private set
  private var internalResolveIndex: Map<QualifiedName, Path> = mapOf()

  fun updatePythonResolveIndex(rawTargets: List<RawBuildTarget>) {
    internalResolveIndex = buildIndex(rawTargets.asSequence())
    resolveIndex =
      internalResolveIndex.mapValues { (_, path) -> path.toVirtualFile() }
  }

  private fun buildIndex(rawTargets: Sequence<RawBuildTarget>): Map<QualifiedName, Path> {
    val executionRoot = project.projectCtx.bazelExecPath?.let { Path.of(it) } ?: return emptyMap()
    val rootDir = Path.of(project.rootDir.path)
    val bazelBin = project.projectCtx.bazelBinPath?.let { Path.of(it) } ?: return emptyMap()
    val targets =
      rawTargets
        .filter {
          it.kind.languageClasses.contains(LanguageClass.PYTHON)
        }
    val allPYSourcesInMainWorkspace =
      targets
        .flatMap {
          it.sources
        }.toSet()
        .filter { it.path.hasExtensionOf("py", "pyi") && it.path.startsWith(rootDir) }
        .map { it.path.relativeTo(rootDir) }

    val qualifiedNamesResolverMap = mutableMapOf<QualifiedName, Path>()

    for (target in targets) {
      val importsPaths = assembleImportsPaths(target)
      val pythonTargetInfo = extractPythonBuildTarget(target) ?: continue
      val relativePathToAbsolutePath: Map<Path, Path> =
        if (pythonTargetInfo.isCodeGenerator) {
            pythonTargetInfo.generatedSources
              .flatMap { file ->
                // some code gen rules return directories. we need to figure out what files are there
                if (file.isDirectory()) {
                  Files
                    .walk(file)
                    .filter {
                      it.isRegularFile() && it.hasExtensionOf("py", "pyi")
                    }
                    .map { it.toAbsolutePath() }
                    .toList()
                } else {
                  listOf(file)
                }
              }.associateBy { absolutePath ->
                val relativePath = absolutePath.relativeTo(bazelBin)
                if (relativePath.startsWith("external")) {
                  relativePath.subpath(2, relativePath.nameCount)
                } else {
                  relativePath
                }
              }
        } else if (target.id.isMainWorkspace) {
          importsPaths
            .flatMap { importsPath ->
              allPYSourcesInMainWorkspace.filter { it.startsWith(importsPath) }
            }
            .ifEmpty {
              target.sources
                .filter { it.path.hasExtensionOf("py", "pyi") && it.path.startsWith(rootDir) }
                .map { it.path.relativeTo(rootDir) }
            }
            .toSet()
            .associateWith { path -> rootDir.resolve(path) }
        } else {
          target.sources.associate { sourceItem ->
            val executionRootRelativePath =
              sourceItem.path
                .relativeTo(executionRoot)
                .toString()
                // if this is an absolute path pointing to $install_base/external, then we remove this prefix and point it to the same copy under execution root
                .removePrefix("../../")
                .let { Path.of(it) }
                .let {
                  if (it.startsWith("external")) {
                    // if this is a file under external/, then we trim the "external/{repo_name}" part
                    Path.of(it.subpath(2, it.nameCount).toString())
                  } else {
                    it
                  }
                }

            executionRootRelativePath to sourceItem.path
          }
        }
      val expandedRelativePathToAbsolutePath = expandPathMaps(relativePathToAbsolutePath)

      // importRoots are the roots of qualified names which will be trimmed from fully qualified names
      for (pair in expandedRelativePathToAbsolutePath) {
        val sourceImports =
          assembleSourceImportsFromImportPaths(importsPaths, pair.key, target.id.isMainWorkspace)
            .filter { it.nameCount > 0 }

        sourceImports.mapNotNull {
          it.toQualifiedName()
        }.forEach { qualName ->
          qualifiedNamesResolverMap[qualName] = pair.value
        }
      }
    }
    return qualifiedNamesResolverMap
  }

  /*
   * expandPathMaps will expand the map to include the parent paths
   * e.g. for an entry aaa/bbb/ccc.py -> /absolute/aaa/bbb/ccc.py,
   * this function will also add (aaa/bbb -> /absolute/aaa/bbb) and (aaa -> /absolute/aaa) into the new map,
   * so that intermediate directories are also tracked
   * */
  private fun expandPathMaps(originalMap: Map<Path, Path>): Map<Path, Path> {
    val newMap = originalMap.toMutableMap()
    for (entry in originalMap) {
      var relativePath = entry.key
      var absolutePath: Path = entry.value
      while (relativePath.nameCount > 1) {
        relativePath = relativePath.parent
        absolutePath = absolutePath.parent
        if (!newMap.containsKey(relativePath)) {
          newMap[relativePath] = absolutePath
        }
      }
    }
    return newMap
  }

  // assembleSourceImportsFromImportPaths returns all possible legal paths relative to the import roots of a full relative path
  private fun assembleSourceImportsFromImportPaths(importsPaths: List<Path>, sourceImport: Path?, isMainWorkspace: Boolean): List<Path> {
    if (null == sourceImport || sourceImport.nameCount == 0) {
      return emptyList()
    }

    val addedNames =
      importsPaths
        .filter { it != sourceImport && sourceImport.startsWith(it) }
        .map { sourceImport.subpath(it.nameCount, sourceImport.nameCount) }
    return if (isMainWorkspace) addedNames.plusElement(sourceImport) else addedNames
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

data class State(var index: Map<String, String> = mapOf())

override fun getState(): PythonResolveIndexService.State? =
  PythonResolveIndexService.State(
    internalResolveIndex
      .map {
        it.key.toString() to it.value.toString()
      }.toMap(),
  )

override fun loadState(state: PythonResolveIndexService.State) {
  internalResolveIndex =
    state.index
      .map { pair ->
        pair.key.split(".").let { QualifiedName.fromComponents(it) } to Path.of(pair.value)
      }.toMap()
  resolveIndex =
    internalResolveIndex.mapValues { (_, path) -> path.toVirtualFile() }
}
}

private fun Path.toQualifiedName(): QualifiedName? {
  val separator = FileSystems.getDefault().separator
  if (hasExtensionOf("py", "pyi")) return null

  val relativePath =
    toString()
      .let { StringUtil.trimEnd(it, separator + PyNames.INIT_DOT_PY) }
      .removeSuffix(".py")

  val relativePathParts = relativePath.split(separator)
  if (relativePathParts.any { it.contains(".") }) return null
  return QualifiedName.fromComponents(relativePathParts.flatMap { it.split(".") }.filter { it.isNotEmpty() })
}

private fun Path.toVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)
