"""Launch a real Minecraft 1.20.1 / Forge 47.4.28 client against the QA server.

Environment:
  QA_ROOT      game directory of the client instance
  QA_USERNAME  offline username (also the evidence prefix)
  QA_EVIDENCE  directory for console log / pid
  QA_SERVER    host:port of the dedicated server
"""
from pathlib import Path
import os
import subprocess
import sys
import time

from minecraft_launcher_lib import command

ROOT = Path(os.environ.get('QA_ROOT', '/home/ubuntu/minecraft-tests/berserk-struggler-1.20.1-forge-47.4.28'))
EVIDENCE = Path(os.environ.get('QA_EVIDENCE', '/home/ubuntu/midland-guard-refinement/evidence'))
VERSION = '1.20.1-forge-47.4.28'
JAVA = '/usr/lib/jvm/java-17-openjdk-amd64/bin/java'
SERVER = os.environ.get('QA_SERVER', '127.0.0.1:25565')
USERNAME = os.environ.get('QA_USERNAME', 'RefineQA')

jars = sorted((ROOT / 'mods').glob('struggler-*.jar'))
if not jars:
    raise FileNotFoundError('no struggler jar in ' + str(ROOT / 'mods'))
JAR = jars[-1]
EVIDENCE.mkdir(parents=True, exist_ok=True)

options = {
    'username': USERNAME,
    'uuid': 'b0d1c2e3f4a5b6c7d8e9f0a1b2c3d4e5',
    'token': '0',
    'executablePath': JAVA,
    'defaultExecutablePath': JAVA,
    'jvmArguments': ['-Xms512M', '-Xmx2G', '-Djava.awt.headless=false'],
    'launcherName': 'Isolated Forge Refinement QA',
    'launcherVersion': '1.0',
    'gameDirectory': str(ROOT),
    'customResolution': True,
    'resolutionWidth': '1280',
    'resolutionHeight': '720',
    'quickPlayMultiplayer': SERVER,
    'quickPlayPath': str(ROOT / 'quickPlay' / 'refine-qa.json'),
}
cmd = command.get_minecraft_command(VERSION, str(ROOT), options)
print(f'[launch] {USERNAME} -> {SERVER} with {JAR.name} from {ROOT}', flush=True)
with (EVIDENCE / f'console-{USERNAME}.log').open('w') as log:
    proc = subprocess.Popen(cmd, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, env=os.environ.copy())
print(f'[launch] {USERNAME} PID={proc.pid}; DISPLAY={os.environ.get("DISPLAY")}', flush=True)
(EVIDENCE / f'client-{USERNAME}.pid').write_text(str(proc.pid) + '\n')
while proc.poll() is None:
    time.sleep(10)
print(f'[launch] {USERNAME} exit={proc.returncode}', flush=True)
(EVIDENCE / f'exit-{USERNAME}.txt').write_text(str(proc.returncode) + '\n')
sys.exit(proc.returncode or 0)
