package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.writeText
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.jvm.run.CHECK_VISIBILITY_KEY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.targetUtils

const val GENERATE_SYNTHETIC_PROVIDER_NAME: String = "BazelGenerateSyntheticTargetRunTaskProvider"
val GENERATE_SYNTHETIC_PROVIDER_ID: Key<GenerateSyntheticTargetRunTaskProvider.Task> = Key.create(GENERATE_SYNTHETIC_PROVIDER_NAME)

val SYNTHETIC_BUILD_FILE_KEY: Key<VirtualFile> = Key.create("bazel.run.synthetic.build_file.vfile")

class GenerateSyntheticTargetRunTaskProvider(
) : BeforeRunTaskProvider<GenerateSyntheticTargetRunTaskProvider.Task>() {
  override fun getId(): Key<Task> = GENERATE_SYNTHETIC_PROVIDER_ID

  override fun getName(): String = GENERATE_SYNTHETIC_PROVIDER_NAME

  override fun createTask(runConfiguration: RunConfiguration): Task = Task()

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val project = environment.project
    val taskState = task.taskState
    val targetLabel = Label.parse(taskState.target)
    val target = project.targetUtils.getBuildTargetForLabel(targetLabel)
      ?: return false
    val language = Language.findLanguageByID(taskState.language) ?: return false
    val generator = SyntheticRunTargetTemplateGenerator.ep.allForLanguage(language)
      .find { it.isSupported(target) } ?: return false
    val template = generator.createSyntheticTemplate(target, taskState.params)
    val vFile = WriteAction.computeAndWait<VirtualFile, RuntimeException> {
      val syntheticTargetDir = project.rootDir.findOrCreateDirectory(".bazelbsp")
        .findOrCreateDirectory("synthetic_targets")
        .findOrCreateDirectory(template.buildFilePath)
      val vFile = syntheticTargetDir.findOrCreateFile("BUILD")
      vFile.writeText(template.buildFileContent)
      vFile
    }
    (configuration as BazelRunConfiguration).putUserData(SYNTHETIC_BUILD_FILE_KEY, vFile)

    if (BazelFeatureFlags.syntheticRunDisableVisibilityCheck) {
      (configuration as BazelRunConfiguration).putUserData(CHECK_VISIBILITY_KEY, false)
      environment.putUserData(CHECK_VISIBILITY_KEY, false)
    }

    return true
  }

  class Task : BeforeRunTask<Task>(GENERATE_SYNTHETIC_PROVIDER_ID),
               PersistentStateComponent<TaskState> {
    var taskState: TaskState = TaskState()
    override fun getState(): TaskState {
      return taskState
    }

    override fun loadState(state: TaskState) {
      this.taskState = state
    }
  }

  class TaskState {
    @Attribute("target")
    var target: String = ""

    @Attribute("language")
    var language: String = ""

    @Attribute("params")
    var params: String = ""
  }
}
