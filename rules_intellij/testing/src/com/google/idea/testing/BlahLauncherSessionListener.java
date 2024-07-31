/*
 * This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
 * It was modified by JetBrains s.r.o. and contributors
 */
package com.google.idea.testing;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public class BlahLauncherSessionListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        System.out.println("BlahLauncherSessionListener: launcherSessionOpened");
    }


}
