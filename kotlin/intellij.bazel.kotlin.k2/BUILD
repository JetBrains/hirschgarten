load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.kotlin.k2` start

jvm_library(
    name = "bazel-kotlin-k2",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.k2",
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
        "@community//plugins/kotlin/refactorings/kotlin.refactorings.k2:kotlin-refactorings",
        "@community//platform/core-api:core",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@community//platform/util",
        "@community//platform/core-impl",
        "@community//java/java-psi-api:psi",
        "@community//platform/projectModel-api:projectModel",
        "//plugins/bazel/java/intellij.bazel.java.sync:bazel-java-sync",
    ],
)

jvm_library(
    name = "bazel-kotlin-k2_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.k2",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-kotlin-k2",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//plugins/kotlin/refactorings/kotlin.refactorings.k2:kotlin-refactorings_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@community//platform/util:util_test_lib",
        "@community//platform/core-impl:core-impl_test_lib",
        "@community//java/java-psi-api:psi_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "//plugins/bazel/java/intellij.bazel.java.sync:bazel-java-sync_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.kotlin.k2` end

### auto-generated section `iml intellij.bazel.kotlin.k2` start
exports_files(
    [
        "intellij.bazel.kotlin.k2.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.kotlin.k2` end
