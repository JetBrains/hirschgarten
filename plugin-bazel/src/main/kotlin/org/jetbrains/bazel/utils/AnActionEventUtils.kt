package org.jetbrains.bazel.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile

val AnActionEvent.selectedDirectory: VirtualFile?
  get() = getData(CommonDataKeys.VIRTUAL_FILE)
    ?.takeIf { it.isDirectory }
