package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.plugins.bsp.building.task.BspProjectTaskRunner

@Service(Service.Level.PROJECT)
@State(
  name = "BspProjectModuleBuildTasksTracker",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
internal class BspProjectModuleBuildTasksTracker : PersistentStateComponent<BspProjectModuleBuildTasksTracker> {
  var lastBuiltByJps: Boolean = false

  companion object {
    @JvmStatic
    fun initialize(workspace: BspWorkspace) {
      workspace.project.messageBus.connect().subscribe(ProjectTaskListener.TOPIC, object : ProjectTaskListener {
        override fun finished(result: ProjectTaskManager.Result) {
          // only concern successful task for updating info
          if (result.isModuleBuildTaskRunSuccessfully()) {
            getInstance(workspace.project).lastBuiltByJps = !result.isRunByBsp()
          }
        }
      })
    }

    /**
     * Assume that any tasks that satisfy BspProjectTaskRunner().canRun were run by BspProjectTaskRunner
     */
    private fun ProjectTaskManager.Result.isRunByBsp() =
      anyTaskMatches { task, _ -> BspProjectTaskRunner().canRun(task) }

    private fun ProjectTaskManager.Result.isModuleBuildTaskRunSuccessfully() =
      anyTaskMatches { task, state -> task is ModuleBuildTask && (!state.isFailed || !state.isSkipped) }

    @JvmStatic
    fun getInstance(project: Project): BspProjectModuleBuildTasksTracker =
      project.getService(BspProjectModuleBuildTasksTracker::class.java)
  }

  override fun getState(): BspProjectModuleBuildTasksTracker = this

  override fun loadState(state: BspProjectModuleBuildTasksTracker) {
    XmlSerializerUtil.copyBean(state, this)
  }
}
