import test from 'node:test';
import assert from 'node:assert/strict';
import {calendarDays,shiftDay,shiftMonth,selectRangeDay,shanghaiToday} from '../src/page/smart-reminder/reminderCalendar.mjs';
test('Chinese calendar uses Monday first and supports month/year/leap boundaries',()=>{
  const days=calendarDays('2026-09');assert.equal(days.length,42);assert.equal(days[0],'2026-08-31');assert.equal(days[41],'2026-10-11');
  assert.equal(shiftMonth('2026-12',1),'2027-01');assert.equal(shiftMonth('2026-01',-1),'2025-12');
  assert.equal(shiftDay('2028-02-28',1),'2028-02-29');assert.equal(shiftDay('2026-02-28',1),'2026-03-01');
  assert.equal(shanghaiToday(Date.parse('2026-09-15T16:01:00Z')),'2026-09-16');
});
test('range selection allows one day, open endpoints and ordered reverse selections',()=>{
  assert.deepEqual(selectRangeDay('','','start','2026-09-16'),{start:'2026-09-16',end:'',active:'end'});
  assert.deepEqual(selectRangeDay('2026-09-16','','end','2026-09-16'),{start:'2026-09-16',end:'2026-09-16',active:'start'});
  assert.deepEqual(selectRangeDay('2026-09-16','','end','2026-09-12'),{start:'2026-09-12',end:'2026-09-16',active:'start'});
  assert.deepEqual(selectRangeDay('2026-09-16','2026-09-20','start','2026-09-21'),{start:'2026-09-21',end:'',active:'end'});
  assert.deepEqual(selectRangeDay('','','end','2026-09-16'),{start:'',end:'2026-09-16',active:'start'});
});
