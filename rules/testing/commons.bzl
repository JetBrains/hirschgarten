load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

KOTEST_DEPS = [
    "@maven//:io_kotest_kotest_assertions_api_jvm",
    "@maven//:io_kotest_kotest_assertions_core_jvm",
    "@maven//:io_kotest_kotest_assertions_shared_jvm",
    "@maven//:io_kotest_kotest_common_jvm",
]

def kt_test(name, src, classname = "", deps = [], **kwargs):
    """Just a simple wrapper around native kt_jvm_test rule. It sets up test class and delivers all kotest deps"""

    if type(src) != "string":
        fail("'src' has to be a string with file name!")

    if (classname == ""):
        classname = _guess_classname(name)

    kt_jvm_test(
        name = name,
        args = ["--select-class=" + classname, "--fail-if-no-tests"],
        srcs = [src],
        deps = KOTEST_DEPS + deps,
        **kwargs
    )

def _guess_classname(name):
    package = _guess_class_package()

    return package + "." + name

def _guess_class_package():
    package_name = native.package_name()
    package_name_without_last_slash = package_name.rstrip("/")
    _, _, package_name_without_org = package_name_without_last_slash.partition("/org/")
    real_package_name_with_slashes = "org/" + package_name_without_org
    package_name_with_dots = real_package_name_with_slashes.replace("/", ".")

    return package_name_with_dots
