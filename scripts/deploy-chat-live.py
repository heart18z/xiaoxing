"""Chat live-observation release; reuse verified backup/cutover/rollback workflow."""
import importlib.util
import os
from pathlib import Path
import sys

spec = importlib.util.spec_from_file_location('chat_release', Path(__file__).with_name('deploy-chat-calendar.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)
r = release.r
r.TAG = 'chat-live-20260916'
r.RELEASE = r.ROOT / 'releases' / r.TAG
r.BACKUP = r.ROOT / 'backups' / ('pre-' + r.TAG)
r.IMAGE = 'aimessage-backend:' + r.TAG

if __name__ == '__main__':
    os.umask(0o077)
    try:
        {'prepare': r.prepare, 'preflight': release.preflight, 'build': release.build,
         'cutover': release.cutover, 'publish_web': release.publish,
         'status': r.status, 'rollback': r.rollback}[sys.argv[1]]()
    except Exception as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
