package org.jetbrains.bazel.ui.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.BoundCompositeSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.buildifier.BuildifierUtil
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.bazelisk.BazeliskDownloader
import kotlin.io.path.Path
import kotlin.io.path.pathString

class BazelProjectSettingsConfigurable(private val project: Project) :
  BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
    displayName = BazelPluginBundle.message(DISPLAY_NAME_KEY),
    helpTopic = "",
  ),
  Configurable.WithEpDependencies {
  private val projectViewPathField: TextFieldWithBrowseButton
  private val buildifierExecutablePathField: TextFieldWithBrowseButton
  private val bazelExecutablePathField: TextFieldWithBrowseButton
  private val runBuildifierOnSaveCheckBox: JBCheckBox
  private val showExcludedDirectoriesAsSeparateNodeCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    projectViewPathField = initProjectViewFileField()
    buildifierExecutablePathField = initBuildifierExecutablePathField()
    bazelExecutablePathField = initBazelExecutablePathField()
    runBuildifierOnSaveCheckBox = initRunBuildifierOnSaveCheckBox()
    showExcludedDirectoriesAsSeparateNodeCheckBox = initShowExcludedDirectoriesAsSeparateNodeCheckBox()
  }

  override fun getDependencies(): List<BaseExtensionPointName<*>> = listOf(BazelGeneralSettingsProvider.ep)

  override fun createConfigurables(): List<UnnamedConfigurable> =
    project.bazelGeneralSettingsProviders.map {
      it.createConfigurable(project)
    }

  override fun createPanel(): DialogPanel =
    panel {
      row(BazelPluginBundle.message("project.settings.project.view.label")) { cell(projectViewPathField).align(Align.FILL) }
      row(BazelPluginBundle.message("project.settings.buildifier.label")) {
        cell(buildifierExecutablePathField).align(Align.FILL).validationInfo { buildifierExecutableValidationInfo() }
      }
      row("Bazel executable") {
        cell(bazelExecutablePathField).align(Align.FILL).validationInfo { bazelExecutableValidationInfo() }
        if (BazeliskDownloader.canDownload()) {
          button("Download Bazelisk") {
            BazelCoroutineService.getInstance(project).start {
              val path = BazeliskDownloader.downloadWithProgress(project)
              bazelExecutablePathField.text = path.toAbsolutePath().toString()
              currentProjectSettings = currentProjectSettings.withNewBazelExecutablePath(path)
            }
          }
        }
      }
      row { cell(runBuildifierOnSaveCheckBox).align(Align.FILL) }
      row { cell(showExcludedDirectoriesAsSeparateNodeCheckBox).align(Align.FILL) }

      // add settings from extensions
      configurables
        .sortedBy { c -> (c as? Configurable)?.displayName ?: "" }
        .forEach { appendDslConfigurable(it) }
    }

  private fun initProjectViewFileField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().apply {
      val title = BazelPluginBundle.message("text.field.project.settings.select.path.title")
      val description = BazelPluginBundle.message("text.field.project.settings.select.path.description")
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactory
          .singleFile()
          .withTitle(title)
          .withDescription(description),
      )
      whenTextChanged {
        if (text.isNotBlank()) {
          val newPath = Path(text)
          currentProjectSettings = currentProjectSettings.withNewProjectViewPath(newPath)
        }
      }
    }

  private fun initBuildifierExecutablePathField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().apply {
      val title = BazelPluginBundle.message("buildifier.select.path.to.executable")
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactory
          .singleFile()
          .withTitle(title),
      )
      whenTextChanged {
        if (text.isNotBlank()) {
          val newPath = Path(text)
          currentProjectSettings = currentProjectSettings.withNewBuildifierExecutablePath(newPath)
        }
      }
    }

  private fun buildifierExecutableValidationInfo(): ValidationInfo? =
    BuildifierUtil.validateBuildifierExecutable(
      buildifierExecutablePathField.text.takeIf { it.isNotBlank() } ?: BuildifierUtil.detectBuildifierExecutable()?.toString(),
    )

  private fun initBazelExecutablePathField(): TextFieldWithBrowseButton =
    TextFieldWithBrowseButton().apply {
      val title = "Select path to Bazel/Bazelisk executable"
      addBrowseFolderListener(
        project,
        FileChooserDescriptorFactory
          .singleFile()
          .withTitle(title),
      )
      whenTextChanged {
        if (text.isNotBlank()) {
          val newPath = Path(text)
          currentProjectSettings = currentProjectSettings.withNewBazelExecutablePath(newPath)
        }
      }
    }

  private fun bazelExecutableValidationInfo(): ValidationInfo? =
    BuildifierUtil.validateBuildifierExecutable(
      bazelExecutablePathField.text.takeIf { it.isNotBlank() } ?: currentProjectSettings.getBazelPath().toString(),
    )

  private fun initRunBuildifierOnSaveCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.run.buildifier.on.save.checkbox.text")).apply {
      isSelected = currentProjectSettings.runBuildifierOnSave
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(runBuildifierOnSave = isSelected)
      }
    }

  private fun initShowExcludedDirectoriesAsSeparateNodeCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text")).apply {
      isSelected = currentProjectSettings.showExcludedDirectoriesAsSeparateNode
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(showExcludedDirectoriesAsSeparateNode = isSelected)
      }
    }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    super<BoundCompositeSearchableConfigurable>.apply()
    val isProjectViewPathChanged = currentProjectSettings.projectViewPath != project.bazelProjectSettings.projectViewPath
    val showExcludedDirectoriesAsSeparateNodeChanged =
      currentProjectSettings.showExcludedDirectoriesAsSeparateNode != project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    project.bazelProjectSettings = currentProjectSettings

    if (isProjectViewPathChanged) {
      BazelCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
    if (showExcludedDirectoriesAsSeparateNodeChanged) {
      ProjectView.getInstance(project).refresh()
    }
  }

  override fun reset() {
    super<BoundCompositeSearchableConfigurable>.reset()
    projectViewPathField.text = savedProjectViewPath()
    buildifierExecutablePathField.text = getBuildifierExecPathPlaceholderMessage()
    bazelExecutablePathField.text = getBazelExecPathPlaceholderMessage()
    runBuildifierOnSaveCheckBox.isSelected = project.bazelProjectSettings.runBuildifierOnSave

    showExcludedDirectoriesAsSeparateNodeCheckBox.isSelected = project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun getBazelExecPathPlaceholderMessage(): String =
    currentProjectSettings.getBazelPath()?.toString()
      ?: "Bazel/Bazelisk executable not found on PATH"

  private fun getBuildifierExecPathPlaceholderMessage(): String =
    currentProjectSettings.getBuildifierPath()?.toString()
      ?: BazelPluginBundle.message("buildifier.executable.not.found", if (SystemInfo.isWindows) 0 else 1)

  private fun savedProjectViewPath() =
    project.bazelProjectSettings.projectViewPath
      ?.pathString
      .orEmpty()

  override fun getDisplayName(): String = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun getId(): String = ID

  override fun disposeUIResources() {
    projectViewPathField.dispose()
    super<BoundCompositeSearchableConfigurable>.disposeUIResources()
  }

  companion object {
    const val ID = "bazel.project.settings"
    const val DISPLAY_NAME_KEY = "project.settings.display.name"
  }

  object SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
    val keys =
      listOf(
        "project.settings.buildifier.label",
        "project.settings.project.view.label",
        "project.settings.plugin.show.excluded.directories.as.separate.node.checkbox.text",
        "project.settings.plugin.title",
        "project.settings.plugin.run.buildifier.on.save.checkbox.text",
      )
  }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
