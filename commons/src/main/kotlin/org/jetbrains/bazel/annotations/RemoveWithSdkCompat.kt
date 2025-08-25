package org.jetbrains.bazel.annotations

/**
 * Indicates that the class/method with this annotation should be removed once the corresponding sdkcompat version is removed.
 * @param version corresponding sdkcompat version, e.g., `v251`, `v252`
 */
@Retention(AnnotationRetention.RUNTIME)
@InternalApi
annotation class RemoveWithSdkCompat(val version: String)
