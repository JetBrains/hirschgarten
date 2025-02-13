import sys
import re
import os
import subprocess
import urllib.request
import urllib.error
import tempfile
import time
from typing import List, Dict, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from logging import getLogger, basicConfig, INFO
from threading import Lock

# Setup logging
basicConfig(level=INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = getLogger(__name__)

def replace_variables(content: str, intellij_version: str, plugin_repository_version: str, intellij_repository_version: str, intellij_repository: str) -> str:
    content = re.sub(rf'SDK_{intellij_version}_PLUGIN_REPOSITORY_VERSION = "[^"]*"', f'SDK_{intellij_version}_PLUGIN_REPOSITORY_VERSION = "{plugin_repository_version}"', content)
    content = re.sub(rf'SDK_{intellij_version}_INTELLIJ_REPOSITORY_VERSION = "[^"]*"', f'SDK_{intellij_version}_INTELLIJ_REPOSITORY_VERSION = "{intellij_repository_version}"', content)
    content = re.sub(rf'SDK_{intellij_version}_INTELLIJ_REPOSITORY = "[^"]*"', f'SDK_{intellij_version}_INTELLIJ_REPOSITORY = "{intellij_repository}"', content)
    return content

def parse_url(url_template: str, variables: Dict[str, str]) -> str:
    """
    Parse and format a URL template using provided variables, handling both two and three positional arguments.
    """
    try:
        if url_template.count('%') == 2:
            return url_template % (
                variables.get("SDK_{intellij_version}_PLUGIN_REPOSITORY_VERSION".format(intellij_version=intellij_version), ''),
                variables.get("SDK_{intellij_version}_PLUGIN_REPOSITORY_VERSION".format(intellij_version=intellij_version), '')
            )
        elif url_template.count('%') == 3:
            return url_template % (
                variables.get("SDK_{intellij_version}_INTELLIJ_REPOSITORY".format(intellij_version=intellij_version), ''),
                variables.get("SDK_{intellij_version}_INTELLIJ_REPOSITORY_VERSION".format(intellij_version=intellij_version), ''),
                variables.get("SDK_{intellij_version}_INTELLIJ_REPOSITORY_VERSION".format(intellij_version=intellij_version), '')
            )
        else:
            raise ValueError(f"Unexpected number of format specifiers in URL template: {url_template}")
    except KeyError as e:
        raise ValueError(f"Missing variable {e} for URL template: {url_template}")
    except TypeError as e:
        raise ValueError(f"Error formatting URL template {url_template}: {e}")

def download_file(url: str, temp_dir: str) -> str:
    file_path = os.path.join(temp_dir, os.path.basename(url))
    logger.info(f"Starting download: {os.path.basename(url)}")
    start_time = time.time()

    def reporthook(block_num: int, block_size: int, total_size: int):
        nonlocal start_time
        if total_size > 0:
            percent = (block_num * block_size) / total_size * 100
            if time.time() - start_time >= 1:
                logger.info(f"Downloading {os.path.basename(url)}: {percent:.2f}% complete")
                start_time = time.time()

    try:
        urllib.request.urlretrieve(url, file_path, reporthook)
        logger.info(f"Finished download: {os.path.basename(url)}")
        return file_path
    except urllib.error.HTTPError as e:
        logger.error(f"HTTPError for {os.path.basename(url)}: {e.code} {e.reason}")
        return ""
    except urllib.error.URLError as e:
        logger.error(f"URLError for {os.path.basename(url)}: {e.reason}")
        return ""

def compute_checksum(file_path: str) -> str:
    result = subprocess.run(['sha256sum', file_path], capture_output=True, text=True, check=True)
    return result.stdout.split()[0]

def update_checksums(content: str, temp_dir: str, intellij_version: str) -> str:
    urls: List[Tuple[str, str]] = re.findall(rf'(\w+_{intellij_version}_\w*URL) = "([^"]*)"', content)
    variables: Dict[str, str] = {
        f'SDK_{intellij_version}_PLUGIN_REPOSITORY_VERSION': plugin_repository_version,
        f'SDK_{intellij_version}_INTELLIJ_REPOSITORY_VERSION': intellij_repository_version,
        f'SDK_{intellij_version}_INTELLIJ_REPOSITORY': intellij_repository,
    }

    download_futures: Dict[Future, Tuple[str, str]] = {}
    with ThreadPoolExecutor(max_workers=8) as executor:
        for var_name, url_template in urls:
            try:
                url = parse_url(url_template, variables)
                future = executor.submit(download_file, url, temp_dir)
                download_futures[future] = (var_name, url)
            except ValueError as e:
                logger.error(f"Error parsing URL for {var_name}: {e}")

        for future in as_completed(download_futures):
            var_name, url = download_futures[future]
            file_path = future.result()
            if file_path:
                checksum = compute_checksum(file_path)
                logger.info(f"Download completed for {os.path.basename(file_path)}. SHA256: {checksum}")
                checksum_var_name = var_name.replace('URL', 'SHA')
                content = re.sub(rf'{checksum_var_name} = "[^"]*"', f'{checksum_var_name} = "{checksum}"', content)
            else:
                logger.warning(f"[{var_name}] Download failed for {url}, skipping checksum update.")

    return content

def main(module_file_path: str, intellij_version: str, plugin_repository_version: str, intellij_repository_version: str, intellij_repository: str) -> None:
    logger.info(f"Reading {module_file_path} file.")
    with open(module_file_path, 'r') as file:
        content = file.read()

    logger.info("Replacing variables in MODULE.bazel content.")
    content = replace_variables(content, intellij_version, plugin_repository_version, intellij_repository_version, intellij_repository)

    with tempfile.TemporaryDirectory() as temp_dir:
        logger.info("Updating checksums for URLs.")
        content = update_checksums(content, temp_dir, intellij_version)

    logger.info(f"Writing updated content back to {module_file_path}.")
    with open(module_file_path, 'w') as file:
        file.write(content)

if __name__ == "__main__":
    if len(sys.argv) != 6:
        print("Usage: script.py <module_file_path> <intellij_version> <plugin_repository_version> <intellij_repository_version> <intellij_repository>")
        sys.exit(1)

    module_file_path: str = sys.argv[1]
    intellij_version: str = sys.argv[2]
    plugin_repository_version: str = sys.argv[3]
    intellij_repository_version: str = sys.argv[4]
    intellij_repository: str = sys.argv[5]

    main(module_file_path, intellij_version, plugin_repository_version, intellij_repository_version, intellij_repository)