package org.jetbrains.bazel.junit.playground;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.ArrayList;
import java.util.List;

public class ConsoleMain {
  public static void main(String[] args) {
    List<DiscoverySelector> selectors = new ArrayList<>();
    // If specific classes are passed via args, select them; otherwise default to Testsy
    if (args != null && args.length > 0) {
      for (String className : args) {
        try {
          Class<?> cls = Class.forName(className);
          selectors.add(DiscoverySelectors.selectClass(cls));
        } catch (ClassNotFoundException e) {
          System.err.println("[playground] Could not load class: " + className + ": " + e);
        }
      }
    }
    if (selectors.isEmpty()) {
      selectors.add(DiscoverySelectors.selectClass(Testsy.class));
    }

    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
      .selectors(selectors)
      .build();

    Launcher launcher = LauncherFactory.create();

    // Register a summary listener to determine process exit code based on test outcomes
    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(summary);

    launcher.execute(request);

    long failed = summary.getSummary().getTestsFailedCount();
    long aborted = summary.getSummary().getTestsAbortedCount();
    // Treat any failure or abort as non-zero exit for Bazel test target
    if (failed > 0 || aborted > 0) {
      System.exit(1);
    } else {
      System.exit(0);
    }
  }

  private static void tryRegisterTeamCityListener(Launcher launcher) {
    try {
      Class<?> listenerClass = Class.forName("org.jetbrains.bazel.junit5.TeamCityTestExecutionListener");
      Object instance = listenerClass.getDeclaredConstructor().newInstance();
      if (instance instanceof TestExecutionListener) {
        launcher.registerTestExecutionListeners((TestExecutionListener) instance);
      }
    } catch (Throwable ignored) {
      // If not available, rely on SPI auto-discovery controlled by the system property.
    }
  }
}
