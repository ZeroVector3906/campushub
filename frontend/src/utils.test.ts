import {describe,it,expect} from 'vitest'
import {activityState,canCheckin,statusType,date} from './utils'
import type {Activity} from './types'
const now=Date.parse('2026-10-29T06:00:00Z')
const base:Activity={id:1,clubId:1,clubName:'读书会',title:'读书会',category:'文化艺术',description:'',location:'图书馆',startsAt:'2026-10-29T06:10:00Z',endsAt:'2026-10-29T08:00:00Z',registrationDeadline:'2026-10-29T06:10:00Z',capacity:10,registrationCount:3,featured:false,status:'PUBLISHED',canRegister:true}
describe('活动状态与签到规则',()=>{
 it('只有有效报名窗口显示报名中',()=>{expect(activityState(base,now)).toBe('报名中');expect(activityState({...base,registrationCount:10},now)).toBe('名额已满');expect(activityState({...base,registrationDeadline:'2026-10-29T05:00:00Z'},now)).toBe('报名截止')})
 it('区分进行、结束与取消状态',()=>{expect(activityState({...base,startsAt:'2026-10-29T05:00:00Z'},now)).toBe('进行中');expect(activityState(base,now+3*3600000)).toBe('已结束');expect(activityState({...base,status:'CANCELLED'},now)).toBe('已取消')})
 it('签到严格限制在开始前30分钟至结束',()=>{expect(canCheckin(base,now)).toBe(true);expect(canCheckin(base,now-21*60000)).toBe(false);expect(canCheckin(base,now+3*3600000)).toBe(false);expect(canCheckin({...base,status:'CANCELLED'},now)).toBe(false)})
 it('状态颜色与缺省日期可读',()=>{expect(statusType('RETURN_PENDING')).toBe('warning');expect(statusType('RETURNED')).toBe('success');expect(date(undefined)).toBe('—')})
})
