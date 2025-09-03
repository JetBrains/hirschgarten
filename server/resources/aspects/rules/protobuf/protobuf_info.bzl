load("@rules_proto//proto:defs.bzl", "proto_common")
load("//aspects:utils/utils.bzl", "ALL_DEPS", "COMPILE_DEPS", "PRIVATE_COMPILE_DEPS", "RUNTIME_DEPS", "abs", "collect_targets_from_attrs", "create_struct", "do_starlark_string_expansion_dict", "file_location", "get_aspect_ids", "is_valid_aspect_target", "update_sync_output_groups")

def extract_protobuf_info(target, ctx, output_groups, **kwargs):
    if ProtoInfo not in target:
        return None, None
    protobuf_info = target[ProtoInfo]
    proto_target_info = create_struct(
        source_mappings = list(extract_source_mappings(protobuf_info)),
    )
    return dict(protobuf_target_info = proto_target_info), None

def extract_source_mappings(protobuf_info):
    mappings = []
    for source in protobuf_info.direct_sources:
        proto = create_struct(
            import_path = proto_common.get_import_path(source),
            proto_file = file_location(source),
        )
        mappings.append(proto)
    return mappings
