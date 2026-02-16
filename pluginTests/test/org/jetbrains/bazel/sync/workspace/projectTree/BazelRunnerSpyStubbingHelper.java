package org.jetbrains.bazel.sync.workspace.projectTree;

import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelProcess;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;

/**
 * Helper for stubbing {@link BazelRunner#runBazelCommand} on Mockito spies.
 * This is implemented in Java because Kotlin's null-safety and generics
 * do not play well with Mockito argument matchers for non-null parameters,
 * which easily leads to NPEs when used directly from Kotlin.
 */
public final class BazelRunnerSpyStubbingHelper {
  public static void stubRunBazelCommand(BazelRunner runner, BazelProcess process) {
    Mockito.doReturn(process).when(runner).runBazelCommand(
      any(BazelCommand.class),
      nullable(String.class),
      anyBoolean()
    );
  }
}