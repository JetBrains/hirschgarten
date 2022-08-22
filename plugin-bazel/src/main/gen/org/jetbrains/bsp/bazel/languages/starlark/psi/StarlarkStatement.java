// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkStatement extends PsiElement {

  @Nullable
  StarlarkDefStmt getDefStmt();

  @Nullable
  StarlarkForStmt getForStmt();

  @Nullable
  StarlarkIfStmt getIfStmt();

  @Nullable
  StarlarkSimpleStmt getSimpleStmt();

}
