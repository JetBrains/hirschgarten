package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadEdgesIndex
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.references.resolveFileTargetToVirtualFile
import org.jetbrains.bazel.languages.starlark.utils.GraphUtils

@ApiStatus.Internal
class StarlarkLoadCycleInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file as? StarlarkFile ?: return PsiElementVisitor.EMPTY_VISITOR
    if (!file.shouldCheckLoadCycles()) return PsiElementVisitor.EMPTY_VISITOR

    val sourceFile = file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
    val cyclicLoadLabels = StarlarkLoadGraph.getOrBuild(file.project).cyclicLoadLabels(sourceFile)

    return if (cyclicLoadLabels.isEmpty()) PsiElementVisitor.EMPTY_VISITOR else LoadCycleVisitor(holder, cyclicLoadLabels)
  }

  private fun StarlarkFile.shouldCheckLoadCycles(): Boolean =
    virtualFile != null &&
    name.endsWith(".bzl") &&
    PsiTreeUtil.findChildOfType(this, StarlarkLoadStatement::class.java) != null

  private class LoadCycleVisitor(private val holder: ProblemsHolder, private val cyclicLoadLabels: Set<String>) : StarlarkElementVisitor() {
    override fun visitLoadStatement(node: StarlarkLoadStatement) {
      val loadLabel = node.getLoadedFileNamePsi() ?: return
      if (loadLabel.getStringContents() !in cyclicLoadLabels) return

      holder.registerProblem(
        loadLabel,
        StarlarkBundle.message("inspection.description.load.cycle")
      )
    }
  }

  private class StarlarkLoadGraph private constructor(
    val resolvedLoadsByFile: Map<VirtualFile, Map<String, VirtualFile>>,
    val cyclicComponentByFile: Map<VirtualFile, Int>,
  ) {
    fun cyclicLoadLabels(sourceFile: VirtualFile): Set<String> {
      val sourceComponent = cyclicComponentByFile[sourceFile] ?: return emptySet()
      val resolvedLoads = resolvedLoadsByFile[sourceFile] ?: return emptySet()

      return resolvedLoads.filterValues { cyclicComponentByFile[it] == sourceComponent }.keys
    }

    companion object {
      fun getOrBuild(project: Project): StarlarkLoadGraph =
        CachedValuesManager.getManager(project).getCachedValue(project) {
          CachedValueProvider.Result.create(
            build(project),
            ModificationTracker { FileBasedIndex.getInstance().getIndexModificationStamp(StarlarkLoadEdgesIndex.NAME, project) },
            ProjectRootModificationTracker.getInstance(project),
            VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
          )
        }

      private fun build(project: Project): StarlarkLoadGraph {
        val resolvedLoadsByFile = collectResolvedLoadsByFile(project)
        val loadedFilesByFile = resolvedLoadsByFile.mapValues { (_, resolvedLoads) -> resolvedLoads.values.toSet() }
        val cyclicComponentByFile = findCyclicComponentByFile(loadedFilesByFile.allFiles(), loadedFilesByFile)

        return StarlarkLoadGraph(resolvedLoadsByFile, cyclicComponentByFile)
      }

      private fun Map<VirtualFile, Set<VirtualFile>>.allFiles(): Set<VirtualFile> = buildSet {
        addAll(keys)
        values.forEach { addAll(it) }
      }

      private fun collectResolvedLoadsByFile(project: Project): Map<VirtualFile, Map<String, VirtualFile>> {
        val index = FileBasedIndex.getInstance()
        val scope = GlobalSearchScope.projectScope(project)
        val resolvedLoadsByFile = linkedMapOf<VirtualFile, MutableMap<String, VirtualFile>>()

        index.processAllKeys(
          StarlarkLoadEdgesIndex.NAME,
          { sourceUrl ->
            val sourceFile = VirtualFileManager.getInstance().findFileByUrl(sourceUrl) ?: return@processAllKeys true
            if (!sourceFile.isValid) return@processAllKeys true
            if (!scope.contains(sourceFile)) return@processAllKeys true

            index.processValues(
              StarlarkLoadEdgesIndex.NAME,
              sourceUrl,
              null,
              { _, loadLabels ->
                for (loadLabel in loadLabels) {
                  val loadedFile = resolveLoadLabel(project, sourceFile, loadLabel) ?: continue
                  if (!loadedFile.isValid) continue
                  resolvedLoadsByFile.getOrPut(sourceFile, ::linkedMapOf).putIfAbsent(loadLabel, loadedFile)
                }
                true
              },
              scope,
            )

            true
          },
          project,
        )

        return resolvedLoadsByFile.mapValues { it.value.toMap() }
      }

      private fun resolveLoadLabel(project: Project, sourceFile: VirtualFile, loadLabel: String): VirtualFile? {
        val label = Label.parseOrNull(loadLabel) ?: return null
        return resolveFileTargetToVirtualFile(project, label, sourceFile)
      }

      private fun findCyclicComponentByFile(
        files: Collection<VirtualFile>,
        loadedFilesByFile: Map<VirtualFile, Set<VirtualFile>>
      ): Map<VirtualFile, Int> =
        GraphUtils.findCyclicStronglyConnectedComponents(files) { loadedFilesByFile[it].orEmpty() }
          .flatMapIndexed { index, component -> component.map { it to index } }
          .toMap()
    }
  }
}
