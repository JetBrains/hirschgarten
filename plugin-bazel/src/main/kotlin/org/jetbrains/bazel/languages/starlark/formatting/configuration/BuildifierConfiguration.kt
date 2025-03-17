package org.jetbrains.bazel.languages.starlark.formatting.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.bazel.languages.starlark.formatting.BuildifierUtil

private const val BUILDIFIER_ID = "Buildifier"

@Service(Service.Level.PROJECT)
@State(name = BUILDIFIER_ID)
data class BuildifierConfiguration(var pathToExecutable: String?) : PersistentStateComponent<BuildifierConfiguration> {
  @Suppress("unused")
  constructor() : this(null) // Empty constructor required for state components

  override fun getState(): BuildifierConfiguration = this

  override fun loadState(state: BuildifierConfiguration) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    val options = listOf("--lint=fix")

    fun getBuildifierConfiguration(project: Project): BuildifierConfiguration {
      val settings = project.getService(BuildifierConfiguration::class.java)
      return settings
    }

    fun getBuildifierPath(project: Project): String? =
      getBuildifierConfiguration(project).pathToExecutable ?: BuildifierUtil.detectBuildifierExecutable()?.absolutePath
  }
}
