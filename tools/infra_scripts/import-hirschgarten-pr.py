#!/usr/bin/env python3
"""
import-hirschgarten-pr — fetch pr from jetbrains/hirschgarten, squash its *own* changes,
rewrite paths, emit patch, apply to current branch, and narrate everything.

usage:
    ./import-hirschgarten-pr.py 291
"""

import argparse, json, os, re, subprocess, sys, urllib.request
from pathlib import Path
from typing import List, Optional, Tuple

HIRSCH = "https://github.com/JetBrains/hirschgarten.git"

# ---------- helpers ---------------------------------------------------------

def sh(cmd: List[str], *, capture: bool = True, check: bool = True, env=None) -> str:
    print(f"$ {' '.join(cmd)}", flush=True)
    res = subprocess.run(cmd, text=True, capture_output=capture, check=check, env=env)
    if capture and res.stdout:
        print(res.stdout.rstrip(), flush=True)
    return res.stdout.strip() if capture else ""

def pr_info(num: int) -> Tuple[Optional[str], str]:
    url = f"https://api.github.com/repos/JetBrains/hirschgarten/pulls/{num}"
    print(f"# hitting gh api: {url}")
    try:
        with urllib.request.urlopen(url) as r:
            d = json.load(r)
            return d["head"]["sha"], d["base"]["ref"]
    except Exception as e:
        print(f"# gh api failed ({e}); defaulting base to main")
        return None, "main"

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

# ---------- main ------------------------------------------------------------

def main(argv=None):
    ap = argparse.ArgumentParser(description="squash pr, rewrite paths, apply patch")
    ap.add_argument("pr", type=int)
    ap.add_argument("--prefix", default="plugins/bazel/")
    ap.add_argument("--keep-pr", action="store_true")
    a = ap.parse_args(argv)

    pr_branch   = f"pr-{a.pr}"
    base_branch = f"{pr_branch}-base"
    tmp_branch  = f"{pr_branch}-squash-tmp"

    print(f"# fetch pr {a.pr}")
    sh(["git","fetch",HIRSCH,f"pull/{a.pr}/head:{pr_branch}"], capture=False)

    head_sha, base_ref = pr_info(a.pr)

    print(f"# fetch base ref {base_ref}")
    sh(["git","fetch",HIRSCH,f"refs/heads/{base_ref}:{base_branch}"], capture=False)

    # true base is the merge‑base, so merges from main inside the pr don’t bloat the diff
    base_sha = sh(["git","merge-base",pr_branch,base_branch])
    print(f"# using merge‑base {base_sha[:7]}")

    # squash
    sh(["git","checkout","-q","-B",tmp_branch,pr_branch], capture=False)
    sh(["git","reset","--soft",base_sha], capture=False)

    commit_shas = sh(["git","rev-list","--reverse",f"{base_sha}..{pr_branch}"]).split()
    if not commit_shas:
        print("no commits to squash", file=sys.stderr); sys.exit(1)

    author   = sh(["git","show","-s","--format=%an <%ae>",commit_shas[0]])
    message  = "\n\n".join(sh(["git","show","-s","--format=%B",c]) for c in commit_shas).strip()
    date     = sh(["git","show","-s","--format=%cI",commit_shas[-1]])
    env = {**os.environ,"GIT_COMMITTER_DATE":date}

    sh(["git","commit","--author",author,"-m",message,"--date",date], capture=False, env=env)

    # patchify
    raw  = sh(["git","format-patch","-1","--stdout"])
    pat  = rewrite(raw,a.prefix)
    ppth = Path(f"pr-{a.pr}-bazel.patch").resolve()
    ppth.write_text(pat)
    print(f"# patch written to {ppth}")

    # cleanup tmp
    sh(["git","checkout","-q","-"], capture=False)
    sh(["git","branch","-D",tmp_branch], capture=False)
    if not a.keep_pr:
        sh(["git","branch","-D",pr_branch], capture=False)
        sh(["git","branch","-D",base_branch], capture=False)

    # apply
    print("# git am -3")
    try:
        sh(["git","am","-3",str(ppth)], capture=False)
        print("# applied ✔")
    except subprocess.CalledProcessError:
        print("# git am failed; fix then `git am --continue` or `--abort`", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    sys.exit(main())
