"""Scoped chat status/calendar release; no schema migration or user-data rewrites."""
import importlib.util
import json
import os
from pathlib import Path
import shutil
import sys

spec=importlib.util.spec_from_file_location('release_base',Path(__file__).with_name('public-profile-polish-deploy.py'))
r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
r.TAG='chat-calendar-20260916'
r.RELEASE=r.ROOT/'releases'/r.TAG
r.BACKUP=r.ROOT/'backups'/('pre-'+r.TAG)
r.IMAGE='aimessage-backend:'+r.TAG

def preflight():
    active=r.sql("SELECT COUNT(*) FROM blade_app_chat_job WHERE job_status IN ('QUEUED','RUNNING') AND created_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    print(json.dumps({'active_chat_jobs':int(active)}),flush=True)
    return active

def build():
    if r.sha(r.RELEASE/'backend/blade-api.jar')!=sys.argv[2] or r.sha(r.RELEASE/'web.tar.gz')!=sys.argv[3]:
        raise RuntimeError('Uploaded artifact hash mismatch')
    r.build()

def cutover():
    if preflight()!='0':raise RuntimeError('Active chat jobs: wait for completion before restarting backend')
    # No database migration is needed. Only verify the required existing table.
    if r.sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='blade_app_chat_job'")!='8':raise RuntimeError('Unexpected chat-job schema')
    (r.RELEASE/'migration-complete').write_text('Existing schema verified; no DDL executed.')
    r.cutover()

def publish():
    old=Path(r.state()['old_web'])/'assets';target=r.RELEASE/'web/dist/assets'
    if not old.resolve().is_relative_to(r.ROOT/'releases'):raise RuntimeError('Unexpected old assets path')
    for source in old.rglob('*'):
        if source.is_symlink():raise RuntimeError('Unexpected old asset symlink')
        if source.is_file():
            destination=target/source.relative_to(old)
            if not destination.exists():destination.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,destination)
    r.run(['nginx','-t'])
    r.publish_web()

if __name__=='__main__':
    os.umask(0o077)
    try:{'prepare':r.prepare,'preflight':preflight,'build':build,'cutover':cutover,'publish_web':publish,'status':r.status,'rollback':r.rollback}[sys.argv[1]]()
    except Exception as error:print(str(error),file=sys.stderr);sys.exit(1)
