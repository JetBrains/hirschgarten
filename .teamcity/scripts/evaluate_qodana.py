import requests
from datetime import datetime
import os
import re
import sys
from typing import Tuple, Optional
import argparse

def parse_arguments():
    parser = argparse.ArgumentParser(description='analyze qodana build logs with custom thresholds')
    parser.add_argument('--unchanged',
                        type=int,
                        required=False,
                        default=None,
                        help='expected number of unchanged problems')
    parser.add_argument('--diff',
                        type=int,
                        default=0,
                        help='allowed difference in unchanged problems (default: 0)')
    return parser.parse_args()


class TeamCityLogRetriever:
    def __init__(self, server_url: str, auth_token: str):
        self.server_url = server_url.rstrip('/')
        self.session = requests.Session()
        self.session.headers.update({
            'Authorization': f'Bearer {auth_token}',
            'Accept': 'text/plain'
        })

    def get_build_log(self, build_id: str) -> str:
        url = f"{self.server_url}/downloadBuildLog.html?buildId={build_id}"
        response = self.session.get(url)
        response.raise_for_status()
        return response.text

    def save_log_to_file(self, build_id: str, output_dir: str) -> str:
        log_content = self.get_build_log(build_id)
        os.makedirs(output_dir, exist_ok=True)

        timestamp = datetime.now().timestamp()
        filename = f"build_{build_id}_{timestamp}.log"
        filepath = os.path.join(output_dir, filename)

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(log_content)

        return filepath

class QodanaLogAnalyzer:
    def __init__(self, log_content: str):
        self.log_content = log_content

    def analyze_problem_counts(self) -> Tuple[Optional[int], Optional[int]]:
        """
        Analyzes the log content to find the UNCHANGED and NEW problem counts.
        Returns a tuple of (unchanged_count, new_count).
        """
        pattern = r'\[.*?\]\s*:\s*\[Code Inspection\] Grouping problems according to baseline: UNCHANGED: ([\d,]+), NEW: ([\d,]+)'
        match = re.search(pattern, self.log_content)


        if match:
            unchanged_count = int(match.group(1).replace(',', ''))
            new_count = int(match.group(2).replace(',', ''))
            return unchanged_count, new_count

        return None, None

def evaluate_qodana_results(log_content: str, expected_unchanged: Optional[int], allowed_diff: int) -> None:
    # check if "project contains no modules" is present
    if "project contains no modules" in log_content.lower():
        print("error: found 'project contains no modules' in the build log")
        sys.exit(1)

    analyzer = QodanaLogAnalyzer(log_content)
    unchanged_count, new_count = analyzer.analyze_problem_counts()

    if unchanged_count is None or new_count is None:
        print("Error: Could not find problem count information in the log")
        sys.exit(1)

    print(f"Analysis Results:")
    print(f"Unchanged Problems: {unchanged_count}")
    print(f"New Problems: {new_count}")

    # skip validation if --unchanged wasn't provided
    if expected_unchanged is not None:
        print(f"Checking against expected: {expected_unchanged} ±{allowed_diff}")
        if abs(unchanged_count - expected_unchanged) > allowed_diff:
            print(
                f"Error: Unexpected number of unchanged problems. Expected {expected_unchanged} ±{allowed_diff}, but found {unchanged_count}")
            sys.exit(1)

    print("Success: No new problems found" +
          (" and unchanged count within acceptable range" if expected_unchanged is not None else ""))
    sys.exit(0)

def main():
    args = parse_arguments()
    build_id = "%env.BUILD_URL%".split("/")[-1]
    server_url = "https://bazel.teamcity.com"
    auth_token_tc = "%jetbrains.bazel.teamcity.token%"

    tc_client = TeamCityLogRetriever(server_url, auth_token_tc)

    try:
        log_content = tc_client.get_build_log(build_id)
        evaluate_qodana_results(log_content, args.unchanged, args.diff)
    except requests.exceptions.RequestException as e:
        print(f"Error retrieving build log: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()