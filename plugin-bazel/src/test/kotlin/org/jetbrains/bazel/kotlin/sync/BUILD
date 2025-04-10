load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "KotlinFacetEntityUpdaterTest",
    src = "KotlinFacetEntityUpdaterTest.kt",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/kotlin/sync",
    ],
)
