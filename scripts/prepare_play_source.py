"""Extract the reviewed Android archive and apply the tracked release overrides."""
from pathlib import Path
import hashlib
import shutil
import stat
import zipfile

root = Path(__file__).resolve().parents[1]
archive = root / 'KRAVEIT_ANDROID_V1_3_FINAL.zip'
expected = '0f16f439ec51340962fceb640365d7eb1d6006d8f49bc0b8f3edfdd3af3b2cd9'
if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
    raise SystemExit('Source archive changed: review it and update the expected checksum.')
destination = root / 'play-src'
destination.mkdir(exist_ok=False)
with zipfile.ZipFile(archive) as z:
    for item in z.infolist():
        target = (destination / item.filename).resolve()
        if not target.is_relative_to(destination.resolve()):
            raise SystemExit('Unsafe archive path')
        if stat.S_ISLNK(item.external_attr >> 16):
            raise SystemExit('Archive symlinks are not allowed')
    z.extractall(destination)
projects = list(destination.rglob('settings.gradle.kts'))
if len(projects) != 1:
    raise SystemExit(f'Expected exactly one Android project, found {len(projects)}')
project = projects[0].parent
overrides = root / 'overrides'
for source in overrides.rglob('*'):
    if source.is_file():
        target = project / source.relative_to(overrides)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
(root / 'play-project-path.txt').write_text(str(project.relative_to(root)) + '\n')
print('Prepared Android project:', project.relative_to(root))
