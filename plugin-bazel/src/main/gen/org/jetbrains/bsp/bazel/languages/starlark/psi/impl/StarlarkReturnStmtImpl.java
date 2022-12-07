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

public class StarlarkReturnStmtImpl extends StarlarkStmtImpl implements StarlarkReturnStmt {

  public StarlarkReturnStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull StarlarkVisitor visitor) {
    visitor.visitReturnStmt(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof StarlarkVisitor) accept((StarlarkVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public StarlarkExprStmt getExprStmt() {
    return findChildByClass(StarlarkExprStmt.class);
  }

}
