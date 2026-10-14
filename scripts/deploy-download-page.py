"""Deploy public download page/admin controls with exact public-route allowlists; retain every other backend entry."""
import hashlib, json, os, shutil, subprocess, sys, tarfile, time, zipfile
from pathlib import Path
import yaml

ROOT=Path('/opt/aimessage')
TAG='download-page-20261015'
RELEASE=ROOT/'releases'/TAG
BACKUP=ROOT/'backups'/('pre-'+TAG)
COMPOSE=ROOT/'compose.yaml'
WEB=Path('/var/www/aimessage/current')
WEB_NEXT=Path('/var/www/aimessage/releases')/TAG/'dist'
IMAGE='aimessage-backend:'+TAG
CONFIG='BOOT-INF/classes/application.yml'
PREFIX='BOOT-INF/classes/org/springblade/modules/smartreminder/'
ENTRIES={PREFIX+name+'.class' for name in ('config/DownloadPageSchema','service/DownloadPageService','service/DownloadPageService$Settings','controller/DownloadPageController')}
PUBLIC=['/app/download/config','/app/download/qr','/app/download/android.apk']
SAVE='/blade-smart/admin/releases/download-page/save'

def run(args):
    p=subprocess.run(args,capture_output=True)
    if p.returncode:raise RuntimeError('Command failed: '+args[0]+' (output withheld)')
    return p.stdout
def compose(*args,path=COMPOSE):return run(['docker','compose','--env-file',str(ROOT/'.env'),'-f',str(path),*args])
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def state():return json.loads((BACKUP/'state.json').read_text())
def live():return json.loads(run(['docker','inspect','aimessage-backend']))[0]
def require_space(gib):
    assert shutil.disk_usage(ROOT).free>=gib*1024**3,'Insufficient free disk space; deployment not started'

def prepare():
    require_space(2)
    assert not RELEASE.exists() and not BACKUP.exists() and not WEB_NEXT.exists()
    assert WEB.is_symlink() and WEB.resolve().is_relative_to(Path('/var/www/aimessage/releases'))
    config=json.loads(compose('config','--format','json'));current=live()
    old=Path(config['services']['backend']['build']['context'])/'blade-api.jar'
    assert current['State']['Running'] and old.is_file() and old.resolve().is_relative_to(ROOT)
    RELEASE.mkdir();(RELEASE/'backend').mkdir(mode=0o700);BACKUP.mkdir(mode=0o700)
    shutil.copy2(COMPOSE,BACKUP/'compose.yaml')
    (BACKUP/'state.json').write_text(json.dumps({'image':current['Config']['Image'],'imageId':current['Image'],
        'composeSha':sha(COMPOSE),'jar':str(old),'jarSha':sha(old),'web':str(WEB.resolve()),'webIndexSha':sha(WEB/'index.html')}))
    print('Prepared isolated backend/web release and prior deployment backup',flush=True)

def revised_config(content):
    before=yaml.safe_load(content);after=json.loads(json.dumps(before))
    assert not any(path in before['blade']['secure']['skip-url'] for path in PUBLIC)
    assert SAVE not in before['blade']['xss']['skip-url']
    output=[];section=None
    lines=content.splitlines(keepends=True)
    for index,line in enumerate(lines):
        if line.strip() in ('secure:','xss:'):section=line.strip()[:-1]
        output.append(line)
        if section and line.strip()=='skip-url:':
            paths=PUBLIC if section=='secure' else [SAVE]
            item=next(l for l in lines[index+1:] if l.strip() and not l.lstrip().startswith('#'))
            assert item.lstrip().startswith('- ')
            indent=item[:len(item)-len(item.lstrip())]
            output.extend(indent+'- '+p+'\n' for p in paths)
            section=None
    result=''.join(output)
    after['blade']['secure']['skip-url']=PUBLIC+after['blade']['secure']['skip-url']
    after['blade']['xss']['skip-url']=[SAVE]+after['blade']['xss']['skip-url']
    assert yaml.safe_load(result)==after,'Unexpected runtime configuration change'
    return result.encode()

