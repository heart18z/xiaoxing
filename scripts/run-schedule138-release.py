"""One hidden SSH prompt, bounded remote steps and visible progress; no credentials in files."""
import getpass, paramiko, sys
from pathlib import Path
mode=sys.argv[1]
password=getpass.getpass('SSH password (hidden): ')
client=paramiko.SSHClient();client.load_system_host_keys();client.set_missing_host_key_policy(paramiko.RejectPolicy())
client.connect('47.100.172.149',username='root',password=password,look_for_keys=False,allow_agent=False,timeout=15,auth_timeout=15,banner_timeout=15)
client.get_transport().set_keepalive(20)
def command(text,timeout=180):
    print('Running: '+text,flush=True)
    _,stdout,stderr=client.exec_command('timeout '+str(timeout)+' '+text,timeout=timeout+20)
    for line in stdout: print(line.rstrip(),flush=True)
    if stdout.channel.recv_exit_status(): raise RuntimeError('Remote step failed: '+stderr.read().decode(errors='replace')[-1000:].replace(password,'[REDACTED]'))
try:
    with client.open_sftp() as sftp:
        for name in ['deploy-schedule138.py','deploy-applications134.py','probe-schedule138.py']:
            sftp.put(str(Path('scripts')/name),'/opt/aimessage/'+name)
        sftp.put('output/schedule138-fixture.json','/opt/aimessage/schedule138-fixture.json')
    if mode in ('probe','release'):command('python3 /opt/aimessage/probe-schedule138.py',110)
    if mode=='release':
        command('python3 /opt/aimessage/deploy-schedule138.py prepare')
        with client.open_sftp() as sftp:sftp.put('output/schedule138-patch.zip','/opt/aimessage/releases/schedule138-20261015/patch.zip')
        command('python3 /opt/aimessage/deploy-schedule138.py build',240)
        command('python3 /opt/aimessage/deploy-schedule138.py cutover',240)
    elif mode=='rollback':command('python3 /opt/aimessage/deploy-schedule138.py rollback')
    elif mode!='probe':raise ValueError('Unknown operation')
finally:client.close()
