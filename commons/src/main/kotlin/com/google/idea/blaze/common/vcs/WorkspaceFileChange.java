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

import java.nio.file.Path;

/**
 * Represents an edit to a file in the user's workspace.
 */
public record WorkspaceFileChange(Operation operation, Path workspaceRelativePath) {

  /**
   * Type of change that affected the file.
   */
  public enum Operation {
    DELETE,
    ADD,
    MODIFY,
  }

  /**
   * Invert this change. For an add, returns a corresponding delete, and for a delete returns a
   * corresponding add. For a modify, return this.
   *
   * <p>This is used when performing delta updates to correctly handle files that have been
   * reverted, i.e. are no longer in the working set.
   */
  public WorkspaceFileChange invert() {
    return switch (operation) {
      case DELETE -> new WorkspaceFileChange(Operation.ADD, workspaceRelativePath);
      case ADD -> new WorkspaceFileChange(Operation.DELETE, workspaceRelativePath);
      case MODIFY -> this;
    };
  }

  @Override
  public String toString() {
    return "WorkspaceFileChange{" + operation + ' ' + workspaceRelativePath + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WorkspaceFileChange that)) {
      return false;
    }
    return operation == that.operation && workspaceRelativePath.equals(that.workspaceRelativePath);
  }

}
