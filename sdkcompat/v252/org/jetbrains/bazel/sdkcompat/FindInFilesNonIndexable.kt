package org.jetbrains.bazel.sdkcompat

import com.intellij.find.impl.FindInProjectUtil.FIND_IN_FILES_SEARCH_IN_NON_INDEXABLE
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

fun setFindInFilesNonIndexable(project: Project) {
  project.putUserData(FIND_IN_FILES_SEARCH_IN_NON_INDEXABLE, true)
  val nonIndexableFileNavigationContributor = Class.forName("com.intellij.ide.util.gotoByName.NonIndexableFileNavigationContributorKt")
  try {
    val keyGetter = nonIndexableFileNavigationContributor.getDeclaredMethod("getGOTO_FILE_SEARCH_IN_NON_INDEXABLE")
    val gotoFileSearchInNonIndexableKey = keyGetter.invoke(null) as Key<Boolean>
    project.putUserData(gotoFileSearchInNonIndexableKey, true)
  } catch (_: NoSuchMethodException) {
  }
}
