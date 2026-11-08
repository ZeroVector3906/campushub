import { reactive, computed } from 'vue'
import { api, resetCsrf, ApiError } from './api'
import type { User,Club,Activity,Equipment,Row,Dashboard } from './types'
export const state=reactive({user:null as User|null,clubs:[] as Club[],activities:[] as Activity[],equipment:[] as Equipment[],overview:{} as Row,mine:{memberships:[],registrations:[],loans:[],repairs:[]} as Dashboard,ready:false,error:'',busy:false,publicLoaded:false,connecting:false})
export const isAdmin=computed(()=>['ADMIN','SUPER_ADMIN'].includes(state.user?.role||''))
export async function refreshPublic(){
 const keys=['clubs','activities','equipment','overview'] as const
 const results=await Promise.allSettled([api<Club[]>('/clubs','GET',undefined,true),api<Activity[]>('/activities','GET',undefined,true),api<Equipment[]>('/equipment','GET',undefined,true),api<Row>('/public/overview','GET',undefined,true)])
 results.forEach((result,index)=>{if(result.status==='fulfilled')(state as any)[keys[index]!]=result.value})
 const failed=results.find(result=>result.status==='rejected')
 state.publicLoaded=!failed
 if(failed?.status==='rejected')throw failed.reason
}
export async function refreshMine(){if(state.user){state.mine=await api('/me/dashboard');if(state.mine.member)Object.assign(state.user,state.mine.member)}}
let initialization:Promise<void>|undefined
export function initialize():Promise<void>{
 if(initialization)return initialization
 state.connecting=true;state.error=''
 initialization=(async()=>{try{
   // A cold host needs a longer readiness window than an ordinary form request.
   await api('/health','GET',undefined,true,45000)
   const results=await Promise.allSettled([refreshPublic(),api<User>('/auth/me','GET',undefined,true)])
   const auth=results[1]
   if(auth.status==='fulfilled')state.user=auth.value
   else if(auth.reason instanceof ApiError&&auth.reason.status===401)state.user=null
   else throw auth.reason
   if(results[0].status==='rejected')throw results[0].reason
   await refreshMine()
 }catch(e){state.error=e instanceof Error?e.message:'加载失败'}finally{state.ready=true;state.connecting=false;initialization=undefined}})()
 return initialization
}
export async function login(username:string,password:string){state.user=await api<User>('/auth/login','POST',{username,password});resetCsrf();await refreshMine()}
export async function logout(){await api('/auth/logout','POST');resetCsrf();state.user=null;state.mine={memberships:[],registrations:[],loans:[],repairs:[]}}
