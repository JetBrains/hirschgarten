package org.jetbrains.bazel.clion.frontend.run.debug

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.project.projectId
import fleet.rpc.client.durable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoApi
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest

private val LOG = logger<CcDebugInfoFrontendService>()

@Service(Service.Level.PROJECT)
internal class CcDebugInfoFrontendService(private val project: Project, private val scope: CoroutineScope) {

  fun start() {
    scope.launch {
      durable {
        CcDebugInfoApi.getInstance().requests(project.projectId()).collect { request ->
          respond(request, ask(request))
        }
      }
    }
  }

  private suspend fun ask(request: CcDebugInfoRequest): CcDebugInfoChoice {
    return try {
      showCcDebugInfoDialog(project, request)
    }
    catch (e: Throwable) {
      rethrowControlFlowException(e)
      LOG.warn("Could not show the debug info dialog", e)
      CcDebugInfoChoice.DEBUG_ANYWAY
    }
  }

  private fun respond(request: CcDebugInfoRequest, choice: CcDebugInfoChoice) {
    if (request.answer.trySend(choice).isFailure) {
      // the backend closes the answer when the debug session ends, thus a failed send is no error
      LOG.info("The debug session ended before the answer to the debug info dialog")
    }
  }

  /** Required since the service is lazy. */
  class StartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
      project.serviceAsync<CcDebugInfoFrontendService>().start()
    }
  }
}
