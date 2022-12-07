// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkBinaryExpression extends StarlarkExpression {

  @NotNull
  List<StarlarkBinaryOperator> getBinaryOperatorList();

  @NotNull
  List<StarlarkExpression> getExpressionList();

}
