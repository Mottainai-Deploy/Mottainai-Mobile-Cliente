"""Validate incoming non-merge commit subjects without rechecking older history."""

import re
import subprocess
import sys


PATTERN = re.compile(
    r"^(feat|fix|docs|style|refactor|test|chore|ci|perf|build|revert)"
    r"(\([A-Za-z0-9][A-Za-z0-9._/-]*\))?: .+"
)


def main(base, head):
    revision = [f"{base}..{head}"] if base.strip("0") else ["-1", head]
    subjects = subprocess.check_output(
        ["git", "log", "--no-merges", "--format=%s", *revision],
        text=True,
        encoding="utf-8",
    ).splitlines()
    invalid = [subject for subject in subjects if not PATTERN.match(subject)]
    for subject in invalid:
        print(f"Invalid commit subject: {subject}")
    if not invalid:
        print(f"Commit convention passed ({len(subjects)} incoming non-merge commits).")
    return bool(invalid)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: check_commit_messages.py BASE_SHA HEAD_SHA")
    sys.exit(main(*sys.argv[1:]))
