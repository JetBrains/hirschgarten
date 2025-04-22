load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelRunLineMarkerContributorTest",
    src = "BazelRunLineMarkerContributorTest.kt",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/java/ui/gutters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/kotlin/ui/gutters",
    ],
)
