load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "ide_starter_test")

ide_starter_test(
    name = "testResultsTree",
    size = "large",
    src = "TestTargetActionResultsTreeTest.kt",
    classname = "org.jetbrains.bazel.ui.testResultsTree.TestTargetActionResultsTreeTest",
    tags = ["manual"],
    deps = [
        ":testTargetActionTreeUtil",
    ],
)

ide_starter_test(
    name = "runAllTestsActionTest",
    size = "large",
    src = "RunAllTestsActionTest.kt",
    classname = "org.jetbrains.bazel.ui.testResultsTree.RunAllTestsActionTest",
    tags = ["manual"],
    deps = [
        ":testTargetActionTreeUtil",
    ],
)

kt_jvm_library(
    name = "testTargetActionTreeUtil",
    srcs = ["TestTargetTreeUtil.kt"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
