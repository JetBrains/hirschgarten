package org.jetbrains.bazel.ui.settings

import com.intellij.execution.Platform
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
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.buildifier.BuildifierUtil
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class BazelProjectSettingsConfigurable(private val project: Project) :
  BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
    displayName = BazelPluginBundle.message(DISPLAY_NAME_KEY),
    helpTopic = "",
  ),
  Configurable.WithEpDependencies {
  private val projectViewPathField: TextFieldWithBrowseButton
  private val buildifierExecutablePathField: TextFieldWithBrowseButton
  private val runBuildifierOnSaveCheckBox: JBCheckBox
  private val showExcludedDirectoriesAsSeparateNodeCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    projectViewPathField = initProjectViewFileField()
    buildifierExecutablePathField = initBuildifierExecutablePathField()
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
    validateBuildifierExecutable(
      buildifierExecutablePathField.text.takeIf { it.isNotBlank() }
        ?: BuildifierUtil.detectBuildifierExecutable(),
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
    runBuildifierOnSaveCheckBox.isSelected = project.bazelProjectSettings.runBuildifierOnSave

    showExcludedDirectoriesAsSeparateNodeCheckBox.isSelected = project.bazelProjectSettings.showExcludedDirectoriesAsSeparateNode

    currentProjectSettings = project.bazelProjectSettings
  }

  private fun getBuildifierExecPathPlaceholderMessage(): String =
    currentProjectSettings.getBuildifierPathString()
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

  private fun validateBuildifierExecutable(executablePath: String?): ValidationInfo? {
    val message =
      when {
        executablePath.isNullOrEmpty() -> BazelPluginBundle.message("path.validation.field.empty")
        !isAbsolutePath(executablePath) -> BazelPluginBundle.message("path.validation.must.be.absolute")
        executablePath.endsWith(" ") -> BazelPluginBundle.message("path.validation.ends.with.whitespace")
        else ->
          try {
            val nioPath = Path("").resolve(executablePath)
            nioPath.getErrorMessage()
          } catch (e: InvalidPathException) {
            BazelPluginBundle.message("path.validation.invalid", e.message.orEmpty())
          } catch (e: IOException) {
            BazelPluginBundle.message("path.validation.inaccessible", e.message.orEmpty())
          }
      }
    return message?.let { ValidationInfo(it, null) }
  }

  private fun Path.getErrorMessage(): String? =
    when {
      this.isRegularFile() -> if (this.isExecutable()) null else BazelPluginBundle.message("path.validation.cannot.execute", this)
      this.isDirectory() -> BazelPluginBundle.message("path.validation.cannot.execute", this)
      else -> BazelPluginBundle.message("path.validation.file.not.found", this)
    }

  private fun isAbsolutePath(path: String): Boolean =
    when (Platform.current()) {
      Platform.UNIX -> path.startsWith("/")
      // On Windows user may create project in \\wsl
      Platform.WINDOWS -> OSAgnosticPathUtil.isAbsoluteDosPath(path) || path.startsWith("\\\\wsl")
    }
}

internal class BazelProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelProjectSettingsConfigurable(project)
}
