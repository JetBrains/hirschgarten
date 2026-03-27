package org.jetbrains.bazel.flow.open;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.OpenProjectTaskBuilder;
import com.intellij.ide.impl.OpenProjectTaskKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

// BAZEL-3057: don't call inline OpenProjectTask directly from Kotlin in order to avoid 2026.1 API breakage in inlined function
final class OpenProjectTaskCompat {
  static OpenProjectTask invoke(Function1<? super OpenProjectTaskBuilder, Unit> buildAction) {
    return OpenProjectTaskKt.OpenProjectTask(buildAction);
  }
}
