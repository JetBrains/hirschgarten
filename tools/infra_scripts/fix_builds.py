#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
import sys

def remove_build_bazel(root: Path, dry_run: bool) -> int:
    removed = 0
    for p in root.rglob("BUILD.bazel"):
        if p.is_file():
            if dry_run:
                print(f"[dry-run] delete {p}")
            else:
                try:
                    p.unlink()
                    print(f"deleted {p}")
                    removed += 1
                except Exception as e:
                    print(f"error deleting {p}: {e}", file=sys.stderr)
    return removed

def rename_files(root: Path, patterns, dry_run: bool, force: bool) -> int:
    """Rename multiple file name patterns under root.

    patterns: iterable of (source_name, target_name) tuples
    """
    renamed = 0
    for src_name, dst_name in patterns:
        for p in root.rglob(src_name):
            if p.is_file():
                target = p.with_name(dst_name)
                if dry_run:
                    action = "replace" if (force and target.exists()) else "rename"
                    print(f"[dry-run] {action} {p} -> {target}")
                    continue
                try:
                    if force:
                        os.replace(p, target)  # overwrites if target exists
                        print(f"replaced {p} -> {target}")
                        renamed += 1
                    else:
                        if target.exists():
                            print(f"skip (target exists) {p} -> {target}", file=sys.stderr)
                        else:
                            p.rename(target)
                            print(f"renamed {p} -> {target}")
                            renamed += 1
                except Exception as e:
                    print(f"error renaming {p} -> {target}: {e}", file=sys.stderr)
    return renamed

def main():
    ap = argparse.ArgumentParser(
        description="recursively: rename '~BUILD' -> 'BUILD', rename '~MODULE.bazel' -> 'MODULE.bazel', and remove 'BUILD.bazel'"
    )
    ap.add_argument(
        "folder",
        nargs="?",
        default=".",
        help="root folder to process (default: current directory)"
    )
    ap.add_argument("-n", "--dry-run", action="store_true", help="show actions without changing anything")
    ap.add_argument("-f", "--force", action="store_true", help="overwrite existing BUILD or MODULE.bazel when renaming")
    args = ap.parse_args()

    root = Path(args.folder).resolve()
    if not root.exists() or not root.is_dir():
        print(f"not a directory: {root}", file=sys.stderr)
        sys.exit(1)

    deleted = remove_build_bazel(root, args.dry_run)
    rename_specs = [("~BUILD", "BUILD"), ("~MODULE.bazel", "MODULE.bazel")]
    renamed = rename_files(root, rename_specs, args.dry_run, args.force)

    if args.dry_run:
        print(f"[dry-run] summary: would delete {deleted} file(s), rename {renamed} file(s)")
    else:
        print(f"summary: deleted {deleted} file(s), renamed {renamed} file(s)")

if __name__ == "__main__":
    main()
