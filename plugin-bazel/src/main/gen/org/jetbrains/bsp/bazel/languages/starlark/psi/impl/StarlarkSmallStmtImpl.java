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

public class StarlarkSmallStmtImpl extends ASTWrapperPsiElement implements StarlarkSmallStmt {

  public StarlarkSmallStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitSmallStmt(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkAssignStmt getAssignStmt() {
    return findChildByClass(StarlarkAssignStmt.class);
  }

  @Override
  @Nullable
  public StarlarkBreakStmt getBreakStmt() {
    return findChildByClass(StarlarkBreakStmt.class);
  }

  @Override
  @Nullable
  public StarlarkContinueStmt getContinueStmt() {
    return findChildByClass(StarlarkContinueStmt.class);
  }

  @Override
  @Nullable
  public StarlarkExprStmt getExprStmt() {
    return findChildByClass(StarlarkExprStmt.class);
  }

  @Override
  @Nullable
  public StarlarkLoadStmt getLoadStmt() {
    return findChildByClass(StarlarkLoadStmt.class);
  }

  @Override
  @Nullable
  public StarlarkPassStmt getPassStmt() {
    return findChildByClass(StarlarkPassStmt.class);
  }

  @Override
  @Nullable
  public StarlarkReturnStmt getReturnStmt() {
    return findChildByClass(StarlarkReturnStmt.class);
  }

}
