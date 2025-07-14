package org.jetbrains.bazel.run.import

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jdom.Element
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.state.HasBazelParams
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import java.nio.file.Path

private const val GOOGLE_BAZEL_RUN_CONFIG_TYPE = "BlazeCommandRunConfigurationType"

internal class ImportRunConfigurationsSyncHook : ProjectSyncHook {
  private val log = logger<ImportRunConfigurationsSyncHook>()

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    environment.withSubtask("Import run configurations") {
      val project = environment.project
      val workspaceContext =
        query("workspace/context") {
          environment.server.workspaceContext()
        }

      val runManager = RunManager.getInstance(project)
      val shouldSetSelectedConfiguration = runManager.selectedConfiguration == null

      val runConfigurationPaths =
        workspaceContext.importRunConfigurations.values.mapNotNull { pathString ->
          project.rootDir.resolveFromRootOrRelative(pathString)
        }
      val runConfigurations =
        runConfigurationPaths.mapNotNull { runConfigurationPath ->
          try {
            importRunConfiguration(project, runConfigurationPath.toNioPath())
          } catch (e: Exception) {
            log.warn("Could not import $runConfigurationPath", e)
            null
          }
        }

      if (shouldSetSelectedConfiguration) {
        runManager.selectedConfiguration = runConfigurations.firstOrNull()
      }
    }
  }

  private fun importRunConfiguration(project: Project, runConfigurationPath: Path): RunnerAndConfigurationSettings? {
    val runConfigurationXml = getConfigurationElement(JDOMUtil.load(runConfigurationPath)) ?: return null
    val settings =
      if (checkNotNull(runConfigurationXml.getAttributeValue("type")) == GOOGLE_BAZEL_RUN_CONFIG_TYPE) {
        loadGoogleBazelRunConfiguration(project, runConfigurationXml)
      } else {
        loadRunConfigurationXmlNormally(project, runConfigurationXml)
      }
    return settings
  }

  private fun getConfigurationElement(runConfigurationXml: Element): Element? {
    var element: Element = runConfigurationXml
    if (element.name == "component" && element.getAttributeValue("name") == "ProjectRunConfigurationManager") {
      element = element.children.firstOrNull() ?: return null
    }
    return element.takeIf { it.name == "configuration" }
  }

  private fun loadGoogleBazelRunConfiguration(project: Project, runConfigurationXml: Element): RunnerAndConfigurationSettings {
    val name = checkNotNull(runConfigurationXml.getAttributeValue("name"))
    val blazeSettings: Element = checkNotNull(runConfigurationXml.getChild("blaze-settings"))
    val googleHandlerId: String = checkNotNull(blazeSettings.getAttributeValue("handler-id"))
    val bazelCommand: String = checkNotNull(blazeSettings.getAttributeValue("blaze-command"))
    val target = Label.parse(checkNotNull(blazeSettings.getChild("blaze-target")).text)
    val additionalBazelParams = blazeSettings.getChild("blaze-user-flag")?.text?.replaceProjectDir(project)
    val programArguments = blazeSettings.getChild("blaze-user-exe-flag")?.text?.replaceProjectDir(project)

    val envsMap =
      blazeSettings.getChild("env_state")?.getChild("envs")?.children.orEmpty().associate { env ->
        val name = checkNotNull(env.getAttributeValue("name"))
        val value = checkNotNull(env.getAttributeValue("value")).replaceProjectDir(project)
        name to value
      }

    val factory = runConfigurationType<BazelRunConfigurationType>().configurationFactories.first()
    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration(name, factory)
    val configuration = settings.configuration as BazelRunConfiguration
    val targets = listOf(target)
    val runHandler =
      GooglePluginAwareRunHandlerProvider.getRunHandlerProvider(googleHandlerId, bazelCommand)
        ?: RunHandlerProvider.getRunHandlerProvider(project, targets)
    configuration.updateTargets(targets, runHandler)
    val state = configuration.handler?.state
    (state as? HasBazelParams)?.additionalBazelParams = additionalBazelParams
    (state as? HasProgramArguments)?.programArguments = programArguments
    (state as? HasEnv)?.env?.set(EnvironmentVariablesData.create(envsMap, true))

    runManager.addConfiguration(settings)
    return settings
  }

  /**
   * Google's plugin sets the $PROJECT_DIR$ to <workspace_root>/.ijwb, while we instead use plain <workspace_root>.
   * That means that even if we just copied Google's run configurations, loading them directly would still not be possible.
   * Example: Google's plugin serializes the path to MODULE.bazel as $PROJECT_DIR$/../MODULE.bazel instead of $PROJECT_DIR$/MODULE.bazel
   */
  private fun String.replaceProjectDir(project: Project): String = replace("\$PROJECT_DIR$/..", project.rootDir.path)

  private fun loadRunConfigurationXmlNormally(project: Project, runConfigurationXml: Element): RunnerAndConfigurationSettings {
    val runManager = RunManagerImpl.getInstanceImpl(project)
    return runManager.loadConfiguration(runConfigurationXml, false)
  }
}
