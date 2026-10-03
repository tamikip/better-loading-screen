"""Offline build against the installed matching NeoForge development runtime."""
from pathlib import Path
import json, os, subprocess, zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.2.1'
DEV = Path(os.environ.get('LOADING_DEV_JAR', 'F:/mc mod/ai/build/moddev/artifacts/neoforge-21.1.228.jar'))

def classpath():
    instance = json.loads((ROOT.parent / (ROOT.parent.name + '.json')).read_text(encoding='utf-8'))
    libraries = ROOT.parents[2] / 'libraries'
    cp = [DEV]
    for lib in instance['libraries']:
        relative = lib.get('downloads', {}).get('artifact', {}).get('path', '')
        if relative and (libraries / relative).is_file() and not relative.startswith('net/neoforged/neoforge/'):
            cp.append(libraries / relative)
    return list(dict.fromkeys(cp))

def args_file(path, args, encoding='utf-8'):
    path.write_text('\n'.join('"' + str(v).replace('\\', '/') + '"' for v in args), encoding=encoding)

def main():
    build = ROOT / 'build'
    classes = build / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    sources = sorted((ROOT / 'src/main/java').rglob('*.java'))
    tests = sorted((ROOT / 'src/test/java').rglob('*.java'))
    args_file(build / 'offline-compile.args', ['--release', '21', '-encoding', 'UTF-8', '-proc:none', '-classpath', os.pathsep.join(map(str, classpath())), '-d', classes] + sources + tests)
    subprocess.run(['javac', '@' + str(build / 'offline-compile.args')], check=True)
    subprocess.run(['java', '-cp', str(classes), 'com.tamikip.loading.LoadingStateTest'], check=True)
    subprocess.run(['java', '-cp', str(classes), 'com.tamikip.loading.LoadingStyleTest'], check=True)
    subprocess.run(['java', '-cp', str(classes), 'com.tamikip.loading.TeleportCommandTest'], check=True)
    subprocess.run(['java', '-cp', os.pathsep.join([str(classes)] + list(map(str, classpath()))), 'com.tamikip.loading.LoadingDefinitionTest'], check=True)
    jar = build / f'libs/teleport-loading-neoforge-1.21.1-{VERSION}.jar'
    jar.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as z:
        for name in ['LICENSE', 'NOTICE', 'THIRD_PARTY.md']:
            z.write(ROOT / name, name)
        for root in [classes, ROOT / 'src/main/resources']:
            for file in sorted(root.rglob('*')):
                if file.is_file() and not file.name.endswith('Test.class'):
                    z.write(file, file.relative_to(root).as_posix())
    print(json.dumps({'jar': str(jar), 'bytes': jar.stat().st_size}))

if __name__ == '__main__': main()
