package org.jetbrains.bazel.sdkcompat

import com.intellij.find.impl.FindInProjectUtil.FIND_IN_FILES_SEARCH_IN_NON_INDEXABLE
import com.intellij.openapi.project.Project

fun setFindInFilesNonIndexable(project: Project): Unit = project.putUserData(FIND_IN_FILES_SEARCH_IN_NON_INDEXABLE, true)
