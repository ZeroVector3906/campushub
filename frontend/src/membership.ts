export const joinTypes=[{value:'CLUB',label:'俱乐部'},{value:'SOCIETY',label:'社团'}]
export const clubDivisions=['课程部','活动部','宣传部']
export const memberTypeName=(type?:string)=>joinTypes.find(x=>x.value===type)?.label||'待分类'
export const memberStatusName=(status?:string)=>({NONE:'未申请',PENDING:'待审核',APPROVED:'已通过',REJECTED:'已驳回',WITHDRAWN:'已退出'}[status||'NONE']||status)
export const roomNames=['和光文体中心单车俱乐部','北三骑行室']
export const roomUsages=['训练','存取车辆','维修','休息','其他']
