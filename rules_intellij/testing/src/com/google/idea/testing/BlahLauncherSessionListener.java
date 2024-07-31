package com.google.idea.testing;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public class BlahLauncherSessionListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        System.out.println("BlahLauncherSessionListener: launcherSessionOpened");
    }


}
