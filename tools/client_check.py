"""Run real client teleport and dimension checks in a fresh isolated world."""
from pathlib import Path
import json, os, shutil, subprocess, sys, time, zipfile
from build import ROOT, DEV, VERSION, classpath, args_file

def runtime_args(cp):
    modules = [p for p in cp if any(s in p.as_posix() for s in ['/bootstraplauncher/', '/securejarhandler/', '/org/ow2/asm/', '/JarJarFileSystems/'])]
    legacy = os.pathsep.join(str(p) for p in cp if p not in modules)
    return ['-Xmx3G', '-DlegacyClassPath=' + legacy, '-DignoreList=neoforge-21.1.228.jar,client-extra',
        '-DlibraryDirectory=' + str(ROOT.parents[2] / 'libraries'), '-p', os.pathsep.join(map(str, modules)), '--add-modules', 'ALL-MODULE-PATH',
        '--add-opens', 'java.base/java.util.jar=cpw.mods.securejarhandler', '--add-opens', 'java.base/java.lang.invoke=cpw.mods.securejarhandler',
        '--add-exports', 'java.base/sun.security.util=cpw.mods.securejarhandler', '-cp', legacy, 'cpw.mods.bootstraplauncher.BootstrapLauncher',
        '--launchTarget', 'forgeclientuserdev', '--fml.neoForgeVersion', '21.1.228', '--fml.fmlVersion', '4.0.44', '--fml.mcVersion', '1.21.1', '--fml.neoFormVersion', '20240808.144430']

