import {createRouter,createWebHashHistory} from 'vue-router'
import {state} from './store'
export const router=createRouter({history:createWebHashHistory(),scrollBehavior:()=>({top:0}),routes:[
 {path:'/',component:()=>import('./pages/Home.vue'),meta:{title:'校园概览'}},
 {path:'/clubs',component:()=>import('./pages/Clubs.vue'),meta:{title:'发现社团'}},
 {path:'/activities',component:()=>import('./pages/Activities.vue'),meta:{title:'校园活动'}},
 {path:'/equipment',component:()=>import('./pages/Equipment.vue'),meta:{title:'器材借用'}},
 {path:'/repairs',component:()=>import('./pages/Repairs.vue'),meta:{title:'在线报修'}},
 {path:'/center',component:()=>import('./pages/Center.vue'),meta:{title:'成员中心',auth:true}},
 {path:'/admin',component:()=>import('./pages/Admin.vue'),meta:{title:'管理工作台',admin:true}},
 {path:'/login',component:()=>import('./pages/Login.vue'),meta:{title:'欢迎加入 CampusHub'}},
 {path:'/:pathMatch(.*)*',redirect:'/'}
]})
router.beforeEach(async to=>{if(!state.ready)return true;if((to.meta.auth||to.meta.admin)&&!state.user)return {path:'/login',query:{redirect:to.fullPath}};if(to.meta.admin && state.user?.role!=='ADMIN')return '/';return true})
