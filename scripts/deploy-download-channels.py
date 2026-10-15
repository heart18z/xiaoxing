"""Scoped upgrade of existing download-channel settings; reuses the verified backup/rollback flow."""
import importlib.util
import os
import sys
from pathlib import Path

spec = importlib.util.spec_from_file_location('download_deploy', Path(__file__).with_name('deploy-download-page.py'))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)
deploy.TAG = 'download-channels-136-20261015'
deploy.RELEASE = deploy.ROOT / 'releases' / deploy.TAG
deploy.BACKUP = deploy.ROOT / 'backups' / ('pre-' + deploy.TAG)
deploy.WEB_NEXT = Path('/var/www/aimessage/releases') / deploy.TAG / 'dist'
deploy.IMAGE = 'aimessage-backend:' + deploy.TAG
deploy.ENTRIES = {deploy.PREFIX + name + '.class' for name in (
    'config/DownloadPageSchema', 'service/DownloadPageService', 'service/DownloadPageService$Settings')}

def existing_config(content):
    parsed = deploy.yaml.safe_load(content)
    assert all(path in parsed['blade']['secure']['skip-url'] for path in deploy.PUBLIC)
    assert deploy.SAVE in parsed['blade']['xss']['skip-url']
    # This release needs no new public routes or configuration exceptions.
    return content.encode('utf8')

deploy.revised_config = existing_config

if __name__ == '__main__':
    os.umask(0o022)
    assert deploy.run(['hostname']).decode().strip() == 'iZuf6dadmqz37mvl5bobysZ'
    {'prepare': deploy.prepare, 'build': deploy.build, 'cutover': deploy.cutover, 'rollback': deploy.rollback}[sys.argv[1]]()
