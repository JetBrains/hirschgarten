#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
"""Convenience methods for plugin_api."""

load("@rules_java//java:defs.bzl", "java_import")

# The current indirect ij_product mapping (eg. "intellij-latest")
INDIRECT_IJ_PRODUCTS = {
    "intellij-latest": "intellij-2024.2",
}

(CHANNEL_STABLE, CHANNEL_BETA, CHANNEL_CANARY, CHANNEL_FREEFORM) = ("stable", "beta", "canary", "freeform")

DIRECT_IJ_PRODUCTS = {
    "intellij-2024.2": struct(
        ide = "intellij",
        directory = "intellij_ce_2024_2",
    ),
    "intellij-2024.3": struct(
        ide = "intellij",
        directory = "intellij_ue_2024_3",
    ),
}

def select_for_plugin_api(params):
    """Selects for a plugin_api.

    Args:
        params: A dict with ij_product -> value.
                You may only include direct ij_products here,
                not indirects (eg. intellij-latest).
    Returns:
        A select statement on all plugin_apis. Unless you include a "default",
        a non-matched plugin_api will result in an error.

    Example:
      java_library(
        name = "foo",
        srcs = select_for_plugin_api({
            "intellij-2016.3.1": [...my intellij 2016.3 sources ....],
            "intellij-2012.2.4": [...my intellij 2016.2 sources ...],
        }),
      )
    """
    for indirect_ij_product in INDIRECT_IJ_PRODUCTS:
        if indirect_ij_product in params:
            error_message = "".join([
                "Do not select on indirect ij_product %s. " % indirect_ij_product,
                "Instead, select on an exact ij_product.",
            ])
            fail(error_message)
    return _do_select_for_plugin_api(params)

def _do_select_for_plugin_api(params):
    """A version of select_for_plugin_api which accepts indirect products."""
    if not params:
        fail("Empty select_for_plugin_api")

    expanded_params = dict(**params)

    # Expand all indirect plugin_apis to point to their
    # corresponding direct plugin_api.
    #
    # {"intellij-2016.3.1": "foo"} ->
    # {"intellij-2016.3.1": "foo", "intellij-latest": "foo"}
    fallback_value = None
    for indirect_ij_product, resolved_plugin_api in INDIRECT_IJ_PRODUCTS.items():
        if resolved_plugin_api in params:
            expanded_params[indirect_ij_product] = params[resolved_plugin_api]
            if not fallback_value:
                fallback_value = params[resolved_plugin_api]
        if indirect_ij_product in params:
            expanded_params[resolved_plugin_api] = params[indirect_ij_product]

    # Map the shorthand ij_products to full config_setting targets.
    # This makes it more convenient so the user doesn't have to
    # fully specify the path to the plugin_apis
    select_params = dict()
    for ij_product, value in expanded_params.items():
        if ij_product == "default":
            select_params["//conditions:default"] = value
        else:
            select_params["@rules_intellij//intellij_platform_sdk:" + ij_product] = value

    return select(
        select_params,
        no_match_error = "define an intellij product version, e.g. --define=ij_product=intellij-latest",
    )

def select_for_ide(intellij = None, intellij_ue = None, android_studio = None, clion = None, default = []):
    """Selects for the supported IDEs.

    Args:
        intellij: Files to use for IntelliJ. If None, will use default.
        intellij_ue: Files to use for IntelliJ UE. If None, will use value chosen for 'intellij'.
        android_studio: Files to use for Android Studio. If None will use default.
        clion: Files to use for CLion. If None will use default.
        default: Files to use for any IDEs not passed.
    Returns:
        A select statement on all plugin_apis to lists of files, sorted into IDEs.

    Example:
      java_library(
        name = "foo",
        srcs = select_for_ide(
            clion = [":cpp_only_sources"],
            default = [":java_only_sources"],
        ),
      )
    """
    intellij = intellij if intellij != None else default
    intellij_ue = intellij_ue if intellij_ue != None else intellij
    android_studio = android_studio if android_studio != None else default
    clion = clion if clion != None else default

    ide_to_value = {
        "intellij": intellij,
        "intellij-ue": intellij_ue,
        "android-studio": android_studio,
        "clion": clion,
    }

    # Map (direct ij_product) -> corresponding ide value
    params = dict()
    for ij_product, value in DIRECT_IJ_PRODUCTS.items():
        params[ij_product] = ide_to_value[value.ide]
    params["default"] = default

    return select_for_plugin_api(params)

def _plugin_api_directory(value):
    if hasattr(value, "oss_workspace"):
        directory = value.oss_workspace
    else:
        directory = value.directory
    return "@" + directory + "//"

