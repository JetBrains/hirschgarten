package org.jetbrains.bazel.flow.open;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.OpenProjectTaskBuilder;
import com.intellij.ide.impl.OpenProjectTaskKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

final class OpenProjectTaskCompat {
  static OpenProjectTask build(Function1<? super OpenProjectTaskBuilder, Unit> buildAction) {
    return OpenProjectTaskKt.OpenProjectTask(buildAction);
  }
}
