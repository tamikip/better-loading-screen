"""Install the exact artifact verified in the isolated client."""
from pathlib import Path
import hashlib, json, shutil, sys, time, zipfile
from build import ROOT, VERSION

jar = ROOT / f'build/libs/teleport-loading-neoforge-1.21.1-{VERSION}.jar'
runtime = Path(sys.argv[1]).resolve()
report = json.loads((ROOT / 'output/client-validation.json').read_text())
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
if report.get('status') != 'passed' or digest(jar) != digest(runtime / 'mods' / jar.name):
    raise SystemExit('Artifact differs from verified client artifact')
with zipfile.ZipFile(jar) as archive:
    if any('loadingverify' in name or name.endswith('Test.class') for name in archive.namelist()):
        raise SystemExit('Test code must not ship')
target = ROOT.parent / 'mods' / jar.name
for previous in target.parent.glob('teleport-loading-neoforge-*.jar'):
    if previous.name == target.name: continue
    backup = ROOT.parent / 'mod_backups' / ('teleport-loading-' + str(time.time_ns()))
    backup.mkdir(parents=True)
    destination = backup / previous.name
    if not previous.resolve().is_relative_to((ROOT.parent / 'mods').resolve()) or not destination.resolve().is_relative_to((ROOT.parent / 'mod_backups').resolve()):
        raise SystemExit('Backup outside instance folders')
    shutil.move(str(previous), str(destination))
if target.exists() and digest(target) != digest(jar):
    backup = ROOT.parent / 'mod_backups' / ('teleport-loading-' + str(time.time_ns()))
    backup.mkdir(parents=True)
    shutil.copy2(target, backup / target.name)
shutil.copy2(jar, target)
if digest(target) != digest(jar): raise SystemExit('Installed checksum mismatch')
report.update({'installed': str(target), 'sha256': digest(jar), 'restart_required': True})
(ROOT / 'output/install-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=True))
