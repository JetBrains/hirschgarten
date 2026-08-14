package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId

/**
 * Allows customizing the behavior of [BepServer.handleEvent].
 */
interface BepEventHandlerProvider {
  companion object {
    @JvmStatic
    @ApiStatus.Internal
    val EP_NAME: ExtensionPointName<BepEventHandlerProvider> = ExtensionPointName.create("org.jetbrains.bazel.bepEventHandlerProvider")
  }

  fun create(context: BepEventHandlerContext): BepEventHandler
}

class BepEventHandlerContext(
  val project: Project,
  val parentId: TaskId,
  val taskEventsHandler: BazelTaskEventsHandler,
  val diagnosticsService: DiagnosticsService,
  val bazelPathsResolver: BazelPathsResolver,
)

interface BepEventHandler {
  /**
   * @return `true` if the event was handled and [handleEvent] shouldn't be called for other handlers or for [BepServer]
   */
  fun handleEvent(event: BuildEventStreamProtos.BuildEvent): Boolean
}
