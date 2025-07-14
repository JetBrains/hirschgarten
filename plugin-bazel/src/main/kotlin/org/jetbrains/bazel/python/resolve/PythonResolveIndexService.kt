package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PyNames
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo

@Service(Service.Level.PROJECT)
@State(name = "PythonResolveIndexService", storages = [Storage("bazelPython.xml")], reportStatistic = true)
class PythonResolveIndexService(private val project: Project) : PersistentStateComponent<PythonResolveIndexService.State> {
  var resolveIndex: Map<QualifiedName, (PsiManager) -> PsiElement?> = emptyMap()
    private set
  private var internalResolveIndex: Map<QualifiedName, Path> = mapOf()

  fun updatePythonResolveIndex(rawTargets: List<RawBuildTarget>) {
    internalResolveIndex = buildIndex(rawTargets)
    resolveIndex =
      internalResolveIndex.mapValues { (_, path) ->
        { psiManager ->
          psiManager.findFileOrDirectory(
            path.toVirtualFile() ?: return@mapValues null,
          )
        }
      }
  }

  private fun buildIndex(rawTargets: List<RawBuildTarget>): Map<QualifiedName, Path> {
    val executionRoot = BazelBinPathService.getInstance(project).bazelExecPath?.let { Path.of(it) } ?: return emptyMap()
    val rootDir = Path.of(project.rootDir.path)
    val bazelBin = BazelBinPathService.getInstance(project).bazelBinPath?.let { Path.of(it) } ?: return emptyMap()
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
        .filter { it.path.extension == "py" && it.path.startsWith(rootDir) }
        .map { it.path.relativeTo(rootDir) }

    val qualifiedNamesResolverMap = mutableMapOf<QualifiedName, Path>()

    for (target in targets) {
      val importsPaths = assembleImportsPaths(target)
      val pythonTargetInfo = extractPythonBuildTarget(target) ?: continue
      val fullQualifiedNameToAbsolutePath: Map<QualifiedName?, Path> =
        if (pythonTargetInfo.isCodeGenerator) {
          pythonTargetInfo.generatedSources
            .flatMap { file ->
              // some code gen rules return directories. we need to figure out what files are there
              if (file.isDirectory()) {
                Files
                  .walk(file)
                  .filter { it.isRegularFile() }
                  .map { it.toAbsolutePath() }
                  .toList()
              } else {
                listOf(file)
              }
            }.associateBy { path -> path.relativeTo(bazelBin).toQualifiedName() }
            .filter { it.key != null }
        } else if (target.id.isMainWorkspace) {
          importsPaths
            .flatMap { importsPath ->
              allPYSourcesInMainWorkspace.filter { it.startsWith(importsPath) }
            }.toSet()
            .associate {
              it.toQualifiedName() to rootDir.resolve(it)
            }.filter { it.key != null }
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

            executionRootRelativePath.toQualifiedName() to sourceItem.path
          }
        }
      val expandedFullQualifiedNameToAbsolutePath = expandFullQualifiedNameMaps(fullQualifiedNameToAbsolutePath)

      // importRoots are the roots of qualified names which will be trimmed from fully qualified names
      val importRoots = importsPaths.map { path -> QualifiedName.fromComponents(path.map { it2 -> it2.toString() }) }

      for (pair in expandedFullQualifiedNameToAbsolutePath) {
        val sourceImports =
          assembleSourceImportsFromImportRoots(importRoots, pair.key)
            .filter { it.componentCount > 0 }

        sourceImports.forEach { qualifiedName ->
          qualifiedNamesResolverMap.put(
            qualifiedName,
            pair.value,
          )
        }
      }
    }
    return qualifiedNamesResolverMap
  }

  /*
   * expandFullQualifiedNameMaps will expand the map to include the parent qualified names
   * e.g. for an entry aaa.bbb.ccc -> /aaa/bbb/ccc.py,
   * this function will also add (aaa.bbb->/aaa/bbb) and (aaa -> /aaa) into the new map,
   * so that pycharm won't show a red line under aaa
   * */
  private fun expandFullQualifiedNameMaps(originalMap: Map<QualifiedName?, Path>): Map<QualifiedName?, Path> {
    val newMap = originalMap.toMutableMap()
    for (entry in originalMap) {
      var qualifiedName = entry.key ?: continue
      var resolvedPath: Path = if (!entry.value.isDirectory()) entry.value.parent else entry.value
      while (qualifiedName.componentCount > 1) {
        qualifiedName = qualifiedName.removeLastComponent()
        resolvedPath = resolvedPath.parent
        if (!newMap.containsKey(qualifiedName)) {
          newMap[qualifiedName] = resolvedPath
        }
      }
    }
    return newMap
  }

  // assembleSourceImportsFromImportRoots returns all possible legal qualified names of a full qualified name
  private fun assembleSourceImportsFromImportRoots(importRoots: List<QualifiedName>, sourceImport: QualifiedName?): List<QualifiedName> {
    if (null == sourceImport || null == sourceImport.getLastComponent()) {
      return emptyList()
    }

    val addedNames =
      importRoots
        .filter { it != sourceImport && sourceImport.matchesPrefix(it) }
        .map { sourceImport.subQualifiedName(it.componentCount, sourceImport.componentCount) }
    return addedNames + sourceImport
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
      internalResolveIndex.mapValues { (_, path) -> { psiManager -> psiManager.findFile(path.toVirtualFile() ?: return@mapValues null) } }
  }
}

private fun Path.toQualifiedName(): QualifiedName? {
  val separator = FileSystems.getDefault().separator
  if (extension != "py") return null

  val relativePath =
    toString()
      .let { StringUtil.trimEnd(it, separator + PyNames.INIT_DOT_PY) }
      .removeSuffix(".py")
  return QualifiedName.fromComponents(
    relativePath.split(separator).flatMap { it.split(".") }.filter { it.isNotEmpty() },
  )
}

private fun PsiManager.findFileOrDirectory(vf: VirtualFile): PsiElement? =
  if (vf.isDirectory) {
    findDirectory(vf)
  } else {
    findFile(vf)
  }

private fun Path.toVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)
