// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkOperand extends PsiElement {

  @Nullable
  StarlarkDictComp getDictComp();

  @Nullable
  StarlarkDictExpr getDictExpr();

  @Nullable
  StarlarkExprStmt getExprStmt();

  @Nullable
  StarlarkListComp getListComp();

  @Nullable
  StarlarkListExpr getListExpr();

  @Nullable
  PsiElement getBytes();

  @Nullable
  PsiElement getFloat();

  @Nullable
  PsiElement getIdentifier();

  @Nullable
  PsiElement getInt();

  @Nullable
  PsiElement getString();

}
