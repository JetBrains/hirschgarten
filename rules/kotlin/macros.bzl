load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

def kt_library(name, deps = [], **kwargs):
    if not "srcs" in kwargs:
        kt_jvm_library(
            name = name,
            **kwargs
        )
    else:
        kt_jvm_library(
            name = name,
            deps = deps + [
                "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
            ],
            **kwargs
        )

def kt_binary(name, deps, **kwargs):
    kt_jvm_binary(
        name = name,
        deps = deps + [
            "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
        ],
        **kwargs
    )
