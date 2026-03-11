package org.jetbrains.bazel.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx
import java.nio.file.Path

@ApiStatus.Internal
interface BazelRepoMappingService {
  val apparentRepoNameToCanonicalName: Map<String, String>
  val canonicalRepoNameToApparentName: Map<String, String>
  val canonicalRepoNameToPath: Map<String, Path>
}

val Project.apparentRepoNameToCanonicalName: Map<String, String>
  @ApiStatus.Internal
  get() =
    service<BazelRepoMappingService>().apparentRepoNameToCanonicalName.takeIf { it.isNotEmpty() }
    ?: mapOf("" to "")

val Project.canonicalRepoNameToApparentName: Map<String, String>
  @ApiStatus.Internal
  get() =
    service<BazelRepoMappingService>().canonicalRepoNameToApparentName.takeIf { it.isNotEmpty() }
    ?: mapOf("" to "")

val Project.canonicalRepoNameToPath: Map<String, Path>
  @ApiStatus.Internal
  get() =
    service<BazelRepoMappingService>().canonicalRepoNameToPath.takeIf { it.isNotEmpty() }
    ?: mapOf("" to projectCtx.getProjectRootDirOrThrow().toNioPath())
