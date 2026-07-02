load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.kotlin.projectWizard` start

jvm_library(
    name = "bazel-kotlin-projectWizard",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.projectWizard",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/lang-api:lang",
        "@community//platform/lang-core",
        "@community//platform/core-api:core",
        "@community//platform/platform-impl:ide-impl",
        "@community//platform/platform-api:ide",
        "@community//platform/ide-core",
        "@community//platform/ide-core-impl",
        "@community//plugins/kotlin/project-wizard/core",
        "@community//plugins/kotlin/project-wizard/idea",
        "@community//java/idea-ui:ui",
    ],
)

jvm_library(
    name = "bazel-kotlin-projectWizard_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.projectWizard",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-kotlin-projectWizard",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/lang-core:lang-core_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-impl:ide-impl_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/ide-core-impl:ide-core-impl_test_lib",
        "@community//plugins/kotlin/project-wizard/core:core_test_lib",
        "@community//plugins/kotlin/project-wizard/idea:idea_test_lib",
        "@community//java/idea-ui:ui_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.kotlin.projectWizard` end

### auto-generated section `iml intellij.bazel.kotlin.projectWizard` start
exports_files(
    [
        "intellij.bazel.kotlin.projectWizard.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.kotlin.projectWizard` end
