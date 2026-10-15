"""Probe only synthetic fixtures. Credentials enter through a hidden prompt, never files."""
import base64, getpass, json, time, urllib.request, urllib.error
from pathlib import Path

key = getpass.getpass('Speech API key (hidden): ')
url = 'https://llm-h5ykpx0f0r7le1by.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation'
for extension in ['wav', 'mp3']:
    audio = Path('output/voice-fixture-zh.' + extension).read_bytes()
    mime = 'audio/wav' if extension == 'wav' else 'audio/mpeg'
    payload = {'model': 'fun-asr-flash-2026-06-15', 'input': {'messages': [{'role': 'user', 'content': [{'type': 'input_audio', 'input_audio': {'data': 'data:' + mime + ';base64,' + base64.b64encode(audio).decode()}}]}]}, 'parameters': {'format': extension}}
    started = time.monotonic()
    request = urllib.request.Request(url, data=json.dumps(payload).encode(), headers={'Authorization': 'Bearer ' + key, 'Content-Type': 'application/json', 'X-DashScope-SSE': 'disable'})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            result = json.loads(response.read())
            print(json.dumps({'format': extension, 'seconds': round(time.monotonic()-started,2), 'result': result}, ensure_ascii=False).replace(key, '[REDACTED]'), flush=True)
    except urllib.error.HTTPError as error:
        result = json.loads(error.read())
        print(json.dumps({'format': extension, 'status': error.code, 'code': result.get('code'), 'message': result.get('message')}, ensure_ascii=False).replace(key, '[REDACTED]'), flush=True)
