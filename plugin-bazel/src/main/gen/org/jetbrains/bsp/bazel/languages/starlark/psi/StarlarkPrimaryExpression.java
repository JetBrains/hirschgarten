// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkPrimaryExpression extends StarlarkExpression {

  @NotNull
  List<StarlarkCallSuffix> getCallSuffixList();

  @NotNull
  List<StarlarkDotSuffix> getDotSuffixList();

  @NotNull
  StarlarkOperand getOperand();

  @NotNull
  List<StarlarkSliceSuffix> getSliceSuffixList();

}
