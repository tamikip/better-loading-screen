"""Smoke-test optional dedicated-server installation with a real datapack."""
import shutil, subprocess, time
from build import ROOT, DEV, VERSION, classpath, args_file
from client_check import runtime_args

runtime = ROOT / ('build/server-runtime-' + str(time.time_ns()))
(runtime / 'mods').mkdir(parents=True)
jar = ROOT / f'build/libs/teleport-loading-neoforge-1.21.1-{VERSION}.jar'
shutil.copy2(jar, runtime / 'mods' / jar.name)
shutil.copytree(ROOT / 'examples/loading-data', runtime / 'world/datapacks/loading-data')
(runtime / 'eula.txt').write_text('eula=true\n')
(runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n')
cp = classpath() + [DEV.with_name('neoforge-21.1.228-client-extra-aka-minecraft-resources.jar')]
args = runtime_args(cp)
args[args.index('forgeclientuserdev')] = 'forgeserveruserdev'
args += ['--nogui']
args_file(runtime / 'launch.args', args, 'mbcs')
print('Server runtime: ' + str(runtime), flush=True)
logfile = runtime / 'console.log'
with logfile.open('w', encoding='utf-8') as log:
    process = subprocess.Popen(['java', '@' + str(runtime / 'launch.args')], cwd=runtime, stdin=subprocess.PIPE,
                               stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW, text=True)
    try:
        deadline = time.monotonic() + 150
        while time.monotonic() < deadline:
            text = logfile.read_text(encoding='utf-8', errors='replace')
            if 'Done (' in text:
                assert 'loading-data' in text, 'Example datapack was not discovered'
                process.communicate('stop\n', timeout=40)
                assert process.returncode == 0, 'Dedicated server failed on shutdown'
                print('PASS: dedicated server startup, datapack discovery and clean stop')
                break
            if process.poll() is not None: raise RuntimeError('Server exited before ready: ' + text[-5000:])
            time.sleep(1)
        else: raise TimeoutError('Dedicated server did not start')
    finally:
        if process.poll() is None: process.kill(); process.wait()
