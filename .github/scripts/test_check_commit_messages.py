"""Exercise the actual CI validator with temporary Git histories."""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


VALIDATOR = Path(__file__).with_name("check_commit_messages.py").resolve()


class CommitConventionTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="mottainai-commit-test-")
        self.root = Path(temporary.name).resolve()
        self.assertEqual(self.root.parent, Path(tempfile.gettempdir()).resolve())
        self.addCleanup(temporary.cleanup)
        self.env = os.environ.copy()
        for name in ("GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE"):
            self.env.pop(name, None)
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.name", "CI Test")
        self.git("config", "user.email", "ci@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        self.git("config", "core.hooksPath", str(self.root / "no-hooks"))
        self.base = self.commit("Historical message outside the current convention")

    def git(self, *arguments):
        result = subprocess.run(
            ["git", *arguments], cwd=self.root, env=self.env,
            text=True, encoding="utf-8", stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, check=False,
        )
        self.assertEqual(result.returncode, 0, result.stdout)
        return result.stdout.strip()

    def commit(self, subject):
        self.git("commit", "-q", "--allow-empty", "-m", subject)
        return self.git("rev-parse", "HEAD")

    def check(self, base, head):
        return subprocess.run(
            [sys.executable, str(VALIDATOR), base, head],
            cwd=self.root, env=self.env, text=True, encoding="utf-8",
            capture_output=True, check=False,
        )

    def merge(self, subject):
        self.git("switch", "-q", "-c", "feature")
        self.commit(subject)
        self.git("switch", "-q", "main")
        self.commit("fix(profile): Preserve current data")
        self.git("merge", "-q", "--no-ff", "feature", "-m", "Merge pull request #999")
        return self.git("rev-parse", "HEAD")

    def test_accepts_scopes_without_rechecking_historical_messages(self):
        head = self.commit("fix(auth): Recover expired sessions")
        self.assertEqual(self.check(self.base, head).returncode, 0)

    def test_rejects_invalid_incoming_subject(self):
        result = self.check(self.base, self.commit("Update login"))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Update login", result.stdout)

    def test_ignores_merge_subject_but_validates_its_commits(self):
        self.assertEqual(self.check(self.base, self.merge("feat: Add search")).returncode, 0)

    def test_rejects_invalid_subject_inside_a_merge(self):
        result = self.check(self.base, self.merge("Update catalog"))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Update catalog", result.stdout)

    def test_accepts_an_empty_range(self):
        self.assertEqual(self.check(self.base, self.base).returncode, 0)

    def test_accepts_first_push_with_a_zero_base(self):
        head = self.commit("docs(client): Initialize branch")
        self.assertEqual(self.check("0" * 40, head).returncode, 0)

    def test_rejects_invalid_subject_on_first_push(self):
        head = self.commit("Update initial branch")
        self.assertNotEqual(self.check("0" * 40, head).returncode, 0)


if __name__ == "__main__":
    unittest.main()
