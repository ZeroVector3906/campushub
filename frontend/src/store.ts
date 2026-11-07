import { reactive, computed } from 'vue'
import { api, resetCsrf } from './api'
import type { User,Club,Activity,Equipment,Row,Dashboard } from './types'
export const state=reactive({user:null as User|null,clubs:[] as Club[],activities:[] as Activity[],equipment:[] as Equipment[],overview:{} as Row,mine:{memberships:[],registrations:[],loans:[],repairs:[]} as Dashboard,ready:false,error:'',busy:false})
export const isAdmin=computed(()=>state.user?.role==='ADMIN')
export async function refreshPublic(){const [clubs,activities,equipment,overview]=await Promise.all([api<Club[]>('/clubs'),api<Activity[]>('/activities'),api<Equipment[]>('/equipment'),api<Row>('/public/overview')]);Object.assign(state,{clubs,activities,equipment,overview})}
export async function refreshMine(){if(state.user){state.mine=await api('/me/dashboard');if(state.mine.member)Object.assign(state.user,state.mine.member)}}
export async function initialize(){state.error='';try{await refreshPublic();state.user=await api<User>('/auth/me','GET',undefined,true).catch(()=>null);await refreshMine()}catch(e){state.error=e instanceof Error?e.message:'加载失败'}finally{state.ready=true}}
export async function login(username:string,password:string){state.user=await api<User>('/auth/login','POST',{username,password});resetCsrf();await refreshMine()}
export async function logout(){await api('/auth/logout','POST');resetCsrf();state.user=null;state.mine={memberships:[],registrations:[],loans:[],repairs:[]}}
