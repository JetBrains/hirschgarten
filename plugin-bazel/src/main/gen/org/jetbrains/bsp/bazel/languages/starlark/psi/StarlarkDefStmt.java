// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface StarlarkDefStmt extends PsiElement {

  @Nullable
  StarlarkParameters getParameters();

  @NotNull
  StarlarkSuite getSuite();

  @NotNull
  PsiElement getIdentifier();

}
