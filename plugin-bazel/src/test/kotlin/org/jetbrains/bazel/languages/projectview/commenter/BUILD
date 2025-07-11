load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewCommenterTest",
    src = "ProjectViewCommenterTest.kt",
    data = ["//plugin-bazel/src/test/testData/projectview/commenter"],
    tags = ["exclusive"],
    deps = ["//plugin-bazel"],
)
