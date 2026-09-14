const id = value => /^[1-9]\d{0,19}$/.test(String(value||''));
// Event service times are Asia/Shanghai, independent of the phone's current timezone.
export function alarmTimestamp(value) {
  const match=String(value||'').match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?(?:\.\d{1,6})?$/);
  if(!match)return NaN;
  const normalized=match[1]+'T'+match[2]+':'+(match[3]||'00');
  const result=Date.parse(normalized+'+08:00');
  if(!Number.isFinite(result)||new Date(result+8*3600000).toISOString().slice(0,19)!==normalized)return NaN;
  return result/1000;
}
export function alarmPlan(detail,owner,now=Date.now()/1000) {
  const event=detail?.event;
  if(!event||!id(event.id)||!id(owner))throw Error('事件或当前账号无效');
  const branch=(detail.branches||[]).find(b=>String(b.recipientUserId)===String(owner));
  if(!branch)throw Error('只能为自己接收的任务设置本机闹铃，好友需在自己的手机上设置');
  if(event.event_status!=='ACTIVE'||branch.branchStatus!=='ACTIVE')throw Error('事件或自己的任务已停止，不能设置闹铃');
  const timestamp=alarmTimestamp(branch.taskEventTime===undefined?event.event_time:branch.taskEventTime);
  if(!Number.isFinite(timestamp)||timestamp<=now+2)throw Error('请先为事件设置明确的未来时间，再设置系统闹铃');
  return {eventId:String(event.id),owner:String(owner),timestamp,title:String(branch.latestSummary||event.event_summary||'小醒提醒').slice(0,100)};
}
