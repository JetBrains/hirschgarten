package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory

object FileChooserDescriptorFactoryCompat {
  fun createSingleFileDescriptor(): FileChooserDescriptor = FileChooserDescriptorFactory.singleFile()
}
