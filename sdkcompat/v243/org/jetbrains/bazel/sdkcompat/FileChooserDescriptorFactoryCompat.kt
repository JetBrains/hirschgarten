package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory

/**
 * The method [FileChooserDescriptorFactory.createSingleFileDescriptor] is deprecated from 251
 */
object FileChooserDescriptorFactoryCompat {
  fun createSingleFileDescriptor(): FileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
}
