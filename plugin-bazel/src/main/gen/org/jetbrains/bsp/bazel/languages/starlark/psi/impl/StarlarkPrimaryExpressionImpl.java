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

public class StarlarkPrimaryExpressionImpl extends StarlarkExpressionImpl implements StarlarkPrimaryExpression {

  public StarlarkPrimaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitPrimaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<StarlarkCallSuffix> getCallSuffixList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkCallSuffix.class);
  }

  @Override
  @NotNull
  public List<StarlarkDotSuffix> getDotSuffixList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkDotSuffix.class);
  }

  @Override
  @NotNull
  public StarlarkOperand getOperand() {
    return findNotNullChildByClass(StarlarkOperand.class);
  }

  @Override
  @NotNull
  public List<StarlarkSliceSuffix> getSliceSuffixList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, StarlarkSliceSuffix.class);
  }

}
