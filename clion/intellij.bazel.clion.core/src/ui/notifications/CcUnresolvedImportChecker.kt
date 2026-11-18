package org.jetbrains.bazel.clion.ui.notifications

import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.preprocessor.OCUnresolvedIncludes
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.workspace.getCcIdentifier
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.generatedSources
import org.jetbrains.bsp.protocol.isGenerated
import org.jetbrains.bsp.protocol.relativeNioPath
import java.util.Collections

@ApiStatus.Internal
class CcUnresolvedImportChecker : UnresolvedImportChecker() {
  override fun isApplicable(project: Project, file: VirtualFile): Boolean =
    OCResolveConfigurations.getAllBuildConfigurationsOfTargetsOfFile(file, project).any { it.getCcIdentifier() != null }

  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val file = psiFile.virtualFile ?: return false
    if (!isApplicable(project, file)) return false
    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return false
    project.service<CcIncludeNotificationUpdates>().watch(file, document)
    if (OCUnresolvedIncludes.of(document).value.isNullOrEmpty()) return false
    return fileHasMissingGeneratedCode(project, file)
  }

  override fun generatedOutputs(target: BuildTarget): Sequence<OutputLocation> {
    val sources = target.generatedSources().filter {
      val name = it.relativeNioPath.fileName.toString()
      OCFileTypeHelpers.isSourceFile(name) || OCFileTypeHelpers.isHeaderFile(name)
    }
    val cc = target.findBuildData<CcBuildTarget>() ?: return sources
    val headers = sequenceOf(cc.ruleContext?.headers, cc.ruleContext?.textualHeaders, cc.compilationContext.headers)
      .filterNotNull()
      .flatMap { it.getOutputLocations() }
      .filter { it.isGenerated }
    return (sources + headers).distinct()
  }
}

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class CcIncludeNotificationUpdates(private val project: Project, private val coroutineScope: CoroutineScope) {
  private val watched = weakDocumentSet()

  // extracted to a function for clarity; weak set will not keep disposed entries
  private fun weakDocumentSet(): MutableSet<Document> =
    Collections.newSetFromMap(ConcurrentCollectionFactory.createConcurrentWeakMap<Document, Boolean>())

  fun watch(file: VirtualFile, document: Document) {
    if (!watched.add(document)) return
    val unresolvedIncludes = OCUnresolvedIncludes.of(document)
    coroutineScope.launch {
      unresolvedIncludes.collect { EditorNotifications.getInstance(project).updateNotifications(file) }
    }
  }
}
