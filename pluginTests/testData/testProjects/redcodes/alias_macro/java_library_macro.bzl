load("@rules_java//java:java_library.bzl", "java_library")

def _java_library_macro_impl(name, visibility, srcs):
    impl_name = name + "_impl"
    java_library(
        name = impl_name,
        srcs = srcs,
        visibility = ["//visibility:private"],
        tags = ["manual"],
    )
    native.alias(
        name = name,
        actual = ":" + impl_name,
        visibility = visibility,
    )

java_library_macro = macro(
    attrs = {
        "srcs": attr.label_list(),
    },
    implementation = _java_library_macro_impl,
)
