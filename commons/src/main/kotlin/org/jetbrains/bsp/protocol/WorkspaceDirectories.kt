package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class DirectoryItem(val uri: String)

@ApiStatus.Internal
data class WorkspaceDirectoriesResult(val includedDirectories: List<DirectoryItem>, val excludedDirectories: List<DirectoryItem>)
