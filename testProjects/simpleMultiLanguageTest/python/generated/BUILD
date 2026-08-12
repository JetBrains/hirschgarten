load("//python/rules:rules.bzl", "test_codegen_directory_py", "test_codegen_files_py")

test_codegen_directory_py(
    name = "generated_directory_lib",
    generate_with_shell_script = select({
        "@platforms//os:windows": False,
        "//conditions:default": True,
    }),
    visibility = ["//visibility:public"],
)

test_codegen_files_py(
    name = "generated_files_lib",
    visibility = ["//visibility:public"],
)
