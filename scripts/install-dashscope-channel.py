"""Server-side additive channel setup. Secret is read from stdin and encrypted in storage."""
import base64, hashlib, json, os, secrets, subprocess, sys, time, zipfile
from pathlib import Path
import yaml
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

def run(args): return subprocess.check_output(args,stderr=subprocess.DEVNULL)
def sql(query):
    # Credentials stay in the container; SQL is supplied through stdin, not the command line.
    p=subprocess.run(['docker','exec','-i','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot -N -B ai_message_reminder'],input=query.encode(),capture_output=True)
    if p.returncode:
        detail=p.stderr.decode(errors='replace').replace(key,'[REDACTED]')
        raise RuntimeError('Database operation failed: '+detail[:700])
    return p.stdout.decode().strip()
def quote(value): return "(CONVERT(0x"+value.encode().hex()+" USING utf8mb4) COLLATE utf8mb4_general_ci)"

if __name__=='__main__':
    os.umask(0o077)
    key=sys.stdin.readline().strip()
    assert key.startswith('sk-')
    model='fun-asr-flash-2026-06-15'
    exists=sql("select count(*) from blade_ai_service_config where config_type='SPEECH' and is_deleted=0 and model_name="+quote(model))
    if exists!='0': raise RuntimeError('Channel already exists; no overwrite performed')
    live=json.loads(run(['docker','inspect','aimessage-backend']))[0]
    env=dict(value.split('=',1) for value in live['Config']['Env'])
    compose=yaml.safe_load(Path('/opt/aimessage/compose.yaml').read_text())
    jar=Path(compose['services']['backend']['build']['context'])/'blade-api.jar'
    with zipfile.ZipFile(jar) as archive: config=yaml.safe_load(archive.read('BOOT-INF/classes/application.yml'))
    sign=env.get('BLADE_TOKEN_SIGN_KEY') or config['blade']['token']['sign-key']
    nonce=secrets.token_bytes(12)
    cipher='v1:'+base64.b64encode(nonce+AESGCM(hashlib.sha256(sign.encode()).digest()).encrypt(nonce,key.encode(),None)).decode()
    channel_id=str(int(time.time()*1000)*1000+secrets.randbelow(1000))
    url='https://llm-h5ykpx0f0r7le1by.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation'
    defaults=sql("select id from blade_ai_service_config where config_type='SPEECH' and system_default=1 and is_deleted=0 order by id")
    sql("insert into blade_ai_service_config(id,config_name,model_alias,config_type,base_url,api_key,model_name,context_window,max_input_tokens,max_tokens,temperature,reasoning_effort,show_thinking,request_timeout,enabled,system_default,extra_body,create_time,update_time,is_deleted) values("+channel_id+","+quote('阿里云 Fun-ASR Flash')+","+quote('Fun-ASR Flash（试用）')+",'SPEECH',"+quote(url)+","+quote(cipher)+","+quote(model)+",1048576,991000,131072,0.3,'xhigh',0,60000,1,0,'{}',now(),now(),0);")
    assert sql("select id from blade_ai_service_config where config_type='SPEECH' and system_default=1 and is_deleted=0 order by id")==defaults
    assert sql("select count(*) from blade_ai_service_config where id="+channel_id+" and api_key like 'v1:%' and enabled=1 and system_default=0")=='1'
    Path('/opt/aimessage/releases/mobile137-20261015/speech-channel.json').write_text(json.dumps({'id':channel_id,'model':model,'defaultChanged':False}))
    print(json.dumps({'added':True,'model':model,'encrypted':True,'defaultChanged':False}))
