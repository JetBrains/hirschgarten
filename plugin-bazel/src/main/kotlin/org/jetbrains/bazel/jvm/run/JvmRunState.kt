package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.runConfigRunWithBazel
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericRunState
import org.jetbrains.bazel.run.state.HasRunWithBazel
import org.jetbrains.bazel.run.state.addEnvironmentFragment
import org.jetbrains.bazel.run.state.addRunWithBazelFragment
import org.jetbrains.bazel.run.state.bazelParamsFragment
import org.jetbrains.bazel.run.state.programArgumentsFragment

class JvmRunState(project: Project) :
  AbstractGenericRunState<JvmRunState>(),
  HasDebugPort,
  HasRunWithBazel {
  @com.intellij.configurationStore.Property(description = "Debug port")
  @get:Attribute("debugPort")
  override var debugPort: Int by property(5005)

  @com.intellij.configurationStore.Property(description = "Run with Bazel")
  @get:Attribute("runWithBazel")
  override var runWithBazel: Boolean by property(ProjectViewService.getInstance(project).getCachedProjectView().runConfigRunWithBazel)

  override fun getEditor(configuration: BazelRunConfiguration): SettingsEditor<JvmRunState> = JvmRunStateEditor(configuration)
}

class JvmRunStateEditor(private val config: BazelRunConfiguration) :
  FragmentedSettingsEditor<JvmRunState>(config.handler?.state as JvmRunState) {
  override fun createFragments(): Collection<SettingsEditorFragment<JvmRunState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader(BazelPluginBundle.message("jvm.runner.header")))
      addDebugPortFragment()
      add(bazelParamsFragment())
      add(programArgumentsFragment())
      addEnvironmentFragment()
      addRunWithBazelFragment()
    }
}
