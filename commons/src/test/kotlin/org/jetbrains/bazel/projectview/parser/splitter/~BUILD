load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "ProjectViewRawSectionsTest",
    size = "small",
    src = "ProjectViewRawSectionsTest.kt",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser/splitter",
    ],
)

kt_junit5_test(
    name = "ProjectViewSectionSplitterTest",
    size = "small",
    src = "ProjectViewSectionSplitterTest.kt",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser/splitter",
    ],
)
