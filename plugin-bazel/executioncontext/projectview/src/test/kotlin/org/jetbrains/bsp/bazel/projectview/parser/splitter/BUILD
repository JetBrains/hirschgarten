load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "ProjectViewRawSectionsTest",
    size = "small",
    src = "ProjectViewRawSectionsTest.kt",
    deps = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
    ],
)

kt_junit5_test(
    name = "ProjectViewSectionSplitterTest",
    size = "small",
    src = "ProjectViewSectionSplitterTest.kt",
    deps = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
    ],
)
