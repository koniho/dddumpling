#!/usr/bin/env python3
"""Build the existing pure Java game for browsers; no Android/iOS packaging or uploads."""
import argparse, hashlib, os, re, shutil, subprocess, sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
os.chdir(root)
p=argparse.ArgumentParser();p.add_argument('--maven',default=os.environ.get('MVN','mvn'));p.add_argument('--skip-audio',action='store_true');p.add_argument('--production',action='store_true');a=p.parse_args()
sources=re.search(r'PURE="(.*?)"',Path('check.sh').read_text(),re.S).group(1).split()
src=Path('build/web-src');shutil.rmtree(src,ignore_errors=True);src.mkdir(parents=True)
for f in sources+[str(f) for f in Path('web/java').rglob('*.java')]+['ios/java/com/dddumpling/game/IOSGame.java','ios/java/com/dddumpling/game/IOSTouch.java']:
 target=src/'com/dddumpling/game'/Path(f).name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,target)
env=dict(os.environ,DDDUMPLING_BUILD_ID='web-dev-1' if not a.production else 'web-1')
subprocess.run(['sh','tools/build-flags.sh',str(src),'false' if a.production else 'true'],env=env,check=True)
subprocess.run([a.maven,'-B','-q','-f','web/pom.xml','-Dmaven.repo.local='+str(root/'build/web-tools/m2'),'package'],check=True)
subprocess.run(['node','--check','build/web/game.js'],check=True)
subprocess.run(['node','--test','web/test/host.test.cjs'],check=True)
if not a.production:
 classes=Path('build/web-tests');classes.mkdir(exist_ok=True)
 cp=str(root/'build/web-maven/classes')
 subprocess.run(['javac','-cp',cp,'-d',str(classes),'tools/Check.java','tools/Font.java','web/test/WebPointerTest.java'],check=True)
 subprocess.run(['java','-cp',str(classes)+os.pathsep+cp,'com.dddumpling.game.WebPointerTest'],check=True)
out=Path('build/web');shutil.copytree('web/public',out,dirs_exist_ok=True)
(out/'fonts').mkdir(exist_ok=True)
for f in ['Bungee-Regular.ttf','Bungee-OFL.txt']:shutil.copyfile(Path('assets/fonts')/f,out/'fonts'/f)
# Audio is rendered off-device at build time, never synthesized in a frame callback.
if not a.skip_audio:
 fingerprint=hashlib.sha256(b''.join(Path(f).read_bytes() for f in sources)+Path('web/tools/WebAssets.java').read_bytes()).hexdigest()
 stamp=Path('build/web-audio.hash')
 if not stamp.exists() or stamp.read_text()!=fingerprint or not (out/'audio/manifest.json').exists():
  classes=Path('build/web-assets');classes.mkdir(exist_ok=True)
  subprocess.run(['javac','-d',str(classes),*sources,str(src/'com/dddumpling/game/BuildFlags.java'),'web/tools/WebAssets.java'],check=True)
  subprocess.run(['java','-Xmx768m','-cp',str(classes),'com.dddumpling.game.WebAssets',str(out/'audio')],check=True)
  stamp.write_text(fingerprint)
print('Browser build: build/web — serve with python3 -m http.server 8766 --bind 127.0.0.1 --directory build/web')
