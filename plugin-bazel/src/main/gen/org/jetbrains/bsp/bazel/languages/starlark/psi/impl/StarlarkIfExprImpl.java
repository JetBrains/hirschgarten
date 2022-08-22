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

public class StarlarkIfExprImpl extends ASTWrapperPsiElement implements StarlarkIfExpr {

  public StarlarkIfExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitIfExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public StarlarkBinaryExpr2 getBinaryExpr2() {
    return findNotNullChildByClass(StarlarkBinaryExpr2.class);
  }

  @Override
  @Nullable
  public StarlarkIfExpr1 getIfExpr1() {
    return findChildByClass(StarlarkIfExpr1.class);
  }

  @Override
  @NotNull
  public List<StarlarkTest> getTestList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkTest.class);
  }

}
