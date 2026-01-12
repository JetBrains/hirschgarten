#!/usr/bin/env python3
"""
import-hirschgarten-pr — fetch PR from jetbrains/hirschgarten via GitHub URLs,
rewrite paths, emit patch, apply to current branch.

usage:
    ./import-hirschgarten-pr.py 291
"""

import argparse, json, re, subprocess, sys, urllib.request
from pathlib import Path
from typing import List, Tuple

REPO = "JetBrains/hirschgarten"

def sh(cmd: List[str], *, capture: bool = True, check: bool = True) -> str:
    print(f"$ {' '.join(cmd)}", flush=True)
    res = subprocess.run(cmd, text=True, capture_output=capture, check=check)
    if capture and res.stdout:
        print(res.stdout.rstrip(), flush=True)
    return res.stdout.strip() if capture else ""

def fetch_url(url: str) -> str:
    print(f"# fetching {url}")
    with urllib.request.urlopen(url) as r:
        return r.read().decode()

def get_author_info(num: int) -> Tuple[str, str]:
    """Extract From: and Date: headers from first commit in .patch"""
    patch = fetch_url(f"https://github.com/{REPO}/pull/{num}.patch")
    author = None
    date = None
    for line in patch.splitlines():
        if line.startswith("From:") and not author:
            author = line
        elif line.startswith("Date:") and not date:
            date = line
        if author and date:
            break
    return (
        author or "From: Unknown <unknown@example.com>",
        date or "Date: Thu, 1 Jan 1970 00:00:00 +0000"
    )

def get_aggregate_diff(num: int) -> str:
    """Fetch aggregate diff from .diff endpoint"""
    return fetch_url(f"https://github.com/{REPO}/pull/{num}.diff")

def get_pr_title(num: int) -> str:
    """Fetch PR title from GitHub API"""
    url = f"https://api.github.com/repos/{REPO}/pulls/{num}"
    print(f"# fetching {url}")
    with urllib.request.urlopen(url) as r:
        return json.load(r)["title"]

def add_prefix(p: str, pre: str) -> str:
    return p if p.startswith(pre) or p == "/dev/null" else pre + p

def rewrite(patch: str, pre: str) -> str:
    out = []
    for ln in patch.splitlines(keepends=True):
        if ln.startswith("diff --git"):
            m = re.match(r"diff --git a/(.+?) b/(.+)", ln)
            if m:
                a, b = m.groups()
                ln = f"diff --git a/{add_prefix(a, pre)} b/{add_prefix(b, pre)}\n"
        elif ln.startswith("--- a/"):
            ln = f"--- a/{add_prefix(ln[6:].strip(), pre)}\n"
        elif ln.startswith("+++ b/"):
            ln = f"+++ b/{add_prefix(ln[6:].strip(), pre)}\n"
        elif ln.startswith("rename from "):
            ln = f"rename from {add_prefix(ln[12:].strip(), pre)}\n"
        elif ln.startswith("rename to "):
            ln = f"rename to {add_prefix(ln[10:].strip(), pre)}\n"
        out.append(ln)
    return "".join(out)

def main(argv=None):
    ap = argparse.ArgumentParser(description="import PR from hirschgarten, rewrite paths, apply")
    ap.add_argument("pr", type=int)
    ap.add_argument("--prefix", default="plugins/bazel/")
    ap.add_argument("--no-apply", action="store_true", help="generate patch without applying")
    a = ap.parse_args(argv)

    print(f"# importing PR {a.pr} from {REPO}")
    author, date = get_author_info(a.pr)
    title = get_pr_title(a.pr)
    diff = get_aggregate_diff(a.pr)

    print(f"# rewriting paths with prefix {a.prefix}")
    rewritten = rewrite(diff, a.prefix)

    patch_content = f"""From 0000000000000000000000000000000000000000 Mon Sep 17 00:00:00 2001
{author}
{date}
Subject: [PATCH] {title}

hirschgarten PR #{a.pr}
---
{rewritten}
"""

    patch_path = Path(f"pr-{a.pr}-bazel.patch").resolve()
    patch_path.write_text(patch_content)
    print(f"# patch written to {patch_path}")

    if a.no_apply:
        print("# --no-apply specified, skipping git am")
        return 0

    print("# git am -3")
    try:
        sh(["git", "am", "-3", str(patch_path)], capture=False)
        print("# applied ✔")
    except subprocess.CalledProcessError:
        print("# git am failed; fix then `git am --continue` or `--abort`", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
