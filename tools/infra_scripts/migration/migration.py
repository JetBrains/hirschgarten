import os
import sys
import subprocess
import shutil
from pathlib import Path

def error(message):
    print(f"ERROR: {message}", file=sys.stderr)

def check_git_filter_repo():
    if shutil.which("git-filter-repo") is None:
        error("git-filter-repo is not installed or not in your PATH.")
        print("To install git-filter-repo, follow these steps:")
        print("0. On macOS simply run 'brew install git-filter-repo'")
        print("If you don't have homebrew installed, or not on macOS:")
        print("1. Ensure you have Python 3 installed.")
        print("2. Run: pip3 install git-filter-repo")
        print("3. Make sure the installation directory is in your PATH.")
        print("For more information, visit: https://github.com/newren/git-filter-repo")
        return False
    return True

def setup_monorepo(monorepo):
    if monorepo.startswith(("http", "git@")):
        monorepo_dir = Path(monorepo).stem
        if not Path(monorepo_dir).is_dir():
            result = subprocess.run(["git", "clone", monorepo, monorepo_dir], check=False)
            if result.returncode != 0:
                error(f"Failed to clone {monorepo}")
                return None
    else:
        monorepo_dir = monorepo.lstrip("./")
        if not Path(monorepo_dir).is_dir():
            error(f"Directory {monorepo_dir} does not exist")
            return None
    
    return monorepo_dir

def setup_unmerged_repo(unmerged_repo, unmerged_dir):
    if unmerged_repo.startswith(("http", "git@")):
        result = subprocess.run(["git", "clone", unmerged_repo, unmerged_dir], check=False)
        if result.returncode != 0:
            error(f"Failed to clone {unmerged_repo}")
            return False
    else:
        try:
            shutil.copytree(unmerged_repo, unmerged_dir)
        except shutil.Error:
            error(f"Failed to copy {unmerged_repo}")
            return False
    return True

def get_subdirectory(repo_name):
    repo_name = Path(repo_name).stem
    subdirs = {
        "bazel-bsp": "server",
        "intellij-bsp": "plugin-bsp",
        "intellij-bazel": "plugin-bazel"
    }
    return subdirs.get(repo_name)

def migrate_branch(monorepo, unmerged_repo, branch_name_to_migrate):
    subdir = get_subdirectory(unmerged_repo)
    if not subdir:
        error(f"Unknown repository name: {unmerged_repo}")
        return False

    repo_name = Path(unmerged_repo).stem

    base_dir = Path.cwd()
    monorepo_dir = setup_monorepo(monorepo)
    if not monorepo_dir:
        error("Failed to setup monorepo")
        return False

    unmerged_dir = base_dir / "temp_unmerged"
    if not setup_unmerged_repo(unmerged_repo, unmerged_dir):
        error("Failed to setup unmerged repo")
        return False

    os.chdir(unmerged_dir)
    result = subprocess.run(["git", "checkout", branch_name_to_migrate], check=False)
    if result.returncode != 0:
        error(f"Failed to checkout branch {branch_name_to_migrate}")
        return False

    print("Running git-filter-repo...")
    result = subprocess.run(["git-filter-repo", "--to-subdirectory-filter", subdir, "--force"], check=False)
    if result.returncode != 0:
        error("git-filter-repo failed")
        return False

    os.chdir(base_dir / monorepo_dir)
    subprocess.run(["git", "remote", "add", "unmerged", str(unmerged_dir)], check=True)
    subprocess.run(["git", "fetch", "unmerged"], check=True)

    new_branch = f"{subdir}/{branch_name_to_migrate}"
    subprocess.run(["git", "checkout", "-b", new_branch], check=True)

    result = subprocess.run([
        "git", "merge", f"unmerged/{branch_name_to_migrate}", 
        "--allow-unrelated-histories", 
        "-m", f"Migrate {branch_name_to_migrate} from {repo_name} to {subdir}"
    ], check=False)
    if result.returncode != 0:
        error("Failed to merge branch")
        return False

    subprocess.run(["git", "remote", "remove", "unmerged"], check=True)

    print(f"Migration completed. New branch '{new_branch}' created in {monorepo_dir}")
    print("You can now push this branch to your fork of the monorepo.")

    os.chdir(base_dir)
    shutil.rmtree(unmerged_dir)

    return True

def main():
    if not check_git_filter_repo():
        sys.exit(1)

    if len(sys.argv) != 4:
        error("Incorrect number of arguments")
        print("Usage: python script.py <monorepo> <unmerged_repo> <branch_name_to_migrate>")
        print("Examples:")
        print("  python script.py https://github.com/user/hirschgarten https://github.com/user/bazel-bsp branch_name_to_migrate")
        print("  python script.py git@github.com:user/hirschgarten.git git@github.com:user/bazel-bsp.git branch_name_to_migrate")
        print("  python script.py ./hirschgarten ./bazel-bsp branch_name_to_migrate")
        sys.exit(1)

    if not migrate_branch(sys.argv[1], sys.argv[2], sys.argv[3]):
        error("Migration failed")
        sys.exit(1)

    print("Migration completed successfully")

if __name__ == "__main__":
    main()