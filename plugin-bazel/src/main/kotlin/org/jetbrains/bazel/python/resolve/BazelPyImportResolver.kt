package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.impl.PyImportResolver
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext
import okio.Path.Companion.toPath
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.io.File
import java.nio.file.Path


class BazelPyImportResolver : PyImportResolver {
  override fun resolveImportReference(
    name: QualifiedName,
    context: PyQualifiedNameResolveContext,
    withRoots: Boolean,
  ): PsiElement? {
    if (!context.project.isBazelProject) return null

    val rootImportResolve = resolveRootImport(name, context)
    if (rootImportResolve != null) return rootImportResolve

    return resolveShortImport(name, context)
  }


  /**
   * Root import is an absolute import, whose qualified name aligns with the project file structure.
   * For example `import aaa.bbb.ccc` that references `aaa/bbb/ccc.py` file or `aaa/bbb/ccc` directory.
   */
  private fun resolveRootImport(name: QualifiedName, context: PyQualifiedNameResolveContext): PsiElement? {
    // VirtualFile.findFileByRelativePath always expects a slash ("/"), even on Windows
    val relativeToRoot = name.components.joinToString("/")
    return ReadAction.compute<PsiElement?, Throwable> {
      findDirectoryOrPythonFile(relativeToRoot, context)
    }
  }

  /**
   * In rare cases, both a directory and a file match the import.
   *
   * Precedence, as per [PEP-420](https://peps.python.org/pep-0420/#specification):
   *
   * "While looking for a module or package named “foo”, for each directory in the parent path:
   *
   * 1. If `<directory>/foo/__init__.py` is found, a regular package is imported and returned.
   *
   * 2. If not, but `<directory>/foo.{py,pyc,so,pyd}` is found, a module is imported and returned (...)"
   *
   * 3. If not, but `<directory>/foo` is found and is a directory,
   * it is recorded and the scan continues with the next directory in the parent path.
   *
   * 4. Otherwise the scan continues with the next directory in the parent path.
   *
   * If the scan completes without returning a module or package,
   * and at least one directory was recorded, then a namespace package is created."
   *
   * The paragraph above mentions looking "for each directory",
   * but this function is only responsible for looking in one (the project root).
   */
  private fun findDirectoryOrPythonFile(pathRelativeToRoot: String, context: PyQualifiedNameResolveContext): PsiElement? {
    val rootDir = context.project.rootDir
    val psiManager = context.psiManager
    val directory = rootDir.findFileByRelativePath(pathRelativeToRoot)?.directoryOrNull()

    // case 1
    if (directory?.findChild("__init__.py") != null) {
      return psiManager.findDirectory(directory) // case 1
    }

    // case 2
    val file = rootDir.findFileByRelativePath("$pathRelativeToRoot.py")?.fileOrNull()
    if (file != null) return psiManager.findFile(file)

    // case 3
    // TODO (BAZEL-1998) - verify if simply returning the namespace package directory is a good approach
    if (directory != null) {
      return psiManager.findDirectory(directory)
    }

    // case 4
    return null
  }


  private fun resolveShortImport(name: QualifiedName, context: PyQualifiedNameResolveContext): PsiElement? {
    val sourcesIndex = buildSourcesIndex(context.project)
    val resolvedName = sourcesIndex[name] ?: return null
    return resolvedName(context.psiManager)
  }


  private fun buildSourcesIndex(project: Project): Map<QualifiedName, (PsiManager) -> PsiElement?> {
    val targets = project.targetUtils.labelToTargetInfo.filter { it.value.kind.languageClasses.contains(LanguageClass.PYTHON) }
    val shortenedNamesResolverMap = mutableMapOf<QualifiedName, (PsiManager) -> PsiElement?>()

    targets.forEach { it ->
      // imported paths relative to the execution roots
      val importedPaths = assembleImportsPaths(project, it.value)
      val importedTargets = importedPaths.map { Path.of(project.rootDir.path).resolve(it).toChildTargets(project) }.flatten()
      val sources = importedTargets.flatMap { it.sources }.toSet()

      // importRoots are the roots of qualified names which will be trimmed from fully qualified names
      val importRoots = importedPaths.map { QualifiedName.fromComponents(it.map { it2 -> it2.toString() }) }
      for (source in sources) {
        val sourceImports =
          assembleSourceImportsFromImportRoots(importRoots, source.path.toQualifiedName(project))
            .filter { it2 -> it2.componentCount > 0 }

        sourceImports.forEach { qualifiedName ->
          shortenedNamesResolverMap.put(
            qualifiedName,
            { psiManager -> psiManager.findFile(source.path.toVirtualFile() ?: return@put null) },
          )
        }
      }

    }
    return shortenedNamesResolverMap
  }

  //assembleSourceImportsFromImportRoots returns all possible legal shortened qualified names of a qualified name
  private fun assembleSourceImportsFromImportRoots(
    importRoots: List<QualifiedName>,
    sourceImport: QualifiedName?,
  ): List<QualifiedName> {
    if (null == sourceImport || null == sourceImport.getLastComponent()) {
      return emptyList()
    }

    val addedNames = importRoots
      .filter { it != sourceImport && sourceImport.matchesPrefix(it) }
      .map { sourceImport.subQualifiedName(it.componentCount, sourceImport.componentCount) }
    return addedNames + sourceImport
  }


  // assembleImportRoots convert "imports" attributes of a bazel python rule to actual imported paths
  private fun assembleImportsPaths(project: Project, target: BuildTarget): List<Path> {
    val label = target.id as? ResolvedLabel ?: return listOf()
    val ideInfo = extractPythonBuildTarget(target) ?: return listOf()
    var buildParentPath = Path.of(label.packagePath.pathSegments.joinToString("/"))

    // In the case of an external repo the build path could be `/BUILD.bazel`
    // which has a basedir of `/`. In this case we translate this to `.` so
    // that it works in the sub file-system.
    if (null == buildParentPath || 0 == buildParentPath.nameCount) {
      buildParentPath = Path.of(".")
    }
    return ideInfo.imports.map {
      buildParentPath.resolve(it).normalize()
    }

  }


}

private fun Path.toChildTargets(project: Project): List<BuildTarget> =
  project.targetUtils
    .allBuildTargets()
    .filter { it.baseDirectory.startsWith(this) && !it.tags.contains(BuildTargetTag.MANUAL) }

private fun Path.toQualifiedName(project: Project): QualifiedName? {
  if (!toString().endsWith(".py")) {
    return null
  }
  val relativePath = toString()
    .let { StringUtil.trimEnd(it, File.separator + PyNames.INIT_DOT_PY) }
    .removeSuffix(".py").toPath().relativeTo(project.rootDir.path.toPath())
    .toString()
  return QualifiedName.fromComponents(StringUtil.split(relativePath, File.separator))
}

private fun Path.toVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)

private fun VirtualFile.fileOrNull(): VirtualFile? = if (!isDirectory) this else null

private fun VirtualFile.directoryOrNull(): VirtualFile? = if (isDirectory) this else null
