package org.jetbrains.jps.bazel;

import <error descr="Using type com.google.common.annotations.VisibleForTesting from an indirect dependency @maven//:com_google_guava_guava">com.google.common.annotations.VisibleForTesting</error>;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import <error descr="Using type com.google.errorprone.annotations.CanIgnoreReturnValue from an indirect dependency @maven//:com_google_errorprone_error_prone_annotations">com.google.errorprone.annotations.CanIgnoreReturnValue</error>;

public final class WorkRequestHandler {
  @<error descr="Using type com.google.common.annotations.VisibleForTesting from an indirect dependency @maven//:com_google_guava_guava">VisibleForTesting</error>
  void respondToRequest(WorkRequest request) {
  }

  public static class WorkRequestHandlerBuilder {
    @<error descr="Using type com.google.errorprone.annotations.CanIgnoreReturnValue from an indirect dependency @maven//:com_google_errorprone_error_prone_annotations">CanIgnoreReturnValue</error>
    public WorkRequestHandlerBuilder setCpuUsageBeforeGc() {
      return this;
    }
  }
}
