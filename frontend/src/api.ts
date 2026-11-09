/// <reference types="vite/client" />
import { ElMessage } from 'element-plus'
let csrfToken = ''
export const connectionEvents=new EventTarget()
export class ApiError extends Error { constructor(public status:number,message:string){super(message)} }
// Use the host's ordinary startup document in a separate origin, never as API data.
export function serviceWakeUrl(){
 try{const target=new URL(import.meta.env.VITE_SERVICE_WAKE_URL||'');if(target.protocol!=='https:'||target.username||target.password||target.search||target.hash||target.pathname!=='/'||(typeof location!=='undefined'&&target.origin===location.origin))return '';return target.href}catch{return ''}
}
export async function wakeService():Promise<()=>void>{
 const url=serviceWakeUrl()
 if(!url||typeof document==='undefined')return ()=>{}
 const frame=document.createElement('iframe')
 frame.src=url;frame.title='骑行服务启动';frame.hidden=true;frame.tabIndex=-1
 frame.referrerPolicy='no-referrer';frame.setAttribute('aria-hidden','true')
 frame.setAttribute('sandbox','allow-scripts allow-same-origin');frame.setAttribute('credentialless','')
 document.body.append(frame)
 return ()=>frame.remove()
}
export function resetCsrf(){csrfToken=''}
// Bound every request, including response-body reads. Never replay a write automatically.
async function request(path:string,options:RequestInit={},timeoutMs=12000):Promise<Response>{
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),timeoutMs)
  try{
    const response=await fetch('/api'+path,{...options,credentials:path==='/health'?'omit':'same-origin',cache:'no-store',signal:controller.signal})
    const text=await response.text()
    if(!response.headers.get('content-type')?.includes('application/json'))throw new ApiError(503,'服务正在启动或暂时不可用，请稍后重新连接')
    let data:unknown
    try{data=JSON.parse(text)}catch{throw new ApiError(503,'服务返回异常，请稍后重新连接')}
    return new Response(JSON.stringify(data),{status:response.status,headers:{'Content-Type':'application/json'}})
  }catch(error){if(error instanceof ApiError)throw error;throw new ApiError(0,controller.signal.aborted?'连接超时，服务可能正在启动，请稍后重新连接':'网络连接失败，请检查网络后重试')}
  finally{clearTimeout(timer)}
}
export async function api<T=any>(path:string,method='GET',body?:unknown,quiet=false,timeoutMs=12000):Promise<T>{
  try{
    if(method!=='GET'&&!csrfToken){const r=await request('/auth/csrf');if(!r.ok)throw new ApiError(r.status,'无法获取安全令牌');csrfToken=(await r.json()).token}
    const res=await request(path,{method,headers:{...(body!==undefined?{'Content-Type':'application/json'}:{}),...(method!=='GET'?{'X-CSRF-TOKEN':csrfToken}:{})},body:body===undefined?undefined:JSON.stringify(body)},timeoutMs)
    const data=await res.json()
    if(!res.ok){if(res.status===403)resetCsrf();throw new ApiError(res.status,data.message||'请求失败')}
    return data as T
  }catch(error){if(!quiet){const unavailable=error instanceof ApiError&&[0,503].includes(error.status);ElMessage.error(unavailable&&method!=='GET'?'连接中断，操作结果尚未确认；恢复后请先查看记录，再决定是否重试':error instanceof Error?error.message:'请求失败');if(unavailable)connectionEvents.dispatchEvent(new Event('unavailable'))}throw error}
}
export async function run(task:()=>Promise<unknown>,success?:string){try{await task();if(success)ElMessage.success(success);return true}catch{return false}}
export async function downloadReport(type:string){
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),20000)
  try{const res=await fetch('/api/admin/reports/export?type='+encodeURIComponent(type),{credentials:'same-origin',cache:'no-store',signal:controller.signal});if(!res.ok||!res.headers.get('content-type')?.includes('text/csv'))throw new Error();const blob=await res.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`征途单车俱乐部-${type}-${new Date().toISOString().slice(0,10)}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}catch{ElMessage.error('报表下载失败或超时，请确认登录状态后重试')}finally{clearTimeout(timer)}
}
