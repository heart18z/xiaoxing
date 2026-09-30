"""Scoped production cookie fix. Run on the verified host; no credentials or database changes."""
import hashlib, json, os, shutil, subprocess, sys, time, zipfile
from pathlib import Path
import yaml

ROOT = Path('/opt/aimessage')
TAG = 'login-cookie-20260930'
RELEASE = ROOT / 'releases' / TAG
BACKUP = ROOT / 'backups' / ('pre-' + TAG)
COMPOSE = ROOT / 'compose.yaml'
IMAGE = 'aimessage-backend:' + TAG
OPTION = '--add-opens=java.base/java.lang=ALL-UNNAMED'
ENTRIES = {
    'BOOT-INF/classes/org/springblade/common/filter/CookieSecureFilter.class',
    'BOOT-INF/classes/org/springblade/modules/auth/handler/BackendCompatibilityExceptionHandler.class',
}

def run(args):
    p = subprocess.run(args, capture_output=True)
    if p.returncode:
        raise RuntimeError('Command failed: ' + args[0] + ' (details withheld)')
    return p.stdout

def compose(*args, path=COMPOSE):
    return run(['docker', 'compose', '--env-file', str(ROOT / '.env'), '-f', str(path), *args])

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def state():
    return json.loads((BACKUP / 'state.json').read_text())

def live():
    return json.loads(run(['docker', 'inspect', 'aimessage-backend']))[0]

def prepare():
    assert not RELEASE.exists() and not BACKUP.exists(), 'Release already exists'
    config = json.loads(compose('config', '--format', 'json'))
    current = live()
    assert current['State']['Running']
    old = Path(config['services']['backend']['build']['context']) / 'blade-api.jar'
    assert old.is_file() and old.resolve().is_relative_to(ROOT)
    RELEASE.mkdir(); (RELEASE / 'backend').mkdir(); BACKUP.mkdir()
    shutil.copy2(COMPOSE, BACKUP / 'compose.yaml')
    (BACKUP / 'state.json').write_text(json.dumps({
        'image': current['Config']['Image'], 'composeSha': sha(COMPOSE),
        'jar': str(old), 'jarSha': sha(old), 'oldImageId': current['Image'],
    }))
    print('Prepared configuration backup and isolated release', flush=True)

def build():
    saved = state(); old = Path(saved['jar'])
    assert sha(COMPOSE) == saved['composeSha'] and sha(old) == saved['jarSha']
    jar = RELEASE / 'backend/blade-api.jar'
    with zipfile.ZipFile(RELEASE / 'patch.zip') as patch, zipfile.ZipFile(old) as original, zipfile.ZipFile(jar, 'w') as target:
        assert set(patch.namelist()) == ENTRIES
        replacement = {name: patch.read(name) for name in ENTRIES}
        for item in original.infolist():
            if item.filename in replacement:
                target.writestr(item, replacement[item.filename])
            else:
                with original.open(item) as source, target.open(item, 'w') as dest:
                    shutil.copyfileobj(source, dest)
        for name in ENTRIES - set(original.namelist()):
            target.writestr(name, replacement[name])
    with zipfile.ZipFile(jar) as after, zipfile.ZipFile(old) as before:
        assert set(after.namelist()) == set(before.namelist()) | ENTRIES
        assert all(after.read(n) == before.read(n) for n in before.namelist() if n not in ENTRIES)
        assert all(after.read(n) == replacement[n] for n in ENTRIES)
    jar.chmod(0o644)
    (RELEASE / 'backend/Dockerfile').write_text('FROM ' + saved['image'] + '\nCOPY blade-api.jar /app/blade-api.jar\n')
    run(['docker', 'build', '--network=none', '-t', IMAGE, str(RELEASE / 'backend')])
    raw = yaml.safe_load(COMPOSE.read_text()); backend = raw['services']['backend']
    backend['image'] = IMAGE; backend['build']['context'] = str(RELEASE / 'backend')
    options = backend.setdefault('environment', {}).get('JAVA_OPTS', '')
    if OPTION not in options.split(): backend['environment']['JAVA_OPTS'] = options + ' ' + OPTION
    candidate = RELEASE / 'compose.yaml'; candidate.write_text(yaml.safe_dump(raw, sort_keys=False))
    before = json.loads(compose('config', '--format', 'json'))
    after = json.loads(compose('config', '--format', 'json', path=candidate))
    for key in ['image', 'build', 'environment']:
        if key == 'environment':
            assert after['services']['backend'][key].get('JAVA_OPTS', '').split().count(OPTION) == 1
            after['services']['backend'][key]['JAVA_OPTS'] = before['services']['backend'][key]['JAVA_OPTS']
        else: after['services']['backend'][key] = before['services']['backend'][key]
    assert before == after, 'Unexpected deployment config changes'
    (RELEASE / 'verified.json').write_text(json.dumps({'jar': sha(jar), 'compose': sha(candidate)}))
    print('Verified: only two classes and JVM compatibility option changed', flush=True)

def rollback():
    assert live()['Config']['Image'] == IMAGE
    shutil.copy2(BACKUP / 'compose.yaml', COMPOSE)
    compose('up', '-d', '--no-build', '--no-deps', 'backend')
    print('Restored prior backend and configuration; database untouched', flush=True)

def cutover():
    saved = state(); verified = json.loads((RELEASE / 'verified.json').read_text())
    assert sha(COMPOSE) == saved['composeSha'] and live()['Image'] == saved['oldImageId']
    assert sha(RELEASE / 'backend/blade-api.jar') == verified['jar']
    assert sha(RELEASE / 'compose.yaml') == verified['compose']
    shutil.copy2(RELEASE / 'compose.yaml', COMPOSE)
    try:
        compose('up', '-d', '--no-build', '--no-deps', 'backend')
        print('Backend restarting; waiting for readiness', flush=True)
        for _ in range(90):
            current = live()
            if current['State'].get('Health', {}).get('Status') == 'healthy':
                options = next(v for v in current['Config']['Env'] if v.startswith('JAVA_OPTS='))
                assert OPTION in options
                print('New backend healthy; JVM compatibility option active', flush=True)
                return
            time.sleep(2)
        raise RuntimeError('Backend readiness timed out')
    except Exception:
        shutil.copy2(BACKUP / 'compose.yaml', COMPOSE)
        compose('up', '-d', '--no-build', '--no-deps', 'backend')
        print('Automatic rollback performed', flush=True)
        raise

if __name__ == '__main__':
    os.umask(0o077)
    assert run(['hostname']).decode().strip() == 'iZuf6dadmqz37mvl5bobysZ'
    {'prepare': prepare, 'build': build, 'cutover': cutover, 'rollback': rollback}[sys.argv[1]]()
