// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.jetbrains.bsp.bazel.languages.starlark.psi.*;

public class StarlarkPrimaryExpr1Impl extends ASTWrapperPsiElement implements StarlarkPrimaryExpr1 {

  public StarlarkPrimaryExpr1Impl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitPrimaryExpr1(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkCallSuffix getCallSuffix() {
    return findChildByClass(StarlarkCallSuffix.class);
  }

  @Override
  @Nullable
  public StarlarkDotSuffix getDotSuffix() {
    return findChildByClass(StarlarkDotSuffix.class);
  }

  @Override
  @Nullable
  public StarlarkSliceSuffix getSliceSuffix() {
    return findChildByClass(StarlarkSliceSuffix.class);
  }

}
