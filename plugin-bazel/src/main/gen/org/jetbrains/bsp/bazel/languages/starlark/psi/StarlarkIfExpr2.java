// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkIfExpr2 extends PsiElement {

  @Nullable
  StarlarkBinaryExpr getBinaryExpr();

  @Nullable
  StarlarkLambdaExpr getLambdaExpr();

  @Nullable
  StarlarkPrimaryExpr getPrimaryExpr();

  @NotNull
  List<StarlarkTest> getTestList();

  @Nullable
  StarlarkUnaryExpr getUnaryExpr();

}
