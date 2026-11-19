package org.jetbrains.bazel.sync.workspace.importer

import org.jetbrains.annotations.ApiStatus
import com.intellij.aspect.lib.OutputGroups
import org.jetbrains.bsp.protocol.OutputLocation

@ApiStatus.Internal
interface ToolOutputRequests {
  fun request(group: OutputGroups, files: List<OutputLocation>)
}

@ApiStatus.Internal
class ToolOutputRequestsCollector : ToolOutputRequests {

  private val outputGroupRequests: MutableSet<OutputGroups> = HashSet();
  private val outputRequests: MutableSet<OutputLocation> = HashSet();

  override fun request(group: OutputGroups, files: List<OutputLocation>) {
    outputGroupRequests.add(group)
    outputRequests.addAll(files)
  }

  fun getOutputGroupRequests(): Set<OutputGroups> {
    return outputGroupRequests
  }

  fun getOutputRequests(): Set<OutputLocation> {
    return outputRequests
  }

}
