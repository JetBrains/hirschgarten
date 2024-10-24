# Copyright 2019-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

def abs(num):
    if num < 0:
        return -num
    else:
        return num

def map(f, xs):
    return [f(x) for x in xs]

def filter(f, xs):
    return [x for x in xs if f(x)]

def not_none(x):
    return x != None

def filter_not_none(xs):
    return filter(not_none, xs)

def flatten(xss):
    return [x for xs in xss for x in xs]

def flatmap(f, xs):
    return flatten(map(f, xs))

def file_location(file):
    if file == None:
        return None

    return to_file_location(
        file.path,
        file.root.path if not file.is_source else "",
        file.is_source,
        file.owner.workspace_root.startswith("..") or file.owner.workspace_root.startswith("external"),
    )

def _strip_root_exec_path_fragment(path, root_fragment):
    if root_fragment and path.startswith(root_fragment + "/"):
        return path[len(root_fragment + "/"):]
    return path

def _strip_external_workspace_prefix(path):
    if path.startswith("../") or path.startswith("external/"):
        without_prefix = path.split("/", 2)
        if len(without_prefix) > 2:
            return without_prefix[2]
        else:
            return ""
    return path

def to_file_location(exec_path, root_exec_path_fragment, is_source, is_external):
    # directory structure:
    # exec_path = (../repo_name)? + (root_fragment)? + relative_path
    relative_path = _strip_external_workspace_prefix(exec_path)
    relative_path = _strip_root_exec_path_fragment(relative_path, root_exec_path_fragment)

    root_exec_path_fragment = exec_path[:-(len("/" + relative_path))] if relative_path != "" else exec_path

    return create_file_location(relative_path, is_source, is_external, root_exec_path_fragment)

def create_file_location(relative_path, is_source, is_external, root_execution_path_fragment):
    return struct(
        relative_path = relative_path,
        is_source = is_source,
        is_external = is_external,
        root_execution_path_fragment = root_execution_path_fragment,
    )

def create_struct(**kwargs):
    d = {name: kwargs[name] for name in kwargs if kwargs[name] != None}
    return struct(**d)

def update_sync_output_groups(groups_dict, key, new_set):
    groups_dict[key] = depset(transitive = [groups_dict.get(key, depset()), new_set])

def get_aspect_ids(ctx, target):
    """Returns the all aspect ids, filtering out self."""
    aspect_ids = None
    if hasattr(ctx, "aspect_ids"):
        aspect_ids = ctx.aspect_ids
    elif hasattr(target, "aspect_ids"):
        aspect_ids = target.aspect_ids
    else:
        return None
    return [aspect_id for aspect_id in aspect_ids if "bsp_target_info_aspect" not in aspect_id]

def is_external(target):
    return not str(target.label).startswith("@@//") and not str(target.label).startswith("@//") and not str(target.label).startswith("//")

def convert_struct_to_dict(s):
    attrs = dir(s)

    # two deprecated methods of struct
    if "to_json" in attrs:
        attrs.remove("to_json")
    if "to_proto" in attrs:
        attrs.remove("to_proto")

    return {key: getattr(s, key) for key in attrs}

def log(text, level):
    print("[" + level + "] " + str(text))

def log_warn(text):
    log(text, "WARN")

def is_valid_aspect_target(target):
    return hasattr(target, "bsp_info")

def _collect_target_from_attr(rule_attrs, attr_name, result):
    """Collects the targets from the given attr into the result."""
    if not hasattr(rule_attrs, attr_name):
        return
    attr_value = getattr(rule_attrs, attr_name)
    type_name = type(attr_value)
    if type_name == "Target":
        result.append(attr_value)
    elif type_name == "list":
        result.extend(attr_value)

def collect_targets_from_attrs(rule_attrs, attrs):
    result = []
    for attr_name in attrs:
        _collect_target_from_attr(rule_attrs, attr_name, result)
    return [target for target in result if is_valid_aspect_target(target)]

COMPILE_DEPS = [
    "deps",
    "jars",
    "exports",
    "associates",
    "proc_macro_deps",
]

PRIVATE_COMPILE_DEPS = [
    "_java_toolchain",
    "_jvm",
    "runtime_jdk",
]

RUNTIME_DEPS = [
    "runtime_deps",
]

ALL_DEPS = COMPILE_DEPS + PRIVATE_COMPILE_DEPS + RUNTIME_DEPS
