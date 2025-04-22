package org.jetbrains.bazel.annotations

/**
 * Indicates that the annotated class can be used from plugins that depend on the Bazel plugin.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicApi
