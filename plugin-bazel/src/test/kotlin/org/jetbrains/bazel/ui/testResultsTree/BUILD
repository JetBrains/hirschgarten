load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "testResultsTree",
    src = "TestTargetActionResultsTreeTest.kt",
    classname = "org.jetbrains.bazel.ui.testResultsTree.TestTargetActionResultsTreeTest",
    ide_ids = [IDE_ID.IC],
    deps = [
        ":testTargetActionTreeUtil",
    ],
)

ide_starter_test(
    name = "runAllTestsActionTest",
    src = "RunAllTestsActionTest.kt",
    classname = "org.jetbrains.bazel.ui.testResultsTree.RunAllTestsActionTest",
    ide_ids = [IDE_ID.IC],
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
