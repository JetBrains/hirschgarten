#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
import sys

def plan_build_bazel_deletions(root: Path) -> list[Path]:
    deletions = []
    for override in root.rglob("~BUILD"):
        build_bazel = override.with_name("BUILD.bazel")
        if override.is_file() and build_bazel.is_file():
            deletions.append(build_bazel)
    return deletions

def plan_renames(root: Path) -> list[tuple[Path, Path]]:
    renames = []
    for alias in root.rglob("~*"):
        if alias.is_file() and len(alias.name) > 1:
            renames.append((alias, alias.with_name(alias.name[1:])))
    return renames

def conflicts_with_different_content(renames: list[tuple[Path, Path]]) -> list[tuple[Path, Path]]:
    return [
        (src, dst)
        for src, dst in renames
        if dst.exists() and dst.read_bytes() != src.read_bytes()
    ]

def main():
    ap = argparse.ArgumentParser(
        description="recursively: in each directory with '~BUILD', replace 'BUILD' with it and remove 'BUILD.bazel'; rename every other '~X' to 'X' (e.g. '~MODULE.bazel' -> 'MODULE.bazel')"
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

    deletions = plan_build_bazel_deletions(root)
    renames = plan_renames(root)
    conflicts = conflicts_with_different_content(renames)
    if conflicts and not args.force:
        for src, dst in conflicts:
            print(f"error: {dst} exists with different content than {src}", file=sys.stderr)
        print("error: nothing changed; rerun with -f to overwrite", file=sys.stderr)
        sys.exit(1)

    for path in deletions:
        if args.dry_run:
            print(f"[dry-run] delete {path}")
        else:
            path.unlink()
            print(f"deleted {path}")

    for src, dst in renames:
        if args.dry_run:
            print(f"[dry-run] replace {src} -> {dst}")
        else:
            os.replace(src, dst)
            print(f"replaced {src} -> {dst}")

    if args.dry_run:
        print(f"[dry-run] summary: would delete {len(deletions)} file(s), replace {len(renames)} file(s)")
    else:
        print(f"summary: deleted {len(deletions)} file(s), replaced {len(renames)} file(s)")

if __name__ == "__main__":
    main()
