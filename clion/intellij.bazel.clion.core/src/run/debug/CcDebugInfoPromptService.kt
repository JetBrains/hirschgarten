package org.jetbrains.bazel.clion.run.debug

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoApi
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<CcDebugInfoPromptService>()

private val FRONTEND_TIMEOUT = 5.seconds

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class CcDebugInfoPromptService {

  // without a buffer, a request of a canceled debug session does not stay in the channel
  private val channel = Channel<CcDebugInfoRequest>(Channel.RENDEZVOUS)

  /** Emits each request once. The frontend collects it through [CcDebugInfoApi.requests]. */
  val requests: Flow<CcDebugInfoRequest> = channel.receiveAsFlow()

  /**
   * Returns the answer of the user to the request that [createRequest] makes.
   * If no frontend takes the request within [FRONTEND_TIMEOUT], returns [CcDebugInfoChoice.DEBUG_ANYWAY].
   * If the frontend closes the answer without a choice, returns [CcDebugInfoChoice.CANCEL].
   */
  suspend fun ask(createRequest: (answer: SendChannel<CcDebugInfoChoice>) -> CcDebugInfoRequest): CcDebugInfoChoice {
    val answer = Channel<CcDebugInfoChoice>(capacity = 1)
    try {
      val request = createRequest(answer)
      // waits until a frontend takes the request, for example while the frontend connects or shows another dialog
      if (withTimeoutOrNull(FRONTEND_TIMEOUT) { channel.send(request) } == null) {
        LOG.warn("No frontend shows the debug info dialog, the debug session continues")
        return CcDebugInfoChoice.DEBUG_ANYWAY
      }

      return answer.receiveCatching().getOrNull() ?: CcDebugInfoChoice.CANCEL
    }
    finally {
      answer.cancel()
    }
  }
}

internal class CcDebugInfoApiImpl : CcDebugInfoApi {

  override suspend fun requests(projectId: ProjectId): Flow<CcDebugInfoRequest> {
    val project = projectId.findProjectOrNull() ?: return emptyFlow()
    return project.serviceAsync<CcDebugInfoPromptService>().requests
  }
}
