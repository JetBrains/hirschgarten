load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "starters",
    size = "large",
    src = "BazelNewProjectWizardTest.kt",
    classname = "org.jetbrains.bazel.hotswap.BazelNewProjectWizardTest",
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui",
    ],
)
