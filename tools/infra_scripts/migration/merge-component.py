import argparse
import os
import subprocess
import sys
import tempfile

def error(message):
    print(f"ERROR: {message}", file=sys.stderr)

def check_git_filter_repo():
    if subprocess.run(["git-filter-repo", "--version"], capture_output=True, check=False).returncode != 0:
        error("git-filter-repo is not installed or not in your PATH.")
        print("To install git-filter-repo, follow these steps:")
        print("1. Ensure you have Python 3 installed.")
        print("2. Run: pip3 install git-filter-repo")
        print("3. Make sure the installation directory is in your PATH.")
        print("For more information, visit: https://github.com/newren/git-filter-repo")
        return False
    return True

def run_command(command, cwd=None, check=True):
    """Run a shell command and return its output."""
    try:
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, shell=True, check=check)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        if check:
            print(f"Error executing command: {command}")
            print(f"Error message: {e.stderr}")
            sys.exit(1)
        return None

def get_local_branches(repo_path):
    """Get a list of local branches in the repository."""
    return run_command("git branch --format='%(refname:short)'", cwd=repo_path).split('\n')

def get_remote_branches(repo_path, remote):
    """Get a list of remote branches for a specific remote."""
    output = run_command(f"git branch -r --format='%(refname:short)'", cwd=repo_path)
    remote_branches = [branch.replace(f"{remote}/", "") for branch in output.split('\n') if branch.startswith(f"{remote}/")]
    print(f"Remote branches: {remote_branches}")
    return remote_branches

def process_repo(monorepo_path, local_repo_path, subfolder):
    """Process the local repository and merge it into the monorepo."""
    print(f"Processing {local_repo_path}...")

    with tempfile.TemporaryDirectory(prefix=f"temp-{subfolder}-", dir=os.getcwd()) as temp_dir:
        print(f"Created temporary directory: {temp_dir}")

        run_command(f"git clone {local_repo_path} {temp_dir}")
        run_command(f"git-filter-repo --to-subdirectory-filter {subfolder} --force", cwd=temp_dir)

        run_command(f"git remote add {subfolder} {temp_dir}", cwd=monorepo_path)
        run_command(f"git fetch {subfolder}", cwd=monorepo_path)

        local_branches = get_local_branches(temp_dir)
        print(f"Local branches in filtered repo: {local_branches}")
        remote_branches = get_remote_branches(monorepo_path, subfolder)

        for branch in local_branches:
            new_branch_name = f"{subfolder}/{branch}"
            if new_branch_name in run_command("git branch", cwd=monorepo_path).split('\n'):
                print(f"Branch {new_branch_name} already exists locally, skipping...")
            else:
                print(f"Creating new branch: {new_branch_name}")
                run_command(f"git branch {new_branch_name} {subfolder}/{branch}", cwd=monorepo_path)

                # Checkout the new branch
                run_command(f"git checkout {new_branch_name}", cwd=monorepo_path)

                # Merge main branch allowing unrelated histories
                print(f"Merging main branch into {new_branch_name}...")
                merge_result = run_command(f"git merge main --allow-unrelated-histories -m 'Merge {new_branch_name} into main'", cwd=monorepo_path, check=False)

                if "CONFLICT" in merge_result:
                    print(f"Merge conflicts occurred in {new_branch_name}. Please resolve them manually.")
                else:
                    print(f"Successfully merged main into {new_branch_name}.")

                # Return to the original branch (usually main)
                run_command("git checkout -", cwd=monorepo_path)

        run_command(f"git remote remove {subfolder}", cwd=monorepo_path)

    print(f"Finished processing {local_repo_path}")

def main():
    parser = argparse.ArgumentParser(description="Merge a local repository into a monorepo.")
    parser.add_argument("monorepo_path", help="Path or URL to the fork of the monorepo")
    parser.add_argument("local_repo_path", help="Path to the local repository to merge")
    parser.add_argument("subfolder", help="Folder name to put the content in the monorepo")
    args = parser.parse_args()

    if not check_git_filter_repo():
        sys.exit(1)

    if not os.path.exists(args.monorepo_path):
        run_command(f"git clone {args.monorepo_path} {args.monorepo_path}")

    process_repo(args.monorepo_path, args.local_repo_path, args.subfolder)

    print("Repository merge process completed successfully!")

if __name__ == "__main__":
    main()