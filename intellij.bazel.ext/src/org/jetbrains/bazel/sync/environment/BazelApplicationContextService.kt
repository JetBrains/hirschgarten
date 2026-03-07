package org.jetbrains.bazel.sync.environment

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelApplicationContextService {
  val disableFileWatcherSymlinkExclusion: Boolean
    get() = false
  val forceBazeliskDownload: Boolean
    get() = false
}
