import { ElMessage } from 'element-plus'
let csrfToken = ''
export class ApiError extends Error { constructor(public status:number,message:string){super(message)} }
export function resetCsrf(){csrfToken=''}
export async function api<T=any>(path:string,method='GET',body?:unknown,quiet=false):Promise<T>{
  if(method!=='GET' && !csrfToken){ const r=await fetch('/api/auth/csrf',{credentials:'same-origin'});if(!r.ok)throw new ApiError(r.status,'无法获取安全令牌');csrfToken=(await r.json()).token }
  const res=await fetch('/api'+path,{method,credentials:'same-origin',headers:{...(body!==undefined?{'Content-Type':'application/json'}:{}),...(method!=='GET'?{'X-CSRF-TOKEN':csrfToken}:{})},body:body===undefined?undefined:JSON.stringify(body)})
  const data=await res.json().catch(()=>({message:'服务器返回异常，请稍后重试'}))
  if(!res.ok){if(res.status===403)resetCsrf();if(!quiet)ElMessage.error(data.message||'请求失败');throw new ApiError(res.status,data.message||'请求失败')}
  return data as T
}
export async function run(task:()=>Promise<unknown>,success?:string){try{await task();if(success)ElMessage.success(success);return true}catch{return false}}
export async function downloadReport(type:string){
  const res=await fetch('/api/admin/reports/export?type='+encodeURIComponent(type),{credentials:'same-origin'})
  if(!res.ok){ElMessage.error('报表下载失败，请确认管理员登录状态');return}
  const url=URL.createObjectURL(await res.blob());const link=document.createElement('a');link.href=url;link.download=`征途单车俱乐部-${type}-${new Date().toISOString().slice(0,10)}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)
}
