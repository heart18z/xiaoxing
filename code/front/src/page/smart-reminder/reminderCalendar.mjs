const iso=date=>date.toISOString().slice(0,10);
export const shanghaiToday=(now=Date.now())=>{const parts=new Intl.DateTimeFormat('en',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(now);return ['year','month','day'].map(type=>parts.find(part=>part.type===type).value).join('-');};
export function shiftDay(day,offset){const date=new Date(day+'T00:00:00Z');date.setUTCDate(date.getUTCDate()+offset);return iso(date);}
export function shiftMonth(month,offset){const date=new Date(month+'-01T00:00:00Z');date.setUTCMonth(date.getUTCMonth()+offset);return iso(date).slice(0,7);}
export function calendarDays(month){const first=month+'-01';const weekday=new Date(first+'T00:00:00Z').getUTCDay();return Array.from({length:42},(_,i)=>shiftDay(first,i-(weekday+6)%7));}
export function selectRangeDay(start,end,active,day){
  if(active==='start')return{start:day,end:end&&end<day?'':end,active:'end'};
  if(start&&day<start)return{start:day,end:start,active:'start'};
  return{start,end:day,active:'start'};
}
