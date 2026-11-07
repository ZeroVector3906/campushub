import {createRouter,createWebHashHistory} from 'vue-router'
import {state} from './store'
export const router=createRouter({history:createWebHashHistory(),scrollBehavior:(to)=>to.hash?{el:to.hash,top:100}:{top:0},routes:[
 {path:'/',component:()=>import('./pages/Home.vue'),meta:{title:'俱乐部首页'}},
 {path:'/clubs',component:()=>import('./pages/Clubs.vue'),meta:{title:'俱乐部展示'}},
 {path:'/activities',component:()=>import('./pages/Activities.vue'),meta:{title:'骑行活动'}},
 {path:'/suggestions',component:()=>import('./pages/RidingRequests.vue'),meta:{title:'活动建议',auth:true}},
 {path:'/rooms',component:()=>import('./pages/RidingRequests.vue'),meta:{title:'骑行室使用',auth:true}},
 {path:'/equipment',component:()=>import('./pages/Equipment.vue'),meta:{title:'器材借用'}},
 {path:'/repairs',component:()=>import('./pages/Repairs.vue'),meta:{title:'在线报修'}},
 {path:'/center',component:()=>import('./pages/Center.vue'),meta:{title:'成员中心',auth:true}},
 {path:'/admin',component:()=>import('./pages/Admin.vue'),meta:{title:'管理工作台',admin:true}},
 {path:'/login',component:()=>import('./pages/Login.vue'),meta:{title:'加入征途俱乐部&社团'}},
 {path:'/:pathMatch(.*)*',redirect:'/'}
]})
router.beforeEach(async to=>{if(!state.ready)return true;if((to.meta.auth||to.meta.admin)&&!state.user)return {path:'/login',query:{redirect:to.fullPath}};if(to.meta.admin && !['ADMIN','SUPER_ADMIN'].includes(state.user?.role||''))return '/';return true})