def select_from_plugin_api_directory(intellij, android_studio, clion, intellij_ue = None):
    """Internal convenience method to generate select statement from the IDE's plugin_api directories.

    Args:
      intellij: the items that IntelliJ product to use.
      android_studio: the items that android studio product to use.
      clion: the items that clion product to use.
      intellij_ue: the items that intellij ue product to use.

    Returns:
      a select statement map from DIRECT_IJ_PRODUCTS to items that they should use.

    """

    ide_to_value = {
        "intellij": intellij,
        "intellij-ue": intellij_ue if intellij_ue else intellij,
        "android-studio": android_studio,
        "clion": clion,
    }

    # Map (direct ij_product) -> corresponding product directory
    params = dict()
    for ij_product, value in DIRECT_IJ_PRODUCTS.items():
        params[ij_product] = [_plugin_api_directory(value) + item for item in ide_to_value[value.ide]]

    # No ij_product == intellij-latest
    params["default"] = params[INDIRECT_IJ_PRODUCTS["intellij-latest"]]

    return select_for_plugin_api(params)

def select_from_plugin_api_version_directory(params):
    """Selects for a plugin_api direct version based on its directory.

    Args:
        params: A dict with ij_product -> value.
                You may only include direct ij_products here,
                not indirects (eg. intellij-latest).
    Returns:
        A select statement on all plugin_apis. Unless you include a "default",
        a non-matched plugin_api will result in an error.
    """
    for indirect_ij_product in INDIRECT_IJ_PRODUCTS:
        if indirect_ij_product in params:
            error_message = "".join([
                "Do not select on indirect ij_product %s. " % indirect_ij_product,
                "Instead, select on an exact ij_product.",
            ])
            fail(error_message)

    # Map (direct ij_product) -> corresponding value relative to product directory
    for ij_product, value in params.items():
        if ij_product != "default":
            params[ij_product] = [_plugin_api_directory(DIRECT_IJ_PRODUCTS[ij_product]) + item for item in value]

    return _do_select_for_plugin_api(params)

def get_versions_to_build(product):
    """"Returns a set of unique product version aliases to test and build during regular release process.

    For each product, we care about four versions aliases to build and release to JetBrains
    repository; -latest, -beta, -oss-oldest-stable and oss-latest-stable.
    However, some of these aliases can point to the same IDE version and this can lead
    to conflicts if we attempt to blindly build and upload the four versions.
    This function is used to return only the aliases that point to different
    IDE versions of the given product.

    Args:
        product: name of the product; android-studio, clion, intellij-ue

    Returns:
        A space separated list of product version aliases to build, the values can be
        oss-oldest-stable, oss-latest-stable, internal-stable and internal-beta.
    """
    aliases_to_build = []
    plugin_api_versions = []
    for alias in ["oss-oldest-stable", "latest", "oss-latest-stable", "beta"]:
        indirect_ij_product = product + "-" + alias
        if indirect_ij_product not in INDIRECT_IJ_PRODUCTS:
            fail(
                "Product-version alias %s not found." % indirect_ij_product,
                "Invalid product: %s only android-studio, clion and intellij-ue are accepted." % product,
            )

        version = INDIRECT_IJ_PRODUCTS[indirect_ij_product]
        if version not in plugin_api_versions:
            plugin_api_versions.append(version)
            if alias == "latest":
                aliases_to_build.append("internal-stable")
            elif alias == "beta":
                aliases_to_build.append("internal-beta")
            else:
                aliases_to_build.append(alias)

    return " ".join(aliases_to_build)

def get_unique_supported_oss_ide_versions(product):
    """"Returns the unique supported IDE versions for the given product in the OSS Bazel plugin

    Args:
        product: name of the product; android-studio, clion, intellij-ue

    Returns:
        A space separated list of the aliases of the unique IDE versions for the
        OSS Bazel plugin.
    """
    supported_versions = []
    unique_aliases = []
    for alias in ["oss-oldest-stable", "oss-latest-stable"]:
        indirect_ij_product = product + "-" + alias
        if indirect_ij_product not in INDIRECT_IJ_PRODUCTS:
            fail(
                "Product-version alias %s not found." % indirect_ij_product,
                "Invalid product: %s, only android-studio, clion and intellij-ue are accepted." % product,
            )
        ver = INDIRECT_IJ_PRODUCTS[indirect_ij_product]
        if ver not in supported_versions:
            supported_versions.append(ver)
            unique_aliases.append(alias)

    return " ".join(unique_aliases)

def combine_visibilities(*args):
    """
    Concatenates the given lists of visibilities and returns the combined list.

    If one of the given elements is //visibility:public then return //visibility:public
    If one of the lists is None, skip it.
    If the result list is empty, then return None.

    Args:
      *args: the list of visibilities lists to combine
    Returns:
      the concatenated visibility targets list
    """
    res = []
    for arg in args:
        if arg:
            for visibility in arg:
                if visibility == "//visibility:public":
                    return ["//visibility:public"]
                res.append(visibility)
    if res == []:
        return None
    return res
