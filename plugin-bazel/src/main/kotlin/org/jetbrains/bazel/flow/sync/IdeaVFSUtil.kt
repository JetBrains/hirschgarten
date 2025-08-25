package org.jetbrains.bazel.flow.sync

import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import java.net.URI
import java.nio.file.Path

object IdeaVFSUtil {
  fun toVirtualFileUrl(uri: String, virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl =
    if (uri.startsWith("file://")) {
      /*
        Apparently on some operating systems(windows)
        VirtualFileUrlManager is unable to decode uri obtained from Path#toUri correctly
        on unix-like operating system uri path will look something like that:
          - file:///home/user/something - valid
        on windows path still contains root directory '/' slash
          - file:///C:/Users/user/something - valid
        VirtualFileUrlManager does not handle that and on windows returns path from root
           /C:/Users/user/something - invalid
       */
      virtualFileUrlManager.fromPath(Path.of(URI.create(uri)).toAbsolutePath().toString())
    } else {
      virtualFileUrlManager.getOrCreateFromUrl(uri)
    }
}