def build():
    saved=state();old=Path(saved['jar'])
    assert sha(COMPOSE)==saved['composeSha'] and sha(old)==saved['jarSha']
    jar=RELEASE/'backend/blade-api.jar'
    with zipfile.ZipFile(RELEASE/'patch.zip') as patch,zipfile.ZipFile(old) as original,zipfile.ZipFile(jar,'w') as target:
        assert set(patch.namelist())==ENTRIES
        replacement={name:patch.read(name) for name in ENTRIES}
        replacement[CONFIG]=revised_config(original.read(CONFIG).decode('utf8'))
        for item in original.infolist():
            if item.filename in replacement:target.writestr(item,replacement[item.filename])
            else:
                with original.open(item) as src,target.open(item,'w') as dst:shutil.copyfileobj(src,dst)
        for name in ENTRIES-set(original.namelist()):target.writestr(name,replacement[name])
    with zipfile.ZipFile(jar) as after,zipfile.ZipFile(old) as before:
        assert set(after.namelist())==set(before.namelist())|ENTRIES
        assert all(after.read(n)==before.read(n) for n in before.namelist() if n not in ENTRIES|{CONFIG})
    jar.chmod(0o644)
    (RELEASE/'backend/Dockerfile').write_text('FROM '+saved['image']+'\nCOPY blade-api.jar /app/blade-api.jar\n')
    run(['docker','build','--network=none','-t',IMAGE,str(RELEASE/'backend')])
    WEB_NEXT.mkdir(parents=True,mode=0o755)
    with tarfile.open(RELEASE/'web.tar.gz','r:gz') as archive:
        for member in archive.getmembers():
            dest=(WEB_NEXT/member.name).resolve()
            assert dest.is_relative_to(WEB_NEXT.resolve()) and member.isfile() and not member.issym() and not member.islnk()
            dest.parent.mkdir(parents=True,exist_ok=True,mode=0o755)
            with archive.extractfile(member) as src,open(dest,'wb') as dst:shutil.copyfileobj(src,dst)
            dest.chmod(0o644)
    assert (WEB_NEXT/'download/index.html').is_file() and (WEB_NEXT/'index.html').is_file()
    # Keep cached clients' old lazy chunks available while the entry point moves to the new build.
    for src in (Path(saved['web'])/'assets').rglob('*'):
        if src.is_file():
            dst=WEB_NEXT/'assets'/src.relative_to(Path(saved['web'])/'assets')
            if not dst.exists():dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);dst.chmod(0o644)
    raw=yaml.safe_load(COMPOSE.read_text());raw['services']['backend']['image']=IMAGE;raw['services']['backend']['build']['context']=str(RELEASE/'backend')
    candidate=RELEASE/'compose.yaml';candidate.write_text(yaml.safe_dump(raw,sort_keys=False))
    before=json.loads(compose('config','--format','json'));after=json.loads(compose('config','--format','json',path=candidate))
    for key in ['image','build']:after['services']['backend'][key]=before['services']['backend'][key]
    assert before==after,'Unexpected compose change'
    (RELEASE/'verified.json').write_text(json.dumps({'jar':sha(jar),'compose':sha(candidate),'web':sha(WEB_NEXT/'index.html'),'landing':sha(WEB_NEXT/'download/index.html')}))
    print('Verified scoped classes, exact allowlists, existing configuration and public/admin web build',flush=True)

def web_switch(target):
    assert target.resolve().is_relative_to(Path('/var/www/aimessage/releases'))
    temp=WEB.parent/('current-'+TAG+'.tmp');assert not temp.exists()
    temp.symlink_to(target);os.replace(temp,WEB)

def restore():
    web_switch(Path(state()['web']));shutil.copy2(BACKUP/'compose.yaml',COMPOSE)
    compose('up','-d','--no-build','--no-deps','backend')
    print('Prior web/backend restored; additive download configuration table retained',flush=True)

def cutover():
    require_space(1)
    assert live()['State'].get('Health',{}).get('Status')=='healthy','Current backend must be healthy before cutover'
    saved=state();verified=json.loads((RELEASE/'verified.json').read_text())
    assert sha(COMPOSE)==saved['composeSha'] and live()['Image']==saved['imageId'] and str(WEB.resolve())==saved['web']
    assert sha(RELEASE/'backend/blade-api.jar')==verified['jar'] and sha(RELEASE/'compose.yaml')==verified['compose']
    assert sha(WEB_NEXT/'index.html')==verified['web'] and sha(WEB_NEXT/'download/index.html')==verified['landing']
    shutil.copy2(RELEASE/'compose.yaml',COMPOSE)
    try:
        compose('up','-d','--no-build','--no-deps','backend');print('Waiting for new backend readiness',flush=True)
        for _ in range(90):
            if live()['State'].get('Health',{}).get('Status')=='healthy':
                web_switch(WEB_NEXT)
                print('Download page and admin controls live; backend healthy',flush=True);return
            time.sleep(2)
        raise RuntimeError('Backend readiness timed out')
    except Exception:restore();raise

def rollback():
    assert live()['Config']['Image']==IMAGE
    restore()
if __name__=='__main__':
    os.umask(0o022)
    assert run(['hostname']).decode().strip()=='iZuf6dadmqz37mvl5bobysZ'
    {'prepare':prepare,'build':build,'cutover':cutover,'rollback':rollback}[sys.argv[1]]()
