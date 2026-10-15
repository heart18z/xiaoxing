"""Operator transport. SSH and speech secrets are hidden prompts, not command arguments."""
import getpass, paramiko, sys
from pathlib import Path
client=paramiko.SSHClient();client.load_system_host_keys();client.set_missing_host_key_policy(paramiko.RejectPolicy())
mode=sys.argv[1]
password=getpass.getpass('SSH password (hidden): ')
speech_key=getpass.getpass('Speech API key (hidden): ') if mode=='channel' else None
client.connect('47.100.172.149',username='root',password=password,look_for_keys=False,allow_agent=False,timeout=15)
client.get_transport().set_keepalive(20)
def command(value,secret=None):
    stdin,stdout,stderr=client.exec_command(value,timeout=600)
    if secret is not None:stdin.write(secret+'\n');stdin.flush();stdin.channel.shutdown_write()
    for line in stdout: print(line.rstrip(),flush=True)
    code=stdout.channel.recv_exit_status()
    if code:raise RuntimeError('Remote operation failed: '+str(code)+'; '+stderr.read().decode()[-1800:].replace(password,'[REDACTED]').replace(secret or '\0','[REDACTED]'))
try:
    with client.open_sftp() as sftp:
        for name in ['deploy-mobile137.py','deploy-applications134.py','install-dashscope-channel.py']:
            sftp.put(str(Path('scripts')/name),'/opt/aimessage/'+name)
    if mode=='prepare':
        command('python3 /opt/aimessage/deploy-mobile137.py prepare')
        with client.open_sftp() as sftp:sftp.put('output/mobile137-patch.zip','/opt/aimessage/releases/mobile137-20261015/patch.zip')
        command('python3 /opt/aimessage/deploy-mobile137.py build')
    elif mode in ['cutover','rollback']:command('python3 /opt/aimessage/deploy-mobile137.py '+mode)
    elif mode=='channel':command('timeout 90 python3 /opt/aimessage/install-dashscope-channel.py',speech_key)
    elif mode=='status':command("docker inspect --format '{{.State.Health.Status}} {{.Config.Image}}' aimessage-backend")
    else:raise ValueError('Unknown operation')
finally:client.close()
