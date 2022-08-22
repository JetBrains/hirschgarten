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

public class StarlarkIfExpr2Impl extends ASTWrapperPsiElement implements StarlarkIfExpr2 {

  public StarlarkIfExpr2Impl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitIfExpr2(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkBinaryExpr getBinaryExpr() {
    return findChildByClass(StarlarkBinaryExpr.class);
  }

  @Override
  @Nullable
  public StarlarkLambdaExpr getLambdaExpr() {
    return findChildByClass(StarlarkLambdaExpr.class);
  }

  @Override
  @Nullable
  public StarlarkPrimaryExpr getPrimaryExpr() {
    return findChildByClass(StarlarkPrimaryExpr.class);
  }

  @Override
  @NotNull
  public List<StarlarkTest> getTestList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkTest.class);
  }

  @Override
  @Nullable
  public StarlarkUnaryExpr getUnaryExpr() {
    return findChildByClass(StarlarkUnaryExpr.class);
  }

}
