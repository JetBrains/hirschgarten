load("@aspect_rules_lint//format:defs.bzl", "format_multirun")
load("@rules_java//java:defs.bzl", "java_binary")

package(default_visibility = ["//visibility:public"])

alias(
    name = "buf",
    actual = select({
        "@platforms//os:windows": "@rules_buf_toolchains//:buf.exe",
        "//conditions:default": "@rules_buf_toolchains//:buf",
    }),
)

# shfmt and yamlfmt are not supported in rules_lint for now
# see https://github.com/aspect-build/rules_lint/issues/429

alias(
    name = "shfmt",
    actual = select({
        "@platforms//os:windows": None,
        "//conditions:default": "@aspect_rules_lint//format:shfmt",
    }),
)

alias(
    name = "yamlfmt",
    actual = select({
        "@platforms//os:windows": None,
        "//conditions:default": "@aspect_rules_lint//format:yamlfmt",
    }),
)

format_multirun(
    name = "format",
    java = ":java-format",
    kotlin = ":ktfmt",
    protocol_buffer = ":buf",
    shell = ":shfmt",
    starlark = "@buildifier_prebuilt//:buildifier",
    yaml = ":yamlfmt",
)

java_binary(
    name = "java-format",
    jvm_flags = [
        "--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    ],
    main_class = "com.google.googlejavaformat.java.Main",
    runtime_deps = ["@google_java_format//jar"],
)

java_binary(
    name = "ktfmt",
    args = ["--format"],
    main_class = "com.pinterest.ktlint.Main",
    runtime_deps = ["@ktlint//jar"],
)
