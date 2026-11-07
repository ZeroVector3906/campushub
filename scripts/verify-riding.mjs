import fs from 'node:fs';
import crypto from 'node:crypto';
// Reuse the existing deployment client's cookie and CSRF implementation.
const source=fs.readFileSync(new URL('verify-deployment.mjs',import.meta.url),'utf8');
const prefix=source.slice(0,source.indexOf('const admin=new Client()'));
const body=`
const admin=new Client(),student=new Client(),anonymous=new Client();
let user,application,authenticated=false;const fixtures=[];
try{
 await admin.call('/api/auth/login','POST',{username:'admin',password:fs.readFileSync(process.env.CAMPUSHUB_PASSWORD_FILE||new URL('cloud-admin-password.txt',runtime),'utf8').trim()});
 authenticated=true;
 const suffix=Date.now().toString(36),username='ridingqa_'+suffix;
 const password=crypto.randomBytes(20).toString('hex');
 user=await anonymous.call('/api/auth/register','POST',{username,password,name:'骑行申请验收账号',email:username+'@example.test',studentNo:'QA'+suffix,department:'部署验收'},201);
 await student.call('/api/auth/login','POST',{username,password});
 await anonymous.call('/api/riding-requests','GET',undefined,401);
 await student.call('/api/admin/riding-requests','GET',undefined,403);
 const suggestion={kind:'SUGGESTION',title:'验收测试建议（非真实活动）',description:'仅验证申请流程，无需组织活动',contact:'qa@example.test'};
 const s=await student.call('/api/riding-requests','POST',suggestion,201);fixtures.push(s.id);
 const room={...suggestion,kind:'ROOM',title:'验收测试使用申请（无需安排）',room:'北三骑行室',usage:'其他',startsAt:new Date(Date.now()+86400000).toISOString(),endsAt:new Date(Date.now()+90000000).toISOString()};
 await student.call('/api/riding-requests','POST',room,400);
 const r=await student.call('/api/riding-requests','POST',{...room,otherUsage:'验收测试，不使用场地'},201);fixtures.push(r.id);
 const r2=await student.call('/api/riding-requests','POST',{...room,room:'和光文体中心俱乐部',usage:'训练'},201);fixtures.push(r2.id);
 await student.call('/api/riding-requests','POST',{...room,usage:'训练',endsAt:room.startsAt},400);
 const list=await admin.call('/api/admin/riding-requests');
 if(!list.some(x=>x.id===r.id&&x.user.id===user.id&&x.contact===room.contact&&x.otherUsage==='验收测试，不使用场地'&&x.startsAt))throw Error('Administrator data incomplete');
 await admin.call('/api/admin/riding-requests/'+s.id+'/review','PUT',{status:'APPROVED',note:'验收测试通过'});
 const own=await student.call('/api/riding-requests');if(!own.some(x=>x.id===s.id&&x.reviewNote==='验收测试通过'))throw Error('Review not visible to applicant');
 const clubs=await student.call('/api/clubs');const club=clubs.find(c=>c.name==='征途单车俱乐部');
 application=await student.call('/api/clubs/'+club.id+'/applications','POST',{reason:'验收测试，非真实加入申请',contact:'qa@example.test',grade:'2025级',college:'部署验收学院'},201);
 const applications=await admin.call('/api/admin/applications');if(!applications.some(x=>x.id===application.id&&x.contact==='qa@example.test'&&x.grade==='2025级'&&x.college==='部署验收学院'))throw Error('Membership details not saved');
}finally{
 for(const id of fixtures)await admin.call('/api/admin/riding-requests/'+id+'/review','PUT',{status:'REJECTED',note:'自动验收测试已结束，无需安排活动或场地'});
 if(application)await student.call('/api/applications/'+application.id+'/withdraw','POST');
 if(user)await admin.call('/api/admin/users/'+user.id+'/access','PUT',{role:'STUDENT',enabled:false});
 if(authenticated)await admin.call('/api/auth/logout','POST');
}
const report={base,completedAt:new Date().toISOString(),passed:checks.length,checks,fixturesRejected:true,testAccountDisabled:true};
fs.writeFileSync(new URL('riding-verification.json',runtime),JSON.stringify(report,null,2));console.log(JSON.stringify({passed:checks.length,fixturesRejected:true,testAccountDisabled:true}));
`;
// Materialize a temporary module next to the ignored runtime reports, never credentials.
const script=new URL('../.runtime/verify-riding-run.mjs',import.meta.url);
fs.writeFileSync(script,prefix+body);
await import(script.href+'?run='+Date.now());
