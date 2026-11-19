package org.jetbrains.bazel.clion.ui.notifications

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurations
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.workspace.getCcIdentifier
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.ui.notifications.UnresolvedImportChecker
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.isGenerated
import org.jetbrains.bsp.protocol.relativeNioPath
import java.nio.file.Path

@ApiStatus.Internal
class CcUnresolvedImportChecker : UnresolvedImportChecker() {
  override fun owningTargets(project: Project, snapshot: WorkspaceSnapshot, path: Path): List<WorkspaceTargetKey> =
    (super.owningTargets(project, snapshot, path) + project.service<CcHeaderToTargetMap>().getTargets(snapshot, path)).distinct()

  override fun isApplicable(project: Project, file: VirtualFile): Boolean =
    OCResolveConfigurations.getAllBuildConfigurationsOfTargetsOfFile(file, project).any { it.getCcIdentifier() != null }

  override fun hasUnresolvedImport(project: Project, psiFile: PsiFile): Boolean {
    val file = psiFile.virtualFile ?: return false
    if (!isApplicable(project, file)) return false
    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return false
    val markup = DocumentMarkupModel.forDocument(document, project, true) as? MarkupModelEx ?: return false
    project.service<CcIncludeNotificationUpdates>().watch(file, markup)
    val errors = mutableListOf<HighlightInfo>()
    DaemonCodeAnalyzerEx.processHighlights(markup, project, HighlightSeverity.ERROR, 0, document.textLength) { info ->
      if (info.type.attributesKey == CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES) errors += info
      true
    }
    if (errors.isEmpty()) return false

    val lexer = LanguageParserDefinitions.INSTANCE.forLanguage(psiFile.language)?.createLexer(project) ?: return false
    lexer.start(document.charsSequence)
    var inInclude = false
    while (lexer.tokenType != null) {
      ProgressManager.checkCanceled()
      val text = lexer.tokenSequence
      if (text.startsWith("#")) {
        val directive = text.takeLastWhile { it.isLetter() || it == '_' }.toString()
        inInclude = directive in setOf("include", "include_next", "import")
      } else if (inInclude) {
        if (errors.any { it.actualStartOffset < lexer.tokenEnd && it.actualEndOffset > lexer.tokenStart }) {
          return fileHasMissingGeneratedCode(project, file)
        }
        if ('\n' in text && !text.startsWith("\\")) inInclude = false
      }
      lexer.advance()
    }
    return false
  }

  override fun generatedOutputs(target: BuildTarget): Sequence<OutputLocation> {
    val sources = super.generatedOutputs(target).filter {
      val name = it.relativeNioPath.fileName.toString()
      OCFileTypeHelpers.isSourceFile(name) || OCFileTypeHelpers.isHeaderFile(name)
    }
    val cc = target.findBuildData<CcBuildTarget>() ?: return sources
    val headers = cc.directHeaders().filter { it.isGenerated }
    return (sources + headers).distinct()
  }
}

private fun CcBuildTarget.directHeaders(): Sequence<OutputLocation> =
  sequenceOf(ruleContext?.headers, ruleContext?.textualHeaders, compilationContext.directHeaders)
    .filterNotNull()
    .flatMap { it.getOutputLocations() }

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class CcHeaderToTargetMap {
  private var snapshot: WorkspaceSnapshot? = null
  private var targetsByHeader: Map<Path, List<WorkspaceTargetKey>> = emptyMap()

  @Synchronized
  fun getTargets(snapshot: WorkspaceSnapshot, path: Path): List<WorkspaceTargetKey> {
    if (this.snapshot !== snapshot) {
      val resolver = DefaultOutputLocationResolver.createLocalWorkspaceOnly(snapshot.bazelInfo, snapshot.repoMapping)
      val repositories = snapshot.repoMapping.getLocalRepositories()
      val targetsByHeader = HashMap<Path, MutableSet<WorkspaceTargetKey>>()
      for (info in snapshot.targets.allTargetInfos()) {
        ProgressManager.checkCanceled()
        if (CC_LANGUAGE_CLASS !in info.kind.languageClasses) continue
        val cc = snapshot.targets.findTargetData(info.key, CcBuildTarget::class.java) ?: continue
        for (header in cc.directHeaders()) {
          val headerPath = resolver.resolve(header, repositories) ?: continue
          targetsByHeader.getOrPut(headerPath) { LinkedHashSet() }.add(info.key)
        }
      }
      this.targetsByHeader = targetsByHeader.mapValues { it.value.toList() }
      this.snapshot = snapshot
    }
    return targetsByHeader[path].orEmpty()
  }
}

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class CcIncludeNotificationUpdates(private val project: Project) : Disposable {
  fun watch(file: VirtualFile, markup: MarkupModelEx) {
    if (markup !is UserDataHolderEx || !markup.replace(watched, null, true)) return
    markup.addMarkupModelListener(this, object : MarkupModelListener {
      override fun afterAdded(highlighter: RangeHighlighterEx) = update(highlighter)

      override fun afterRemoved(highlighter: RangeHighlighterEx) = update(highlighter)

      override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
        EditorNotifications.getInstance(project).updateNotifications(file)
      }

      private fun update(highlighter: RangeHighlighterEx) {
        if (highlighter.textAttributesKey == CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES) {
          EditorNotifications.getInstance(project).updateNotifications(file)
        }
      }
    })
  }

  override fun dispose() {}

  companion object {
    private val watched = Key.create<Boolean>("bazel.cc.include.notifications")
  }
}
