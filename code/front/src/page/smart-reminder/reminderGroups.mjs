const dayOf=ms=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(ms);
const timeOf=value=>{if(!value)return NaN;const text=String(value).replace(' ','T');return Date.parse(/(?:Z|[+-]\d\d:\d\d)$/.test(text)?text:text+'+08:00');};
export function reminderGroups(rows,now=Date.now(),range=[]){
  const today=dayOf(now),tomorrow=dayOf(now+86400000);
  const groups=[{key:'today',title:'今日待提醒',icon:'clock',status:'待提醒'},{key:'sent',title:'今日已提醒',icon:'check',status:'已提醒'},{key:'tomorrow',title:'明日待提醒',icon:'calendar',status:'明天'},{key:'later',title:'历史创建 · 未到期',icon:'calendar',status:'未到期'}].map(g=>({...g,items:[]}));
  for(const row of rows){
    const sent=timeOf(row.lastRemindedAt),next=timeOf(row.nextEvaluateTime||row.eventTime||row.deadlineTime),deadline=timeOf(row.deadlineTime||row.eventTime);
    const active=row.eventStatus==='ACTIVE'&&row.branchStatus==='ACTIVE';
    if(Number.isFinite(sent)&&dayOf(sent)===today)groups[1].items.push({...row,displayTime:row.lastRemindedAt});
    if(!active)continue;
    if(Number.isFinite(sent)&&dayOf(sent)===today&&!(next>sent))continue;
    // A sent reminder and another future reminder may legitimately occur on the same day.
    if(Number.isFinite(next)&&dayOf(next)<=today&&(!Number.isFinite(sent)||next>sent))groups[0].items.push({...row,displayTime:row.nextEvaluateTime||row.eventTime||row.deadlineTime});
    else if(Number.isFinite(next)&&dayOf(next)===tomorrow)groups[2].items.push({...row,displayTime:row.nextEvaluateTime||row.eventTime||row.deadlineTime});
    else if(!Number.isFinite(deadline)||deadline>=now)groups[3].items.push({...row,displayTime:row.eventTime||row.deadlineTime||row.nextEvaluateTime});
  }
  // One card per event/category, even when a creator has several recipient branches.
  for(const group of groups){const grouped=new Map();for(const item of group.items){
    // Filter each branch before combining recipients; use the date actually shown on the card.
    const ms=timeOf(item.displayTime),date=Number.isFinite(ms)?dayOf(ms):'';
    if((range[0]||range[1])&&(!date||(range[0]&&date<range[0])||(range[1]&&date>range[1])))continue;
    const key=item.eventId;const existing=grouped.get(key);if(existing){existing.people.push(item.recipientName);if(item.displayTime!==existing.displayTime)existing.differentTimes=true;}else grouped.set(key,{...item,people:[item.recipientName]});}group.items=[...grouped.values()].sort((a,b)=>(timeOf(a.displayTime)||Infinity)-(timeOf(b.displayTime)||Infinity));}
  return groups;
}
export const reminderDateLabel=value=>value?String(value).replace('T',' ').slice(5,16):'时间待确定';
