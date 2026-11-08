import { ElMessage } from 'element-plus'
let csrfToken = ''
export class ApiError extends Error { constructor(public status:number,message:string){super(message)} }
export function resetCsrf(){csrfToken=''}
// Bound every request, including response-body reads. Never replay a write automatically.
async function request(path:string,options:RequestInit={}):Promise<Response>{
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),12000)
  try{
    const response=await fetch('/api'+path,{...options,credentials:'same-origin',cache:'no-store',signal:controller.signal})
    const text=await response.text()
    if(!response.headers.get('content-type')?.includes('application/json'))throw new ApiError(503,'服务正在启动或暂时不可用，请稍后重新连接')
    let data:unknown
    try{data=JSON.parse(text)}catch{throw new ApiError(503,'服务返回异常，请稍后重新连接')}
    return new Response(JSON.stringify(data),{status:response.status,headers:{'Content-Type':'application/json'}})
  }catch(error){if(error instanceof ApiError)throw error;throw new ApiError(0,controller.signal.aborted?'连接超时，服务可能正在启动，请稍后重新连接':'网络连接失败，请检查网络后重试')}
  finally{clearTimeout(timer)}
}
export async function api<T=any>(path:string,method='GET',body?:unknown,quiet=false):Promise<T>{
  try{
    if(method!=='GET'&&!csrfToken){const r=await request('/auth/csrf');if(!r.ok)throw new ApiError(r.status,'无法获取安全令牌');csrfToken=(await r.json()).token}
    const res=await request(path,{method,headers:{...(body!==undefined?{'Content-Type':'application/json'}:{}),...(method!=='GET'?{'X-CSRF-TOKEN':csrfToken}:{})},body:body===undefined?undefined:JSON.stringify(body)})
    const data=await res.json()
    if(!res.ok){if(res.status===403)resetCsrf();throw new ApiError(res.status,data.message||'请求失败')}
    return data as T
  }catch(error){if(!quiet)ElMessage.error(error instanceof Error?error.message:'请求失败');throw error}
}
export async function run(task:()=>Promise<unknown>,success?:string){try{await task();if(success)ElMessage.success(success);return true}catch{return false}}
export async function downloadReport(type:string){
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),20000)
  try{const res=await fetch('/api/admin/reports/export?type='+encodeURIComponent(type),{credentials:'same-origin',cache:'no-store',signal:controller.signal});if(!res.ok||!res.headers.get('content-type')?.includes('text/csv'))throw new Error();const blob=await res.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`征途单车俱乐部-${type}-${new Date().toISOString().slice(0,10)}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}catch{ElMessage.error('报表下载失败或超时，请确认登录状态后重试')}finally{clearTimeout(timer)}
}
