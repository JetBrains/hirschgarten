package org.jetbrains.bazel.languages.projectview.base

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import org.jetbrains.bazel.config.BazelProjectViewBundle
import javax.swing.Icon

internal object ProjectViewFileType : LanguageFileType(ProjectViewLanguage) {

  // TODO: whole icon loading for entire bazel plugin should be moved to dedicated module
  //  this works because intellij.bazel.projectview module is in the same classloader as core module
  val bazelIcon = IconLoader.getIcon("icons/bazel.svg", ProjectViewFileType::class.java.classLoader)

  override fun getName(): String = "ProjectView"

  override fun getDescription(): String = BazelProjectViewBundle.message("bazel.language.projectview.description")

  override fun getDefaultExtension(): String = "bazelproject"

  override fun getIcon(): Icon = bazelIcon
}
