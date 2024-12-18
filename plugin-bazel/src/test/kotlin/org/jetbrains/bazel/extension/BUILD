load("@//rules/testing:intellij.bzl", "kt_intellij_junit5_test")

kt_intellij_junit5_test(
    name = "BazelBuildTargetClassifierTest",
    src = "BazelBuildTargetClassifierTest.kt",
    associates = ["//plugin-bazel/src:intellij-bazel"],
    deps = ["//plugin-bsp:intellij-bsp"],
)
