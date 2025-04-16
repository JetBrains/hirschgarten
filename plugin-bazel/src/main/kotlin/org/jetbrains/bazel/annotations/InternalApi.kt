package org.jetbrains.bazel.annotations

/**
 * Indicates that the annotated class should NOT be used from plugins that depend on the Bazel plugin.
 * This is a separate class from ApiStatus.Internal because we need to have AnnotationRetention.RUNTIME retention for PublicApiCheckTest.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalApi
