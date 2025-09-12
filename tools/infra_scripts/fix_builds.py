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

def rename_tilde_build(root: Path, dry_run: bool, force: bool) -> int:
    renamed = 0
    for p in root.rglob("~BUILD"):
        if p.is_file():
            target = p.with_name("BUILD")
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
        description="recursively: rename '~BUILD' -> 'BUILD' and remove 'BUILD.bazel'"
    )
    ap.add_argument(
        "folder",
        nargs="?",
        default=".",
        help="root folder to process (default: current directory)"
    )
    ap.add_argument("-n", "--dry-run", action="store_true", help="show actions without changing anything")
    ap.add_argument("-f", "--force", action="store_true", help="overwrite existing BUILD when renaming")
    args = ap.parse_args()

    root = Path(args.folder).resolve()
    if not root.exists() or not root.is_dir():
        print(f"not a directory: {root}", file=sys.stderr)
        sys.exit(1)

    deleted = remove_build_bazel(root, args.dry_run)
    renamed = rename_tilde_build(root, args.dry_run, args.force)

    if args.dry_run:
        print(f"[dry-run] summary: would delete {deleted} file(s), rename {renamed} file(s)")
    else:
        print(f"summary: deleted {deleted} file(s), renamed {renamed} file(s)")

if __name__ == "__main__":
    main()
