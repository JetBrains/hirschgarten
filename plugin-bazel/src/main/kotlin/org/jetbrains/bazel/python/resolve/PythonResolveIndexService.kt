package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.components.Service
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
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.relativeTo

@Service(Service.Level.PROJECT)
class PythonResolveIndexService(private val project: Project) {
  // Todo: this index should be persisted after python generated source files are supported
  var resolveIndex: Map<QualifiedName, (PsiManager) -> PsiElement?> = emptyMap()
    private set

  fun updatePythonResolveIndex(rawTargets: List<RawBuildTarget>) {
    resolveIndex = buildIndex(rawTargets)
  }

  private fun buildIndex(rawTargets: List<RawBuildTarget>): Map<QualifiedName, (PsiManager) -> PsiElement?> {
    val executionRoot = BazelBinPathService.getInstance(project).bazelExecPath?.let { Path.of(it) } ?: return emptyMap()
    val rootDir = Path.of(project.rootDir.path)
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

    val qualifiedNamesResolverMap = mutableMapOf<QualifiedName, (PsiManager) -> PsiElement?>()

    for (target in targets) {
      val importsPaths = assembleImportsPaths(target)

      val fullQualifiedNameToAbsolutePath: Map<QualifiedName?, Path> =
        if (target.id.isMainWorkspace) {
          importsPaths
            .flatMap { importsPath ->
              allPYSourcesInMainWorkspace.filter { it.startsWith(importsPath) }
            }.toSet()
            .associate {
              it.toQualifiedName() to rootDir.resolve(it)
            }
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
                    // todo: if this is a generated python file #BAZEL-1758
                    it
                  }
                }

            executionRootRelativePath.toQualifiedName() to sourceItem.path
          }
        }

      // importRoots are the roots of qualified names which will be trimmed from fully qualified names
      val importRoots = importsPaths.map { path -> QualifiedName.fromComponents(path.map { it2 -> it2.toString() }) }

      for (pair in fullQualifiedNameToAbsolutePath) {
        val sourceImports =
          assembleSourceImportsFromImportRoots(importRoots, pair.key)
            .filter { it.componentCount > 0 }

        sourceImports.forEach { qualifiedName ->
          qualifiedNamesResolverMap.put(
            qualifiedName,
            { psiManager -> psiManager.findFile(pair.value.toVirtualFile() ?: return@put null) },
          )
        }
      }
    }
    return qualifiedNamesResolverMap
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
    val label = target.id
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

private fun Path.toVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)
