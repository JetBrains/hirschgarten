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

public class StarlarkBinaryExpr2Impl extends ASTWrapperPsiElement implements StarlarkBinaryExpr2 {

  public StarlarkBinaryExpr2Impl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitBinaryExpr2(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkBinaryExpr2 getBinaryExpr2() {
    return findChildByClass(StarlarkBinaryExpr2.class);
  }

  @Override
  @NotNull
  public StarlarkBinaryExpr21 getBinaryExpr21() {
    return findNotNullChildByClass(StarlarkBinaryExpr21.class);
  }

  @Override
  @Nullable
  public StarlarkBinop getBinop() {
    return findChildByClass(StarlarkBinop.class);
  }

}
