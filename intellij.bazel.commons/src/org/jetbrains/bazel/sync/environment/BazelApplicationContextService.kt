package org.jetbrains.bazel.sync.environment

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelApplicationContextService {
  val disableFileWatcherSymlinkExclusion: Boolean
    get() = false
  val disableHardLinksOutputFiles: Boolean
    get() = false
  val forcedBazeliskDownload: BazeliskDownload
    get() = BazeliskDownload.Off
  val enablePty: Boolean
    get() = true
}
