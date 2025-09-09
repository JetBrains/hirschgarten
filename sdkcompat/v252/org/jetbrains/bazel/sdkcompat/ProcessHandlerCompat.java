package org.jetbrains.bazel.sdkcompat;

import com.intellij.execution.process.ProcessHandler;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

public abstract class ProcessHandlerCompat extends ProcessHandler {
  protected abstract @Nullable CompletableFuture<@Nullable Long> getNativePidCompat();
}
