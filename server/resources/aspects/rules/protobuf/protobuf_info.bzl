load("//aspects:utils/utils.bzl", "collect_targets_from_attrs", "create_struct", "file_location", "update_sync_output_groups")

def is_protobuf_rule_kind(ctx):
    return ctx.rule.kind in [
        "proto_library",
    ]

def extract_protobuf_info(target, ctx, output_groups, **kwargs):
    if not is_protobuf_rule_kind(ctx):
        return None, None
    result = create_struct(
        allow_exports = get_protobuf_allow_exports(ctx),
        exports = get_protobuf_exports(ctx),
        import_prefix = get_protobuf_import_prefix(ctx),
        strip_import_prefix = get_protobuf_strip_import_prefix(ctx),
    )
    return dict(protobuf_target_info = result), None

def get_protobuf_allow_exports(ctx):
    allow_exports = getattr(ctx.rule.attr, "allow_exports")
    if allow_exports:
        return allow_exports
    return False

def get_protobuf_exports(ctx):
    exports = []
    export_attr = getattr(ctx.rule.attr, "exports")
    if not export_attr:
        return exports
    for export_attr in export_attr:
        exports.append(export_attr)
    return exports

def get_protobuf_import_prefix(ctx):
    import_path = getattr(ctx.rule.attr, "import_prefix")
    if import_path:
        return import_path
    return ""

def get_protobuf_strip_import_prefix(ctx):
    strip_import_prefix = getattr(ctx.rule.attr, "strip_import_prefix")
    if strip_import_prefix:
        return strip_import_prefix
    return ""
