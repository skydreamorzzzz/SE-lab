"""Check preserved Lab2 originals and the six directly reused CSS assets."""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--source", type=Path, default=ROOT.parent.parent / "过去的lab实验/lab2/实验2_241220086_唐天洋")
parser.add_argument("--baseline", type=Path, default=ROOT / "evidence/design/source-baseline.json")
parser.add_argument("--output", type=Path, default=ROOT / "target/design-assets.json")
args = parser.parse_args()
baseline = json.loads(args.baseline.read_text(encoding="utf-8"))
originals = []
for name, expected in baseline.items():
    file = args.source / name.replace("\\", "/")
    actual = hashlib.sha256(file.read_bytes()).hexdigest() if file.exists() else None
    originals.append({"file": name, "unchanged": expected == actual})


def normalized(css):
    return "\n".join(line.rstrip() for line in css.splitlines()).strip()


styles = []
for original, asset in [("主页", "home"), ("搜索页面", "search"), ("商品展示", "detail"),
                        ("出售商品", "edit"), ("用户资料", "profile"), ("管理员后台", "admin")]:
    html = (args.source / "UI界面" / (original + ".html")).read_text(encoding="utf-8")
    css = re.search(r"<style[^>]*>(.*?)</style>", html, re.S | re.I).group(1)
    runtime = (ROOT / "src/main/resources/static/css" / ("lab2-" + asset + ".css")).read_text(encoding="utf-8")
    styles.append({"source": original + ".html", "asset": "lab2-" + asset + ".css",
                   "allOriginalRulesPreserved": normalized(css) == normalized(runtime)})
result = {"passed": all(r["unchanged"] for r in originals) and all(r["allOriginalRulesPreserved"] for r in styles),
          "originalFiles": originals, "reusedStylesheets": styles,
          "method": "Original files: SHA-256; extracted full CSS: compare after normalizing line endings and trailing whitespace only"}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))
raise SystemExit(0 if result["passed"] else 1)
