import fs from 'node:fs';
import crypto from 'node:crypto';
const base=process.env.CAMPUSHUB_VERIFY_BASE||'https://campushub-2ar9.onrender.com';
const runtime=new URL('../.runtime/',import.meta.url);
const read=name=>fs.readFileSync(new URL(name,runtime),'utf8').replace(/^\uFEFF/,'');
const checks=[];
class Client {
  cookies=new Map();
  async raw(path,method='GET',body,token='') {
    const r=await fetch(base+path,{method,headers:{Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join('; '),...(body!==undefined?{'Content-Type':'application/json'}:{}),...(token?{'X-CSRF-TOKEN':token}:{})},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(45000)});
    for(const c of r.headers.getSetCookie()){const p=c.split(';')[0],at=p.indexOf('=');this.cookies.set(p.slice(0,at),p.slice(at+1));}
    const text=await r.text();return {status:r.status,text,headers:r.headers};
  }
  async call(path,method='GET',body,expected=200){
    let token='';if(method!=='GET')token=JSON.parse((await this.raw('/api/auth/csrf')).text).token;
    const r=await this.raw(path,method,body,token);
    if(r.status!==expected)throw Error(`${method} ${path}: expected ${expected}, got ${r.status}: ${r.text.slice(0,400)}`);
    checks.push({method,path,status:r.status});return r.text?JSON.parse(r.text):null;
  }
}
const admin=new Client(),student=new Client(),anon=new Client();
await anon.call('/api/health');
const root=await anon.raw('/');if(root.status!==200||!root.text.includes('西安欧亚学院征途单车俱乐部'))throw Error('Homepage failed');
await admin.call('/api/auth/login','POST',{username:'admin',password:fs.readFileSync(process.env.CAMPUSHUB_PASSWORD_FILE||new URL('cloud-admin-password.txt',runtime),'utf8').trim()});
console.log('Homepage, health endpoint and administrator login verified.');
const clubs=await anon.call('/api/clubs');
if(clubs.length!==1||clubs[0].name!=='征途单车俱乐部'||clubs[0].icon!=='bike')throw Error('Club identity or migration failed');
const clubMap=new Map([[clubs[0].id,clubs[0].id]]);
const illustration=await anon.raw('/images/cycling.svg');if(illustration.status!==200||!illustration.text.includes('<svg'))throw Error('Cycling illustration missing');
checks.push({check:'Zhengtu identity and cycling asset',status:200});
const suffix=Date.now().toString(36),username='cloudqa_'+suffix,password=crypto.randomBytes(20).toString('hex');
const user=await student.call('/api/auth/register','POST',{username,password,name:'云端验收测试账号',email:username+'@example.test',studentNo:'QA'+suffix,department:'部署验收'},201);
await student.call('/api/auth/login','POST',{username,password});
let qaActivity,qaEquipment;
try {
  await student.call('/api/admin/users','GET',undefined,403);
  await anon.call('/api/admin/users','GET',undefined,401);
  const noCsrf=await student.raw('/api/clubs/'+clubMap.values().next().value+'/applications','POST',{reason:'CSRF check'});if(noCsrf.status!==403)throw Error('CSRF protection failed');
  checks.push({check:'CSRF rejection',status:noCsrf.status});
  const application=await student.call('/api/clubs/'+clubMap.values().next().value+'/applications','POST',{reason:'云端部署验收，仅测试操作'},201);
  await admin.call('/api/admin/applications/'+application.id+'/review','PUT',{status:'APPROVED',note:'云端验收'});
  const now=Date.now(),stamp=ms=>new Date(ms).toISOString();
  const body={clubId:clubMap.values().next().value,title:'征途骑行验收活动（已完成后取消）'+suffix,category:'骑行训练',description:'仅供部署接口验收，无实际活动。',location:'云端测试环境',startsAt:stamp(now+120000),endsAt:stamp(now+3600000),registrationDeadline:stamp(now+120000),capacity:1,checkinCode:crypto.randomBytes(8).toString('hex'),featured:false,status:'PUBLISHED'};
  qaActivity={body,data:await admin.call('/api/admin/activities','POST',body,201)};
  const registration=await student.call('/api/activities/'+qaActivity.data.id+'/registrations','POST',{},201);
  await student.call('/api/registrations/'+registration.id+'/checkin','POST',{code:'WRONG'},400);
  await student.call('/api/registrations/'+registration.id+'/checkin','POST',{code:body.checkinCode});
  qaEquipment=await admin.call('/api/admin/equipment','POST',{name:'征途部署验收工具（非实际资产）',assetCode:'ZT-QA-'+suffix,category:'维修工具',description:'仅用于接口验收，结束后停用；无实际器材或库存。',location:'部署验收环境',totalQuantity:2,active:true},201);
  const equipment=qaEquipment;
  const loan=await student.call('/api/loans','POST',{equipmentId:equipment.id,quantity:1,purpose:'云端接口验收，无实际领用',dueDate:new Date(now+86400000).toISOString().slice(0,10)},201);
  await admin.call('/api/admin/loans/'+loan.id+'/review','PUT',{status:'APPROVED',note:'云端验收'});
  await student.call('/api/loans/'+loan.id+'/return','POST');
  await admin.call('/api/admin/loans/'+loan.id+'/review','PUT',{status:'RETURNED',note:'验收完成，库存恢复'});
  const restored=(await student.call('/api/equipment')).find(e=>e.id===equipment.id);if(restored.availableQuantity!==equipment.availableQuantity)throw Error('Inventory not restored');
  const repair=await student.call('/api/repairs','POST',{equipmentId:equipment.id,title:'云端验收工单（无需实际维修）',description:'仅验证工单流程',location:'云端测试环境',priority:'LOW'},201);
  for(const status of ['PROCESSING','RESOLVED','CLOSED'])await admin.call('/api/admin/repairs/'+repair.id+'/progress','POST',{status,note:'云端验收步骤 '+status});
  await admin.call('/api/admin/reports');
  for(const type of ['members','activities','equipment','repairs']){const csv=await admin.raw('/api/admin/reports/export?type='+type);if(csv.status!==200||!csv.headers.get('content-type')?.includes('text/csv'))throw Error('CSV export failed');checks.push({check:'CSV '+type,status:csv.status});}
  await student.call('/api/applications/'+application.id+'/withdraw','POST');
  await student.call('/api/auth/logout','POST');await student.call('/api/auth/me','GET',undefined,401);
} finally {
  if(qaEquipment)await admin.call('/api/admin/equipment/'+qaEquipment.id,'PUT',{...qaEquipment,active:false});
  if(qaActivity)await admin.call('/api/admin/activities/'+qaActivity.data.id,'PUT',{...qaActivity.body,status:'CANCELLED'});
  await admin.call('/api/admin/users/'+user.id+'/access','PUT',{role:'STUDENT',enabled:false});
}
await admin.call('/api/auth/logout','POST');
const report={base,completedAt:new Date().toISOString(),checks,fixtureAccount:username,fixtureAccountDisabled:true,qaActivityCancelled:true,qaEquipmentDisabled:true,notes:'Zhengtu club identity verified. QA equipment disabled, activity cancelled and account disabled. CSV response verified; browser download-to-disk not verified.'};
fs.writeFileSync(new URL('zhengtu-cloud-verification.json',runtime),JSON.stringify(report,null,2));
console.log(JSON.stringify({passed:checks.length,fixtureAccountDisabled:true,qaActivityCancelled:true}));
