package org.jetbrains.bazel.sync.scope

import org.jetbrains.bazel.label.Label

/**
 * Scope of the sync. Multiple versions of sync are supported, including
 * - full project syncs (all targets are re-synced)
 * - partial syncs (only a subset of targets is re-synced).
 */
sealed interface ProjectSyncScope

/**
 * Represents all the syncs which are re-syncing the whole project, and all the targets should be refreshed.
 */
sealed interface FullProjectSync : ProjectSyncScope

/**
 * Represents the first phase of the phased sync - the quick sync after which the project is in the incomplete mode
 */
data object FirstPhaseSync : FullProjectSync

/**
 * Represents the second phase of the phased sync - the "heavy" sync after which the project is in its final form
 */
data object SecondPhaseSync : FullProjectSync

/**
 * Represents a partial project sync, which operates only on a limited subset of targets,
 * and only things related to these targets should be refreshed
 */
data class PartialProjectSync(val targetsToSync: List<Label>) : ProjectSyncScope
