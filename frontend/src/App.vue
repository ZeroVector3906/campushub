<script setup lang="ts">
import {brand} from './brand'
import {computed,nextTick,onMounted,onUnmounted,ref,watch} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {state,isAdmin,initialize,logout} from './store'
import {run,connectionEvents} from './api'
import Icon from './components/Icon.vue'

const route=useRoute(),router=useRouter()
const mobile=ref(false),services=ref(false),searchOpen=ref(false),notifications=ref(false),search=ref('')
const searchInput=ref<HTMLInputElement>()
const serviceLinks=[{path:'/rooms',label:'骑行室使用',description:'为下一次出发，留一处空间。',icon:'bike'},{path:'/equipment',label:'器材借用',description:'共享装备，让热爱轻装上路。',icon:'package'},{path:'/repairs',label:'在线报修',description:'提交问题，随时了解维修进度。',icon:'wrench'}]
const notes=computed(()=>[...state.mine.memberships.map(x=>({title:x.club.name+' · 入社申请',text:x.reviewNote||'可在成员中心查看审核状态'})),...state.mine.loans.map(x=>({title:x.equipment.name+' · 器材借用',text:x.reviewNote||'可在成员中心查看借还进度'})),...state.mine.repairs.map(x=>({title:x.title,text:x.updates.at(-1)?.note||'工单已提交'}))].slice(0,12))
const today=new Intl.DateTimeFormat('zh-CN',{month:'long',day:'numeric',weekday:'long'}).format(new Date())
function closePanels(){mobile.value=false;services.value=false;searchOpen.value=false}
function focusMain(){document.getElementById('main-content')?.focus()}
async function toggleSearch(){const open=!searchOpen.value;closePanels();searchOpen.value=open;if(open){await nextTick();searchInput.value?.focus()}}
function submitSearch(){router.push({path:'/clubs',query:{q:search.value.trim()}});search.value='';closePanels()}
watch(()=>route.fullPath,closePanels)
let reconnectTimer:ReturnType<typeof setTimeout>|undefined
let reconnectAttempts=0
async function connect(){clearTimeout(reconnectTimer);await initialize();if(!state.error){if((route.meta.auth||route.meta.admin)&&!state.user)await router.replace({path:'/login',query:{redirect:route.fullPath}});else if(route.meta.admin&&!isAdmin.value)await router.replace('/')}if(state.error&&reconnectAttempts++<3)reconnectTimer=setTimeout(connect,15000)}
function reconnectAfterFailure(){if(!state.connecting){reconnectAttempts=0;void connect()}}
onUnmounted(()=>{clearTimeout(reconnectTimer);connectionEvents.removeEventListener('unavailable',reconnectAfterFailure)})
onMounted(()=>{connectionEvents.addEventListener('unavailable',reconnectAfterFailure);void connect()})
</script>

