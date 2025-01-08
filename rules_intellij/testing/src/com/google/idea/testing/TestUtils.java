/*
 * This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
 * It was modified by JetBrains s.r.o. and contributors
 *
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.testing;

import com.google.common.base.StandardSystemProperty;
import java.io.File;

/** Test utilities. */
public final class TestUtils {
  private TestUtils() {}

  /**
   * Gets directory name that should be used for all files created during testing.
   *
   * <p>This method will return a directory that's common to all tests run within the same <i>build
   * target</i>.
   *
   * @return standard absolute file name, for example "/tmp/zogjones/foo_unittest/".
   */
  public static String getTmpDir() {
    return getTmpDirFile().getAbsolutePath();
  }

  /**
   * Gets directory that should be used for all files created during testing.
   *
   * <p>This method will return a directory that's common to all tests run within the same <i>build
   * target</i>.
   *
   * @return standard file, for example the File representing "/tmp/zogjones/foo_unittest/".
   */
  public static File getTmpDirFile() {
    return TmpDirHolder.tmpDir;
  }

  private static final class TmpDirHolder {
    private static final File tmpDir = findTmpDir();

    private static File findTmpDir() {
      File tmpDir;

      // Flag value specified in environment?
      String tmpDirStr = getUserValue("TEST_TMPDIR");
      if ((tmpDirStr != null) && (tmpDirStr.length() > 0)) {
        tmpDir = new File(tmpDirStr);
      } else {
        // Fallback default $TEMP/$USER/tmp/$TESTNAME
        String baseTmpDir = StandardSystemProperty.JAVA_IO_TMPDIR.value();
        tmpDir = new File(baseTmpDir).getAbsoluteFile();

        // .. Add username
        String username = StandardSystemProperty.USER_NAME.value();
        username = username.replace('/', '_');
        username = username.replace('\\', '_');
        username = username.replace('\000', '_');
        tmpDir = new File(tmpDir, username);
        tmpDir = new File(tmpDir, "tmp");
      }

      // Ensure tmpDir exists
      if (!tmpDir.isDirectory()) {
        tmpDir.mkdirs();
      }
      return tmpDir;
    }
  }

  /**
   * Returns the value for system property <code>name</code>, or if that is not found the value of
   * the user's environment variable <code>name</code>. If neither is found, null is returned.
   *
   * @param name the name of property to get
   * @return the value of the property or null if it is not found
   */
  static String getUserValue(String name) {
    String propValue = System.getProperty(name);
    if (propValue == null) {
      return System.getenv(name);
    }
    return propValue;
  }
}
