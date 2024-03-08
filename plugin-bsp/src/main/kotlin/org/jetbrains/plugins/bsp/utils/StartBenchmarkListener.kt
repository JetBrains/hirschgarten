package org.jetbrains.plugins.bsp.utils

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.bsp.flow.open.BspProjectOpenProcessor
import org.jetbrains.plugins.bsp.magicmetamodel.impl.BenchmarkFlags
import kotlin.system.exitProcess

public class StartBenchmarkListener : ApplicationInitializedListener {
  override suspend fun execute(asyncScope: CoroutineScope) {
    BenchmarkFlags.benchmarkProjectPath()?.let { projectPath ->
      val vf = VfsUtil.findFileByIoFile(projectPath.toFile(), false)
      if (vf == null) {
        println("No project found at $projectPath")
        exitProcess(1)
      }
      ProjectManagerEx.getInstanceEx().openProjectAsync(
        projectPath,
        BspProjectOpenProcessor().calculateOpenProjectTask(projectPath, false, null, vf),
      )
      super.execute(asyncScope)
    }
  }
}
