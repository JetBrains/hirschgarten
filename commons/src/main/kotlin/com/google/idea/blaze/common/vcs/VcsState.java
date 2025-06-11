/*
 * Copyright 2023 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.common.vcs;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.common.vcs.WorkspaceFileChange.Operation;
import java.nio.file.Path;
import java.util.Optional;

/**
 * State of the projects VCS at a point in time.
 *
 * @param workspaceId           A unique ID for the workspace that this state derives from.
 *
 *                              <p>This is treated as an opaque string for equality testing only.
 * @param upstreamRevision      Upstream/base revision or CL number. This usually represents the last checked-in change that
 *                              the users workspace contains.
 *
 *                              <p>This is treated as an opaque string for equality testing only.
 * @param workingSet            The set of files in the workspace that differ compared to {@link #upstreamRevision()}.
 * @param workspaceSnapshotPath The readonly workspace snapshot path that this state derives from. If set, this can be used to
 *                              ensure atomic operations on the workspace by ensuring that a set of sequential operations are
 *                              all using the exact same revision of the workspace.
 */
public record VcsState(String workspaceId, String upstreamRevision, ImmutableSet<WorkspaceFileChange> workingSet,
                       Optional<Path> workspaceSnapshotPath) {

  @Override
  public String toString() {
    return "VcsState{upstreamRevision='" + upstreamRevision + "', workingSet=" + workingSet + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VcsState that)) {
      return false;
    }
    return workspaceId.equals(that.workspaceId)
      && upstreamRevision.equals(that.upstreamRevision)
      && workingSet.equals(that.workingSet)
      && workspaceSnapshotPath.equals(that.workspaceSnapshotPath);
  }

  /**
   * Returns workspace-relative paths of modified files (excluding deletions), according to the VCS
   */
  public ImmutableSet<Path> modifiedFiles() {
    return workingSet.stream()
      .filter(c -> c.operation() != Operation.DELETE)
      .map(WorkspaceFileChange::workspaceRelativePath)
      .collect(toImmutableSet());
  }
}
