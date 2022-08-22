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

public class StarlarkStatementImpl extends ASTWrapperPsiElement implements StarlarkStatement {

  public StarlarkStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkDefStmt getDefStmt() {
    return findChildByClass(StarlarkDefStmt.class);
  }

  @Override
  @Nullable
  public StarlarkForStmt getForStmt() {
    return findChildByClass(StarlarkForStmt.class);
  }

  @Override
  @Nullable
  public StarlarkIfStmt getIfStmt() {
    return findChildByClass(StarlarkIfStmt.class);
  }

  @Override
  @Nullable
  public StarlarkSimpleStmt getSimpleStmt() {
    return findChildByClass(StarlarkSimpleStmt.class);
  }

}
