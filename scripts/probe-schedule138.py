"""Server-side semantic smoke test with synthetic schedules only; no real event mutations."""
import base64, hashlib, json, subprocess, sys, urllib.request, zipfile
from pathlib import Path
import yaml
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

def run(args): return subprocess.check_output(args,stderr=subprocess.DEVNULL,timeout=15)

try:
    env=dict(x.split('=',1) for x in json.loads(run(['docker','inspect','aimessage-backend']))[0]['Config']['Env'])
    compose=json.loads(run(['docker','compose','--env-file','/opt/aimessage/.env','-f','/opt/aimessage/compose.yaml','config','--format','json']))
    with zipfile.ZipFile(Path(compose['services']['backend']['build']['context'])/'blade-api.jar') as archive:
        cfg=yaml.safe_load(archive.read('BOOT-INF/classes/application.yml'))
    sign=env.get('BLADE_TOKEN_SIGN_KEY') or cfg['blade']['token']['sign-key']
    sql="SELECT JSON_OBJECT('url',base_url,'model',model_name,'key',api_key) FROM blade_ai_service_config WHERE config_type='LLM' AND enabled=1 AND is_deleted=0 ORDER BY system_default DESC,id LIMIT 1"
    config=json.loads(run(['docker','exec','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --default-character-set=utf8mb4 -uroot -N -B ai_message_reminder -e "$1"','probe',sql]))
    key=config['key']
    if key.startswith('v1:'):
        packed=base64.b64decode(key[3:]);key=AESGCM(hashlib.sha256(sign.encode()).digest()).decrypt(packed[:12],packed[12:],None).decode()
    fixture=json.loads(Path('/opt/aimessage/schedule138-fixture.json').read_text())
    body={'model':config['model'],'messages':[{'role':'system','content':fixture['prompt']},{'role':'user','content':json.dumps(fixture['input'],ensure_ascii=False)}],'stream':False,'max_tokens':4096}
    request=urllib.request.Request(config['url'],data=json.dumps(body).encode(),headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=90) as response: result=json.loads(response.read())
    answer=result['choices'][0]['message']['content'];start=answer.index('{');end=answer.rindex('}')+1
    conflicts=json.loads(answer[start:end])['conflicts']
    actual={(int(row['proposedIndex']),str(row['otherEventId'])) for row in conflicts}
    expected={tuple(pair) for pair in fixture['expected']}
    print(json.dumps({'syntheticCases':len(fixture['input']['proposed']),'passed':actual==expected,'missing':sorted(expected-actual),'unexpected':sorted(actual-expected)}),flush=True)
    if actual!=expected: sys.exit(1)
except Exception as error:
    print('Synthetic semantic probe failed: '+type(error).__name__,flush=True);sys.exit(1)