<template>
 <div class="site-shell" @keydown.esc="closePanels">
  <a class="skip-link" href="#main-content" @click.prevent="focusMain">跳至主要内容</a>
  <header class="site-header">
   <div class="global-nav">
    <router-link to="/" class="site-brand" :aria-label="brand.fullName"><Icon name="bike" :size="29"/><span>征途<small>{{brand.school}}</small></span></router-link>
    <nav class="desktop-nav" aria-label="主导航">
     <router-link to="/clubs" :class="{current:route.path==='/clubs'}">了解征途</router-link>
     <router-link to="/activities" :class="{current:route.path==='/activities'}">骑行活动</router-link>
     <router-link to="/suggestions" :class="{current:route.path==='/suggestions'}">活动建议</router-link>
     <button :class="{current:serviceLinks.some(l=>l.path===route.path)}" :aria-expanded="services" aria-controls="service-menu" @click="services=!services;searchOpen=false">骑行服务<Icon name="down" :size="12"/></button>
     <router-link to="/center" :class="{current:route.path==='/center'}">成员中心</router-link>
     <router-link v-if="isAdmin" to="/admin" :class="{current:route.path==='/admin'}">管理后台</router-link>
    </nav>
    <div class="global-actions">
     <button class="nav-icon" aria-label="搜索社团" :aria-expanded="searchOpen" aria-controls="site-search" @click="toggleSearch"><Icon :name="searchOpen?'x':'search'" :size="19"/></button>
     <button class="nav-icon notification-trigger" aria-label="查看消息" @click="notifications=true"><Icon name="bell" :size="19"/><i v-if="notes.length"/></button>
     <router-link v-if="state.user" to="/center" class="account-chip" :aria-label="state.user.name+' · '+(state.user.role==='SUPER_ADMIN'?'超级管理员':isAdmin?'管理员':'成员中心')"><span>{{state.user.name[0]}}</span><b>{{state.user.role==='SUPER_ADMIN'?'超级管理员':state.user.name}}</b></router-link>
     <router-link v-else to="/login" class="nav-login">登录 / 注册</router-link>
     <button class="nav-icon mobile-nav-toggle" :aria-expanded="mobile" aria-controls="mobile-navigation" :aria-label="mobile?'关闭导航':'打开导航'" @click="mobile=!mobile;services=false;searchOpen=false"><Icon :name="mobile?'x':'menu'" :size="23"/></button>
    </div>
   </div>
   <Transition name="nav-reveal"><div v-if="services" id="service-menu" class="service-menu"><div class="service-menu-inner"><p>骑行服务<span>让每一次出发，更从容。</span></p><router-link v-for="item in serviceLinks" :key="item.path" :to="item.path" @click="closePanels"><Icon :name="item.icon" :size="28"/><div><strong>{{item.label}}</strong><span>{{item.description}}</span></div><Icon name="right" :size="19"/></router-link></div></div></Transition>
   <Transition name="nav-reveal"><form v-if="searchOpen" id="site-search" class="site-search" role="search" @submit.prevent="submitSearch"><label for="club-search">探索征途</label><div><Icon name="search" :size="25"/><input id="club-search" ref="searchInput" v-model="search" placeholder="搜索俱乐部与社团…" aria-label="搜索俱乐部与社团"><button type="submit" aria-label="提交搜索"><Icon name="arrow" :size="23"/></button></div><p>搜索俱乐部名称，了解团队与加入方式。</p></form></Transition>
   <Transition name="nav-reveal"><nav v-if="mobile" id="mobile-navigation" class="mobile-navigation" aria-label="手机导航" @click="closePanels"><router-link to="/">俱乐部首页<Icon name="right"/></router-link><router-link to="/clubs">了解征途<Icon name="right"/></router-link><router-link to="/activities">骑行活动<Icon name="right"/></router-link><router-link to="/suggestions">活动建议<Icon name="right"/></router-link><router-link v-for="item in serviceLinks" :key="item.path" :to="item.path">{{item.label}}<Icon name="right"/></router-link><router-link to="/center">成员中心<Icon name="right"/></router-link><router-link v-if="isAdmin" to="/admin">管理后台<Icon name="right"/></router-link></nav></Transition>
  </header>
  <button v-if="services||searchOpen||mobile" class="nav-backdrop" aria-label="关闭展开的导航" @click="closePanels"/>
  <div class="site-context"><div><router-link to="/">{{brand.name}}</router-link><span>{{route.path==='/'?'热爱不设终点':route.meta.title}}</span><router-link to="/clubs" class="context-join">加入征途<Icon name="right" :size="13"/></router-link></div></div>
  <div class="site-workspace">
   <main id="main-content" tabindex="-1">
    <div v-if="state.connecting||state.error" class="connection-status" role="status" aria-live="polite"><span>{{state.connecting?'正在连接骑行服务，服务启动期间可继续浏览俱乐部介绍…':state.error+'。介绍页面可继续浏览，在线申请请等待连接恢复。'}}</span><el-button size="small" :loading="state.connecting" @click="connect">重新连接</el-button></div>
    <div v-if="(route.meta.auth||route.meta.admin)&&!state.ready" class="loading-page"><el-skeleton :rows="6" animated/></div>
    <div v-else-if="(route.meta.auth||route.meta.admin)&&state.error" class="panel error-panel"><h2>骑行服务暂时未连接</h2><p>你的资料不会因连接失败而被清除，请稍后重新连接。</p><el-button type="primary" :loading="state.connecting" @click="connect">重新连接</el-button></div>
    <router-view v-else v-slot="{Component,route:viewRoute}"><Transition name="page" mode="out-in" appear><div :key="viewRoute.path" class="route-page"><component :is="Component"/></div></Transition></router-view>
   </main>
   <footer class="site-footer"><div class="footer-inner"><div class="footer-brand"><Icon name="bike" :size="26"/><b>征途</b><span>安全同行，热爱骑行。</span></div><nav aria-label="页脚导航"><router-link to="/clubs">关于俱乐部</router-link><router-link to="/activities">骑行活动</router-link><router-link to="/rooms">骑行室使用</router-link><router-link to="/equipment">器材服务</router-link><router-link to="/center">成员中心</router-link></nav><div class="footer-bottom"><span>© {{new Date().getFullYear()}} {{brand.fullName}}</span><span>{{today}}</span></div><p class="developer-credit">由25级零向量开发</p></div></footer>
  </div>
  <el-drawer v-model="notifications" title="我的消息" size="min(400px, 100vw)"><div v-if="!state.user" class="empty-block"><Icon name="bell" :size="36"/><h3>登录后查看你的动态</h3><el-button type="primary" @click="notifications=false;router.push('/login')">去登录</el-button></div><template v-else><p class="muted">来自你的申请、借用与报修记录</p><div v-for="(n,i) in notes" :key="i" class="notification"><span class="notification-dot"/><div><h4>{{n.title}}</h4><p>{{n.text}}</p></div></div><el-empty v-if="!notes.length" description="暂时没有新的动态"/><el-button class="full-width" @click="notifications=false;router.push('/center')">查看成员中心</el-button><el-button class="full-width logout-button" @click="run(async()=>{await logout();notifications=false;router.push('/')},'已安全退出')">退出当前账号</el-button></template></el-drawer>
 </div>
</template>
