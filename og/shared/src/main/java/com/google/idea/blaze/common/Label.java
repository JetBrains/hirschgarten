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
package com.google.idea.blaze.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents an absolute build target label.
 *
 * <p>This class is a simple wrapper around a string and should be used in place of {@link String}
 * whenever appropriate.
 *
 * <p>It can be considered equivalent to <a href="https://bazel.build/rules/lib/Label>Label</a> in
 * bazel.
 *
 * <p>Note that this class only supports labels in the current workspace, i.e. not labels of the
 * form {@code @repo//pkg/foo:abc}.
 */
public record Label(String workspace, String buildPackage, String name) {

  public static final String ROOT_WORKSPACE = "";

  public static Label of(String label) {
    Preconditions.checkArgument(!label.isBlank(), "Empty label");
    Preconditions.checkArgument(!label.contains("\\"), "Label contains backslashes: " + label);
    final var workspacePosition = label.startsWith("@") ? (label.startsWith("@@") ? 2 : 1) : 0;
    final var workspaceEnd = label.indexOf("//", workspacePosition);
    final var buildPackagePosition = workspaceEnd + 2;
    Preconditions.checkArgument(buildPackagePosition >= 2, "Invalid label: " + label);
    final var buildPackageEnd = label.indexOf(":", buildPackagePosition);
    final var namePosition = buildPackageEnd + 1;
    Preconditions.checkArgument(namePosition >= 1, "Invalid label: " + label);

    final var workspace = label.substring(workspacePosition, workspaceEnd);
    final var buildPackage = label.substring(buildPackagePosition, buildPackageEnd);
    final var name = label.substring(namePosition);
    return new Label(
        Interners.STRING.intern(workspace),
        Interners.STRING.intern(buildPackage),
        Interners.STRING.intern(name));
  }

  public static Label fromWorkspacePackageAndName(String workspace, Path packagePath, Path name) {
    String packageWithForwardSlashes = withForwardSlashes(packagePath);
    String nameWithForwardSlashes = withForwardSlashes(name);
    if (workspace.isEmpty()) {
      return Label.of(String.format("//%s:%s", packageWithForwardSlashes, nameWithForwardSlashes));
    } else {
      return Label.of(
          String.format(
              "@@%s//%s:%s", workspace, packageWithForwardSlashes, nameWithForwardSlashes));
    }
  }

  private static String withForwardSlashes(Path p) {
    StringBuilder res = new StringBuilder();
    p.iterator().forEachRemaining(part -> res.append(part).append("/"));
    // Remove the trailing slash
    return res.isEmpty() ? "" : res.substring(0, res.length() - 1);
  }

  public static Label fromWorkspacePackageAndName(String workspace, Path packagePath, String name) {
    return fromWorkspacePackageAndName(workspace, packagePath, Path.of(name));
  }

  public static ImmutableList<Label> toLabelList(List<String> labels) {
    return labels.stream().map(Label::of).collect(toImmutableList());
  }

  public Path getPackage() {
    return Path.of(buildPackage);
  }

  public Path getName() {
    return Path.of(name);
  }

  public String getWorkspaceName() {
    return workspace;
  }

  public Label siblingWithName(String name) {
    return fromWorkspacePackageAndName(getWorkspaceName(), getPackage(), name);
  }

  public Label siblingWithPathAndName(String pathAndName) {
    int colonPos = pathAndName.indexOf(':');
    Preconditions.checkArgument(colonPos > 0, pathAndName);
    return fromWorkspacePackageAndName(
        getWorkspaceName(),
        getPackage().resolve(pathAndName.substring(0, colonPos)),
        pathAndName.substring(colonPos + 1));
  }

  /** When this label refers to a source file, returns the workspace relative path to that file. */
  public Path toFilePath() {
    return getPackage().resolve(getName());
  }

  @Override
  public String toString() {
    final var result =
        new StringBuilder(5 + workspace.length() + buildPackage.length() + name.length());
    if (!workspace.isEmpty()) {
      result.append("@@");
      result.append(workspace);
    }
    result.append("//");
    result.append(buildPackage);
    result.append(":");
    result.append(name);
    return result.toString();
  }
}
