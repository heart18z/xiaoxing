"""Prepare synthetic model fixtures and a patch from already compiled conflict138 classes."""
from pathlib import Path
import re, json, zipfile

source=Path('code/back/src/main/java/org/springblade/modules/smartreminder/service/SmartScheduleService.java').read_text(encoding='utf-8')
rules=re.search(r'TIME_EVIDENCE_RULES = """(.*?)"""',source,re.S)[1]
prompt=re.search(r'REVIEW_PROMPT = """(.*?)"""',source,re.S)[1]+rules
pairs=[
    ('3月1日去福州出差','3月1日去西安出差',True),
    ('3月1日下午3点去厦大','3月1日去福州出差',True),
    ('3月1日到客户现场','3月1日去外地出差',True),
    ('3月1日16:00至16:30线上聊天','3月1日15:00至15:30线上会议',False),
    ('3月1日16点参加另外一场会议','3月1日15点开项目会议，未说明结束时间',True),
    ('3月1日15点同一场项目会议的再次转告','3月1日15点项目会议',False),
    ('3月2日去福州出差','3月1日去西安出差，1日当天返回',False),
    ('3月1日提交报告，只有当日截止要求，无固定占用时段','3月1日15:00至16:00开会',False),
]
proposed=[];existing=[];expected=[]
for i,(task,old,conflict) in enumerate(pairs):
    person=str(90000+i);other=str(80000+i)
    time='2099-03-01 15:00:00'
    if i in (3,4):time='2099-03-01 16:00:00'
    if i==6:time='2099-03-02 08:00:00'
    if i==7:time=None
    proposed.append({'summary':task,'timeDescription':task,'eventTime':time,'deadlineTime':'2099-03-01 23:59:59' if i==7 else None,
        'recipients':[{'id':person,'name':'模拟接收人'+str(i),'taskContent':task}]})
    existing.append({'eventId':other,'recipientId':person,'task':old,'timeDescription':old,
        'eventTime':'2099-03-01 15:00:00' if i in (3,4,5,7) else '2099-03-01 08:00:00','createdAt':'2099-02-28 08:00:00'})
    if conflict:expected.append([i,other])
Path('output/schedule138-fixture.json').write_text(json.dumps({'prompt':prompt,'input':{'proposed':proposed,'existing':existing,
    'timezone':'Asia/Shanghai','currentDate':'2099-02-28'},'expected':expected},ensure_ascii=False),encoding='utf-8')
root=Path('output/conflict138/org/springblade/modules/smartreminder/service')
files=list(root.glob('*.class'))
assert len(files)==10,'Compile the three modified service classes before preparing the patch'
with zipfile.ZipFile('output/schedule138-patch.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for path in files: archive.write(path,'BOOT-INF/classes/org/springblade/modules/smartreminder/service/'+path.name)
print('Synthetic fixtures and ten-class patch prepared')
