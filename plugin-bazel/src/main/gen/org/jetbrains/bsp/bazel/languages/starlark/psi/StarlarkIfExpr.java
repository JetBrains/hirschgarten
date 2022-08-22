// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkIfExpr extends PsiElement {

  @NotNull
  StarlarkBinaryExpr2 getBinaryExpr2();

  @Nullable
  StarlarkIfExpr1 getIfExpr1();

  @NotNull
  List<StarlarkTest> getTestList();

}
