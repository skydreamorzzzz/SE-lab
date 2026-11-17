"""Count nonblank, non-comment Java source lines; includes braces, excludes HTML/CSS and generated outputs."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]

def count(directory):
    files = list(directory.rglob('*.java'))
    total = 0
    for source in files:
        text = source.read_text(encoding='utf-8')
        # Preserve quoted Java strings so URL literals are not mistaken for comments.
        tokens = re.compile(r'("(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\')|/\*.*?\*/|//[^\n]*', re.S)
        code = tokens.sub(lambda match: match.group(1) or '', text)
        total += sum(bool(line.strip()) for line in code.splitlines())
    return len(files), total

for label, folder in [('main', 'src/main/java'), ('test', 'src/test/java')]:
    files, lines = count(root / folder)
    print(f'{label}: {files} Java files, {lines} nonblank non-comment lines')
