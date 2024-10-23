package org.jetbrains.bsp.sdkcompat.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton

fun TextFieldWithBrowseButton.addBrowseFolderListenerCompat(
  title: String?,
  description: String?,
  project: Project?,
) = addBrowseFolderListener(
  project,
  FileChooserDescriptorFactory
    .createSingleFileDescriptor()
    .withTitle(
      title,
    ).withDescription(description),
)
