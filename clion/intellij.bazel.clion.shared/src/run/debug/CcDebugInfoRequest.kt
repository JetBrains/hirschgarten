package org.jetbrains.bazel.clion.shared.run.debug

import fleet.rpc.core.SendChannelSerializer
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

/**
 * Asks the frontend to show the debug info dialog. The backend computes all values.
 *
 * @param targetName the short label of the debugged target
 * @param targetCount the number of targets without debug info
 * @param targetsWithoutDbgCount the number of targets without debug info that Bazel built without `--compilation_mode=dbg`
 * @param details one line for each issue
 * @param expandDetails true if the details are the only hint, because `--compilation_mode=dbg` cannot fix the issues
 * @param projectViewFileName the name of the project view file that the dialog links to
 * @param canUseDbg true shows the option to set `--compilation_mode=dbg`
 * @param answer receives the choice of the user. The backend waits on it.
 */
@ApiStatus.Internal
@Serializable
data class CcDebugInfoRequest(
  val targetName: String,
  val targetCount: Int,
  val targetsWithoutDbgCount: Int,
  val details: List<String>,
  val expandDetails: Boolean,
  val projectViewFileName: String,
  val canUseDbg: Boolean,
  @Serializable(with = SendChannelSerializer::class) val answer: SendChannel<CcDebugInfoChoice>,
)
