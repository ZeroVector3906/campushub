import fs from 'node:fs';
// Reuse the project's deployment client (cookies, CSRF, timeouts and request evidence).
const source=fs.readFileSync(new URL('verify-deployment.mjs',import.meta.url),'utf8');
const prefix=source.slice(0,source.indexOf('const admin=new Client()'));
const body=String.raw`
const admin=new Client(),clubClient=new Client(),societyClient=new Client(),anon=new Client();
const fixtureUsers=[],requests=[];let gear,loan,authenticated=false;
const suffix=Date.now().toString(36),password=crypto.randomBytes(20).toString('hex');
const profile=(u,type,status)=>({name:'身份权限验收账号',email:u.email,studentNo:u.studentNo,department:'部署验收学院',contact:'qa@example.test',grade:'2026级',joinType:type,clubDivision:type==='CLUB'?'活动部':'',memberStatus:status,note:'自动验收测试，非真实成员'});
const room=(place,usage)=>({kind:'ROOM',title:'身份权限验收（无需安排场地）',description:'仅验证成员权限，不使用实际场地',contact:'qa@example.test',room:place,usage,startsAt:new Date(Date.now()+86400000).toISOString(),endsAt:new Date(Date.now()+90000000).toISOString()});
try{
 await admin.call('/api/auth/login','POST',{username:'admin',password:fs.readFileSync(process.env.CAMPUSHUB_PASSWORD_FILE||new URL('cloud-admin-password.txt',runtime),'utf8').trim()});authenticated=true;
 await anon.call('/api/health');
 const root=await anon.raw('/');if(root.status!==200||!root.text.includes('西安欧亚学院征途单车俱乐部&amp;社团'))throw Error('Site name not updated');checks.push({check:'new site title',status:200});
 for(const [type,client] of [['CLUB',clubClient],['SOCIETY',societyClient]]){
  const username='memberqa_'+type.toLowerCase()+'_'+suffix;
  const u=await anon.call('/api/auth/register','POST',{username,password,name:'自动验收账号',email:username+'@example.test',studentNo:'QA'+suffix+type,department:'部署验收学院',joinType:type,contact:'qa@example.test',grade:'2025级',clubDivision:type==='CLUB'?'课程部':''},201);
  fixtureUsers.push({u,client});
  if(u.role!=='STUDENT'||u.memberStatus!=='PENDING'||!u.canBorrowEquipment||u.allowedRooms.length)throw Error('Registration granted unapproved rights');
  await client.call('/api/auth/login','POST',{username,password});
 }
 const club=fixtureUsers[0].u,society=fixtureUsers[1].u;
 await societyClient.call('/api/riding-requests','POST',room('北三骑行室','存车'),403);
 await admin.call('/api/admin/users/'+club.id+'/profile','PUT',profile(club,'CLUB','APPROVED'));
 await admin.call('/api/admin/users/'+society.id+'/profile','PUT',profile(society,'SOCIETY','APPROVED'));
 const members=await admin.call('/api/admin/users');
 if(!members.some(x=>x.id===club.id&&x.clubDivision==='活动部'&&x.contact==='qa@example.test'&&x.grade==='2026级'))throw Error('Member editor fields missing');
 const clubStatus=(await clubClient.call('/api/me/dashboard')).member;
 const societyStatus=(await societyClient.call('/api/me/dashboard')).member;
 if(!clubStatus.canBorrowEquipment||clubStatus.allowedRooms.length!==2||!societyStatus.canBorrowEquipment||societyStatus.allowedRooms.join()!=='北三骑行室')throw Error('Wrong membership access');
 gear=await admin.call('/api/admin/equipment','POST',{name:'身份权限验收器材（非真实库存）',assetCode:'MEMBER-QA-'+suffix,category:'骑行装备',description:'自动验收专用，结束后停用',location:'验收测试环境',totalQuantity:1,active:true},201);
 const societyLoan=await societyClient.call('/api/loans','POST',{equipmentId:gear.id,quantity:1,purpose:'社团器材申请权限保持原样',dueDate:new Date(Date.now()+86400000).toISOString().slice(0,10)},201);await societyClient.call('/api/loans/'+societyLoan.id+'/cancel','POST');
 loan=await clubClient.call('/api/loans','POST',{equipmentId:gear.id,quantity:1,purpose:'自动验收测试',dueDate:new Date(Date.now()+86400000).toISOString().slice(0,10)},201);
 loan=await admin.call('/api/admin/loans/'+loan.id+'/review','PUT',{status:'APPROVED',note:'自动验收，不发放实物'});
 const pending=await clubClient.call('/api/riding-requests','POST',room('和光文体中心单车俱乐部','维修'),201);requests.push(pending.id);
 for(const usage of ['存车','拿车']){const r=await societyClient.call('/api/riding-requests','POST',room('北三骑行室',usage),201);requests.push(r.id)}
 for(const usage of ['训练','维修','休息'])await societyClient.call('/api/riding-requests','POST',room('北三骑行室',usage),403);
 await societyClient.call('/api/riding-requests','POST',{...room('北三骑行室','其他'),otherUsage:'室内活动'},403);
 await societyClient.call('/api/riding-requests','POST',room('和光文体中心单车俱乐部','存车'),403);
 await societyClient.call('/api/admin/users/'+club.id+'/profile','PUT',profile(club,'CLUB','APPROVED'),403);
 await admin.call('/api/admin/users/'+club.id+'/profile','PUT',profile(club,'SOCIETY','APPROVED'));
 await clubClient.call('/api/riding-requests','POST',room('和光文体中心单车俱乐部','休息'),403);
 await admin.call('/api/admin/riding-requests/'+pending.id+'/review','PUT',{status:'APPROVED'},403);
 await clubClient.call('/api/loans/'+loan.id+'/return','POST');
 loan=await admin.call('/api/admin/loans/'+loan.id+'/review','PUT',{status:'RETURNED',note:'验收完成，无实物发放'});
 const me=await clubClient.call('/api/auth/me');if(me.role!=='STUDENT')throw Error('Member edit changed security role');
 const csv=await admin.raw('/api/admin/reports/export?type=members');if(csv.status!==200||!csv.text.includes('加入身份')||!csv.text.includes('联系方式'))throw Error('Member export missing fields');checks.push({check:'member export fields',status:200});
}finally{
 if(authenticated){
  if(loan&&loan.status==='PENDING')await fixtureUsers[0].client.call('/api/loans/'+loan.id+'/cancel','POST');
  if(loan&&loan.status==='APPROVED'){await fixtureUsers[0].client.call('/api/loans/'+loan.id+'/return','POST');loan=await admin.call('/api/admin/loans/'+loan.id+'/review','PUT',{status:'RETURNED',note:'验收清理'});}
  for(const id of requests)await admin.call('/api/admin/riding-requests/'+id+'/review','PUT',{status:'REJECTED',note:'自动验收结束，无需安排场地'});
  if(gear)await admin.call('/api/admin/equipment/'+gear.id,'PUT',{...gear,active:false});
  for(const {u,client} of fixtureUsers){const mine=await client.call('/api/me/dashboard');for(const m of mine.memberships.filter(x=>['PENDING','APPROVED'].includes(x.status)))await client.call('/api/applications/'+m.id+'/withdraw','POST');await admin.call('/api/admin/users/'+u.id+'/access','PUT',{role:'STUDENT',enabled:false});}
  await admin.call('/api/auth/logout','POST');
 }
}
const report={base,completedAt:new Date().toISOString(),passed:checks.length,checks,fixtureAccountsDisabled:true,fixtureMembershipsWithdrawn:true,testRequestsRejected:true,testEquipmentDisabled:true};
fs.writeFileSync(new URL('member-verification.json',runtime),JSON.stringify(report,null,2));console.log(JSON.stringify({passed:checks.length,fixtureAccountsDisabled:true,testRequestsRejected:true,testEquipmentDisabled:true}));
`;
const script=new URL('../.runtime/verify-members-run.mjs',import.meta.url);
fs.writeFileSync(script,prefix+body);await import(script.href+'?run='+Date.now());
