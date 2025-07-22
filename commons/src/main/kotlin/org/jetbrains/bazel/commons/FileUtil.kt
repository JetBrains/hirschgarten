package org.jetbrains.bazel.commons

import java.io.File

interface FileUtil {
  fun isAncestor(
    ancestor: String,
    file: String,
    strict: Boolean,
  ): Boolean

  fun isAncestor(
    ancestor: File,
    file: File,
    strict: Boolean,
  ): Boolean

  companion object {
    private lateinit var instance: FileUtil

    fun getInstance(): FileUtil = instance

    fun provideFileUtil(fileUtil: FileUtil) {
      instance = fileUtil
    }
  }
}