def main():
    runtime = ROOT / ('build/client-runtime-' + str(time.time_ns()))
    print('Runtime: ' + str(runtime), flush=True)
    runtime.mkdir(parents=True, exist_ok=True)
    (ROOT / 'output').mkdir(exist_ok=True)
    mods = runtime / 'mods'
    mods.mkdir(exist_ok=True)
    jar = ROOT / f'build/libs/teleport-loading-neoforge-1.21.1-{VERSION}.jar'
    shutil.copy2(jar, mods / jar.name)
    for dep in (ROOT.parent / 'mods').glob('*.jar'):
        if 'sodium-neoforge-' in dep.name or 'iris-neoforge-' in dep.name or 'ModernUI-' in dep.name: shutil.copy2(dep, mods / dep.name)
    cp = classpath() + [DEV.with_name('neoforge-21.1.228-client-extra-aka-minecraft-resources.jar')]
    classes = ROOT / 'build/client-check-classes'
    classes.mkdir(exist_ok=True)
    args_file(runtime / 'compile.args', ['--release', '21', '-encoding', 'UTF-8', '-proc:none', '-classpath', os.pathsep.join(map(str, cp + [jar])), '-d', classes,
        ROOT.parent / 'traveler_atlas/tools/PrepareWorld.java'] + list((ROOT / 'src/clientTest/java').rglob('*.java')))
    subprocess.run(['javac', '@' + str(runtime / 'compile.args')], check=True)
    world = runtime / 'saves/LoadingTest/level.dat'
    subprocess.run(['java', '-cp', os.pathsep.join(map(str, [classes] + cp)), 'PrepareWorld', str(ROOT.parent / 'emberforge_mod/build/runtime/world/level.dat'), str(world)], check=True)
    packs = '--packs' in sys.argv
    if packs:
        shutil.copytree(ROOT / 'examples/loading-resources', runtime / 'resourcepacks/loading-resources')
        datapack = world.parent / 'datapacks/loading-data'
        shutil.copytree(ROOT / 'examples/loading-data', datapack)
        definition = datapack / 'data/loading_example/loading_screen/screen.json'
        values = json.loads(definition.read_text(encoding='utf-8'))
        values['dimensions']['loading_client_check:other'] = values['dimensions']['example:moon']
        definition.write_text(json.dumps(values, ensure_ascii=False), encoding='utf-8')
    with zipfile.ZipFile(mods / 'loading-client-check.jar', 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/neoforge.mods.toml', 'modLoader="javafml"\nloaderVersion="[4,)"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="loading_client_check"\nversion="1"\ndisplayName="Loading verification"\n')
        for file in (classes / 'com/tamikip/loadingverify').rglob('*.class'): z.write(file, file.relative_to(classes).as_posix())
        z.writestr('data/loading_client_check/dimension/other.json', json.dumps({'type':'minecraft:overworld','generator':{'type':'minecraft:noise','settings':'minecraft:overworld','biome_source':{'type':'minecraft:multi_noise','preset':'minecraft:overworld'}}}))
    config = runtime / 'config'
    config.mkdir(exist_ok=True)
    if (ROOT.parent / 'config/ModernUI').exists(): shutil.copytree(ROOT.parent / 'config/ModernUI', config / 'ModernUI')
    (config / 'teleport_loading-client.toml').write_text('minimumDisplayMs = 2000\nstableRenderMs = 350\n', encoding='utf-8')
    base_args = runtime_args(cp)
    if packs: base_args.insert(0, '-Dloading.verifyPacks=true')
    args = base_args + ['--gameDir', '.', '--assetsDir', ROOT.parents[2] / 'assets', '--assetIndex', '17',
        '--username', 'LoadingPreview', '--uuid', '00000000000000000000000000000002', '--accessToken', '0', '--version', '1.21.1', '--userType', 'legacy',
        '--width', '1600', '--height', '900', '--quickPlaySingleplayer', 'LoadingTest']
    args_file(runtime / 'launch.args', args, 'mbcs')
    options = 'version:3955\nlang:zh_cn\nguiScale:2\nrenderDistance:6\nsimulationDistance:5\nmaxFps:60\ngamma:0.8\npauseOnLostFocus:false\nonboardAccessibility:false\ntutorialStep:none\n'
    if packs: options += 'resourcePacks:["vanilla","file/loading-resources"]\n'
    (runtime / 'options.txt').write_text(options, encoding='utf-8')
    with (runtime / 'console.log').open('w', encoding='utf-8') as log:
        subprocess.run(['java', '@' + str(runtime / 'launch.args')], cwd=runtime, stdout=log, stderr=subprocess.STDOUT, timeout=300, creationflags=subprocess.CREATE_NO_WINDOW, check=True)
    verify_screenshots()
    if packs: verify_custom_icon()
    verify_font(runtime, packs)
    print((ROOT / 'output/client-validation.json').read_text())

def verify_screenshots():
    from PIL import Image
    import numpy as np
    atlas = np.asarray(Image.open(ROOT / 'src/main/resources/assets/teleport_loading/textures/gui/elements.png').convert('RGBA'))
    for phase, night in [(1, False), (2, True), (3, True), (4, False), (5, False)]:
        shot = np.asarray(Image.open(ROOT / f'output/loading-phase-{phase}.png').convert('RGB'))
        h, w = shot.shape[:2]
        assert tuple(shot[20,20]) == ((28,27,33) if night else (255,255,255)), 'Wrong background palette'
        reference = min(w // 2, h // 2 * 2.24)
        rw = int(reference * 0.174) * 2
        rh = (int(reference * 0.174) // 3) * 2
        x = ((w // 2 - rw // 2) // 2) * 2
        line = h // 2 - int(reference * 0.038)
        y = (line - rh // 4) * 2
        mask = np.asarray(Image.fromarray(atlas[:,:,3]).resize((rw,rh), Image.Resampling.NEAREST)) > 220
        crop = shot[y:y+rh,x:x+rw].mean(axis=2)
        first = crop[:,:rw//7][mask[:,:rw//7]].mean()
        last = crop[:,rw*6//7:][mask[:,rw*6//7:]].mean()
        assert (first - last > 35) if night else (last - first > 20), f'Progress contrast lost in phase {phase}: {first}, {last}'
    print('PASS: actual screenshot day/night backgrounds and filled/unfilled icon contrast')

def verify_font(runtime, packs=False):
    log = (Path(runtime) / 'console.log').read_text(encoding='utf-8', errors='replace')
    font = 'loading_example:ui' if packs else 'teleport_loading:tips'
    assert "Loaded font: '" + font + "', font set: [HYWenHei-85W" in log, 'Requested font did not load in Modern UI'
    path = ROOT / 'output/client-validation.json'
    report = json.loads(path.read_text())
    for item in ['HYWenHei-85W loaded by Modern UI', 'pixel-verified day/night icon contrast']:
        if item not in report['checks']: report['checks'].append(item)
    if packs:
        report['checks'] += ['resource pack custom TTF and OGG event', 'resource reload', 'datapack initial sync and priority', 'datapack reload sync', 'custom dimension icon mapping']
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('PASS: requested HYWenHei-85W font loaded by Modern UI')

def verify_custom_icon():
    from PIL import Image
    import numpy as np
    shot = np.asarray(Image.open(ROOT / 'output/loading-phase-4.png').convert('RGB'))
    h, w = shot.shape[:2]
    reference = min(w // 2, h // 2 * 2.24)
    size = int(reference * 0.14)
    x = (w // 2 - size) // 2 * 2
    y = (h // 4 - int(reference * 0.033) - size // 2) * 2
    atlas = Image.open(ROOT / 'examples/loading-resources/assets/loading_example/textures/gui/dimensions.png').convert('RGBA')
    expected = np.asarray(atlas.crop((627, 0, 1254, 627)).resize((size * 2, size * 2), Image.Resampling.BILINEAR))[:, :, 3].astype(float)
    actual = 255 - shot[y:y+size*2, x:x+size*2].mean(axis=2)
    correlation = np.corrcoef(expected.flatten(), actual.flatten())[0, 1]
    assert correlation > 0.96, f'Custom dimension icon does not match resource-pack image: {correlation}'
    print('PASS: screenshot matches reloaded resource-pack dimension icon')

if __name__ == '__main__': main()
