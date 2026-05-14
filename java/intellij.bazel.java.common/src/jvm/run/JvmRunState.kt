package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.runConfigRunWithBazel
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericRunState
import org.jetbrains.bazel.run.state.HasRunWithBazel
import org.jetbrains.bazel.run.state.addEnvironmentFragment
import org.jetbrains.bazel.run.state.addRunWithBazelFragment
import org.jetbrains.bazel.run.state.bazelParamsFragment
import org.jetbrains.bazel.run.state.programArgumentsFragment

@ApiStatus.Internal
class JvmRunState(project: Project) :
  AbstractGenericRunState<JvmRunState>(),
  HasDebugPort,
  HasRunWithBazel {
  @com.intellij.configurationStore.Property(description = "Debug port")
  @get:Attribute("debugPort")
  override var debugPort: Int by property(5005)

  @com.intellij.configurationStore.Property(description = "Run with Bazel")
  @get:Attribute("runWithBazel")
  override var runWithBazel: Boolean by property(project.projectView().runConfigRunWithBazel)

  override fun createFragments(configuration: BazelRunConfiguration): Collection<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      addDebugPortFragment()
      add(bazelParamsFragment())
      add(programArgumentsFragment())
      addEnvironmentFragment()
      addRunWithBazelFragment()
    }
}
