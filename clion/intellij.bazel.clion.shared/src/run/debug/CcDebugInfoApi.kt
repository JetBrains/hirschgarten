package org.jetbrains.bazel.clion.shared.run.debug

import com.intellij.platform.project.ProjectId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Rpc
interface CcDebugInfoApi : RemoteApi<Unit> {

  /** Emits each debug info dialog that the project must show. The backend waits for [CcDebugInfoRequest.answer]. */
  suspend fun requests(projectId: ProjectId): Flow<CcDebugInfoRequest>

  companion object {
    suspend fun getInstance(): CcDebugInfoApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<CcDebugInfoApi>())
    }
  }
}
