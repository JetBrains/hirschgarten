/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package com.google.idea.testing.runfiles;

import com.intellij.openapi.util.text.StringUtil;
import java.nio.file.Path;
import java.nio.file.Paths;

/** A utlility class that knows how to locate Bazel's runfiles root directory. */
public class Runfiles {

  private Runfiles() {}

  /** Returns the runtime location of a data dependency. */
  public static Path runfilesPath(String relativePath) {
    String testSrcdir = getUserValue("TEST_SRCDIR");
    String testWorkspace = getUserValue("TEST_WORKSPACE");

    // Hack to make tests run under JPS
    if (testSrcdir == null || testWorkspace == null) {
      relativePath = StringUtil.trimStart(relativePath, "/");
      Path currentDir = Paths.get("").toAbsolutePath();
      // in JPS this will be
      while (!currentDir.endsWith("bazel")) {
        currentDir = currentDir.getParent();
      }
      return currentDir.resolve(relativePath);
    }

    return Path.of(testSrcdir, testWorkspace, relativePath);
  }

  /** Returns the runtime location of data dependencies. */
  public static Path runfilesPath() {
    return runfilesPath("");
  }

  private static String getUserValue(String name) {
    String propValue = System.getProperty(name);
    if (propValue == null) {
      return System.getenv(name);
    }
    return propValue;
  }
}
