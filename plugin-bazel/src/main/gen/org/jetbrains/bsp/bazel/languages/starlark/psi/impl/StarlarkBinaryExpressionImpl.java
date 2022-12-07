// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes.*;
import org.jetbrains.bsp.bazel.languages.starlark.psi.*;

public class StarlarkBinaryExpressionImpl extends StarlarkExpressionImpl implements StarlarkBinaryExpression {

  public StarlarkBinaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitBinaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<StarlarkBinaryOperator> getBinaryOperatorList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkBinaryOperator.class);
  }

  @Override
  @NotNull
  public List<StarlarkExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkExpression.class);
  }

}
