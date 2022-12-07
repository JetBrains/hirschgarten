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

public class StarlarkOperandImpl extends ASTWrapperPsiElement implements StarlarkOperand {

  public StarlarkOperandImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitOperand(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkDictComp getDictComp() {
    return findChildByClass(StarlarkDictComp.class);
  }

  @Override
  @Nullable
  public StarlarkDictExpr getDictExpr() {
    return findChildByClass(StarlarkDictExpr.class);
  }

  @Override
  @Nullable
  public StarlarkExprStmt getExprStmt() {
    return findChildByClass(StarlarkExprStmt.class);
  }

  @Override
  @Nullable
  public StarlarkListComp getListComp() {
    return findChildByClass(StarlarkListComp.class);
  }

  @Override
  @Nullable
  public StarlarkListExpr getListExpr() {
    return findChildByClass(StarlarkListExpr.class);
  }

  @Override
  @Nullable
  public PsiElement getBytes() {
    return findChildByType(BYTES);
  }

  @Override
  @Nullable
  public PsiElement getFloat() {
    return findChildByType(FLOAT);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(IDENTIFIER);
  }

  @Override
  @Nullable
  public PsiElement getInt() {
    return findChildByType(INT);
  }

  @Override
  @Nullable
  public PsiElement getString() {
    return findChildByType(STRING);
  }

}
