"""Reject new lint errors relative to the pinned, unmodified upstream report."""
from collections import Counter
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

def errors(path):
    root = ET.parse(path).getroot()
    assert root.tag == "issues", f"Unexpected report: {path}"
    result = Counter()
    for issue in root.findall("issue"):
        if issue.get("severity") not in ("Error", "Fatal"):
            continue
        locations = []
        for loc in issue.findall("location"):
            # AGP normally writes paths relative to the module; normalize runner roots.
            name = loc.get("file", "").replace("\\\\", "/")
            if "/app/" in name:
                name = name.split("/app/", 1)[1]
            if name.startswith("app/"):
                name = name[4:]
            locations.append(name)
        # Exclude line numbers: inserting the opt-in update guard moves existing lines.
        key = (issue.get("id"), issue.get("message"), issue.get("errorLine1"),
               tuple(sorted(locations)))
        result[key] += 1
    return result

baseline = errors(Path(sys.argv[1]))
print(f"Pinned upstream: {sum(baseline.values())} existing lint errors")
for path in sys.argv[2:]:
    current = errors(Path(path))
    added = current - baseline
    for key, count in sorted(current.items(), key=lambda pair: repr(pair[0])):
        print(f"{path}: {key[0]} ({count}): {key[1]} at {key[3]}")
    assert not added, f"New lint errors in {path}: {dict(added)}"
    print(f"{path}: {sum(current.values())} errors, no new errors")
