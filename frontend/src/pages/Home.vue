<script setup lang="ts">
import {computed,onBeforeUnmount,onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {state,showGuestRecruitment} from '../store'
import {vReveal} from '../motion'
import {clubNews,brand} from '../brand'
import {clubProfile as profile} from '../clubProfile'
import MemberComparison from '../components/MemberComparison.vue'
import Icon from '../components/Icon.vue'
import ActivityCard from '../components/ActivityCard.vue'
import ImmersiveField from '../components/ImmersiveField.vue'
const router=useRouter(),guide=ref(false),heroScene=ref<HTMLElement|null>(null)
let sceneFrame=0,sceneSettleTimer=0,reducedMotion:MediaQueryList|undefined
const upcoming=computed(()=>state.activities.filter(a=>a.status==='PUBLISHED'&&new Date(a.endsAt).getTime()>Date.now()).sort((a,b)=>+new Date(a.startsAt)-+new Date(b.startsAt)))
const stats=computed(()=>[{label:'同行骑友',value:state.overview.memberCount,unit:'人'},{label:'骑行与训练',value:state.overview.activityCount,unit:'场'},{label:'热爱，从这里开始',value:profile.founded,unit:'年'},{label:'共享骑行器材',value:state.overview.equipmentCount,unit:'件'}])
const services=[{path:'/rooms',icon:'bike',title:'骑行室使用',text:'存取车辆、训练与休息，提前安排好你的骑行空间。'},{path:'/equipment',icon:'package',title:'器材借用',text:'按实际库存提交申请，让合适的装备陪你出发。'},{path:'/repairs',icon:'wrench',title:'在线报修',text:'从问题提交到处理完成，维修进度随时可查。'},{path:'/suggestions',icon:'compass',title:'活动建议',text:'有想去的路线？把你的下一程告诉我们。'}]
function updateHeroDepth(){
 const scene=heroScene.value
 if(!scene||reducedMotion?.matches)return
 if(sceneFrame)cancelAnimationFrame(sceneFrame)
 sceneFrame=requestAnimationFrame(()=>{
  sceneFrame=0
  const bounds=scene.getBoundingClientRect()
  const distance=(window.innerHeight*.54-(bounds.top+bounds.height*.5))*.035
  scene.style.setProperty('--scene-depth',Math.max(-14,Math.min(14,distance))+'px')
 })
}
function trackHeroPointer(event:PointerEvent){
 const scene=heroScene.value
 if(!scene||event.pointerType!=='mouse'||reducedMotion?.matches)return
 const bounds=scene.getBoundingClientRect()
 const x=Math.max(0,Math.min(1,(event.clientX-bounds.left)/bounds.width))
 const y=Math.max(0,Math.min(1,(event.clientY-bounds.top)/bounds.height))
 scene.style.setProperty('--scene-tilt-x',((.5-y)*4.4)+'deg')
 scene.style.setProperty('--scene-tilt-y',((x-.5)*5.4)+'deg')
 scene.style.setProperty('--scene-focus-x',(x*100)+'%')
 scene.style.setProperty('--scene-focus-y',(y*100)+'%')
 scene.classList.add('is-pointed')
}
function resetHeroPointer(){
 const scene=heroScene.value
 if(!scene)return
 scene.style.setProperty('--scene-tilt-x','0deg')
 scene.style.setProperty('--scene-tilt-y','0deg')
 scene.style.setProperty('--scene-focus-x','50%')
 scene.style.setProperty('--scene-focus-y','50%')
 scene.classList.remove('is-pointed')
}
function settleHeroScroll(){
 if(reducedMotion?.matches)return
 const scene=heroScene.value
 if(!scene)return
 scene.classList.add('is-scrolling')
 window.clearTimeout(sceneSettleTimer)
 sceneSettleTimer=window.setTimeout(()=>scene.classList.remove('is-scrolling'),140)
 updateHeroDepth()
}
onMounted(()=>{
 reducedMotion=window.matchMedia('(prefers-reduced-motion: reduce)')
 window.addEventListener('scroll',settleHeroScroll,{passive:true})
 window.addEventListener('resize',updateHeroDepth,{passive:true})
 reducedMotion.addEventListener('change',updateHeroDepth)
 updateHeroDepth()
})
onBeforeUnmount(()=>{
 window.removeEventListener('scroll',settleHeroScroll)
 window.removeEventListener('resize',updateHeroDepth)
 reducedMotion?.removeEventListener('change',updateHeroDepth)
 if(sceneFrame)cancelAnimationFrame(sceneFrame)
 window.clearTimeout(sceneSettleTimer)
})
</script>

<template>
 <div class="page-content zt-home">
  <section class="zt-hero" aria-labelledby="home-title"><ImmersiveField/>
   <div class="zt-hero-copy">
    <div class="zt-kicker">{{brand.fullName}}</div>
    <span class="zt-pill">2013 年启程 · 与热爱同行</span>
    <h1 id="home-title">把热爱，<br><span>骑成征途。</span></h1>
    <p>一辆单车，一群伙伴，一段共同的征途。<br>在骑行中遇见风景，也遇见更好的自己。</p>
    <div class="zt-actions"><router-link v-if="showGuestRecruitment" to="/clubs" class="zt-primary">了解征途 · 申请加入<Icon name="right" :size="17"/></router-link><router-link to="/activities" class="zt-secondary">查看骑行活动<Icon name="right" :size="17"/></router-link></div>
   </div>
   <figure ref="heroScene" class="zt-hero-visual home-3d-scene" v-reveal @pointermove="trackHeroPointer" @pointerleave="resetHeroPointer" aria-label="征途车队照片，可随指针和页面滚动产生景深变化"><div class="scene-plane"><img src="/images/zhengtu-team.jpg" alt="征途单车俱乐部车队合影（简介资料图）" fetchpriority="high"><div class="scene-glint" aria-hidden="true"></div><svg class="scene-route" viewBox="0 0 1320 430" preserveAspectRatio="none" aria-hidden="true"><path d="M82 302C208 295 226 221 365 235S519 307 640 239 776 146 915 188s174 94 323 26" pathLength="1"/><circle cx="1225" cy="215" r="5"/></svg><div class="scene-stamp" aria-hidden="true"><span>西安欧亚学院 · 2013 — 至今</span><strong>一段征途，<br>由此出发。</strong><i><span></span></i></div><div class="scene-coordinate" aria-hidden="true"><span>骑行 · 探索 · 同行</span><b>始于欧亚校园</b></div></div><figcaption><span>西安欧亚学院 · 征途单车俱乐部</span><span>俱乐部车队合影 · 简介资料图</span></figcaption></figure>
  </section>
  <div class="zt-stat-grid"><div v-for="(s,i) in stats" :key="s.label" v-reveal="{delay:i*60}"><b>{{s.value??'—'}}<small>{{s.unit}}</small></b><span>{{s.label}}</span></div></div>
  <section class="zt-story"><div class="story-copy" v-reveal><div class="eyebrow">关于征途</div><h2>从校园出发。<br><span>让世界成为你的风景。</span></h2><p>{{profile.brief}}</p><router-link to="/clubs" class="text-link">认识征途单车俱乐部<Icon name="right" :size="17"/></router-link><div class="zt-values"><span><Icon name="shield" :size="18"/>安全同行</span><span><Icon name="users" :size="18"/>团队协作</span><span><Icon name="mountain" :size="18"/>探索成长</span></div></div><figure v-reveal="{kind:'image'}"><img src="/images/zhengtu-anniversary.jpg" alt="征途单车俱乐部十周年自行车运动文化活动现场" loading="lazy"><figcaption>十周年活动现场 · 简介资料图</figcaption></figure></section>
  <MemberComparison v-if="showGuestRecruitment"/>
  <section class="home-section"><div class="section-heading" v-reveal><div><div class="eyebrow">下一程，一起出发</div><h2>下一场，骑向哪里？</h2><p>活动发布、在线报名、现场签到，每一次参与都有记录。</p></div><router-link to="/activities" class="text-link">全部活动<Icon name="right" :size="17"/></router-link></div><div v-if="upcoming.length" class="activity-grid" v-reveal><ActivityCard v-for="a in upcoming.slice(0,3)" :key="a.id" :activity="a" @select="router.push({path:'/activities',query:{activity:a.id}})"/></div><div v-else class="zt-empty-ride panel" v-reveal><span class="empty-ride-icon"><Icon name="bike" :size="46"/></span><div><h3>{{state.publicLoaded?'下一次出发，正在准备中。':'骑行活动数据正在连接'}}</h3><p>新的相遇，值得期待。正式时间、路线与集合地点以活动详情为准。</p></div><router-link :to="['ADMIN','SUPER_ADMIN'].includes(state.user?.role||'')?'/admin':'/activities'" class="text-link">{{['ADMIN','SUPER_ADMIN'].includes(state.user?.role||'')?'前往后台发布活动':'查看活动记录'}}<Icon name="right" :size="17"/></router-link></div></section>
  <section class="home-section service-section"><div class="section-heading" v-reveal><div><div class="eyebrow">骑行之外，也为你想好</div><h2>专注热爱。其他，交给征途。</h2></div></div><div class="zt-services"><router-link v-for="(service,i) in services" :key="service.path" v-reveal="{delay:i*60}" :to="service.path"><Icon :name="service.icon" :size="34"/><h3>{{service.title}}</h3><p>{{service.text}}</p><span>了解服务<Icon name="right" :size="16"/></span></router-link></div></section>
  <section class="zt-news home-section"><div class="section-heading" v-reveal><div><div class="eyebrow">被记录的热爱</div><h2>征途，有故事。</h2><p>来自学校官网的俱乐部报道。</p></div></div><div class="zt-news-grid"><a v-for="(n,i) in clubNews" :key="n.url" :href="n.url" target="_blank" rel="noopener noreferrer" class="zt-news-card" v-reveal="{delay:i*90}"><div class="zt-news-number"><span>0{{i+1}}</span><Icon name="arrowup" :size="24"/></div><span class="category-label">{{n.tag}} · {{n.date}}</span><h3>{{n.title}}</h3><p>{{n.summary}}</p><span class="text-link">阅读学校原文<Icon name="arrowup" :size="16"/></span></a></div></section>
  <section v-if="showGuestRecruitment" class="zt-join-banner" v-reveal><span class="eyebrow">不必一个人出发</span><h2>下一段征途，<br>期待有你。</h2><p>找到你的伙伴，开启属于你的骑行生活。</p><button class="zt-primary" @click="guide=true">第一次加入？查看指引<Icon name="right" :size="17"/></button></section>
 </div>
 <el-dialog v-model="guide" title="征途入社与活动指引" width="min(590px,94vw)"><div class="guide-content"><h3>01 · 提交入社申请</h3><p>注册时选择加入俱乐部或社团并填写资料，招新开放时会提交待审核申请。已有账号可在俱乐部展示申请加入或查看审核结果；成员中心仅保留身份状态和活动、借还记录。</p><h3>02 · 报名与签到</h3><p>查看已发布活动的时间、集合地点和说明，按需报名。签到开放期间，在成员中心输入现场负责人提供的签到码。</p><h3>03 · 器材借还与报修</h3><p>按实际库存申请器材，经审核后领取；使用后提交归还申请，等待管理员验收。器材问题可在线提交报修工单。</p><h3>出发前</h3><p>请阅读具体活动安排，检查车辆与装备，遵守交通规则。未发布的路线、时间和联系方式以俱乐部确认信息为准。</p></div><template #footer><el-button type="primary" @click="guide=false;router.push('/clubs')">前往入社申请</el-button></template></el-dialog>
</template>
<style scoped>
.home-3d-scene{--scene-depth:0px;--scene-tilt-x:0deg;--scene-tilt-y:0deg;--scene-focus-x:50%;--scene-focus-y:50%;perspective:1500px;transform-style:preserve-3d;isolation:isolate}
.home-3d-scene .scene-plane{position:absolute;inset:0;transform-style:preserve-3d;transform:rotateX(var(--scene-tilt-x)) rotateY(var(--scene-tilt-y)) translate3d(0,var(--scene-depth),0);transition:transform .72s cubic-bezier(.2,.7,.15,1)}
.home-3d-scene.is-pointed .scene-plane{transition-duration:90ms}
.home-3d-scene.is-scrolling .scene-plane{transition:none}
.home-3d-scene .scene-plane>img{position:absolute;inset:-2%;width:104%;height:104%;max-width:none;object-fit:cover;object-position:center 54%;display:block;transform:translateZ(-18px) scale(1.045);transition:transform .8s cubic-bezier(.2,.7,.15,1)}
.home-3d-scene.is-pointed .scene-plane>img{transform:translateZ(-18px) scale(1.075)}
.scene-glint{position:absolute;inset:0;pointer-events:none;background:radial-gradient(ellipse 38% 64% at var(--scene-focus-x) var(--scene-focus-y),#ffffff35,transparent 78%);mix-blend-mode:screen;opacity:0;transform:translateZ(28px);transition:opacity .4s ease}
.home-3d-scene.is-pointed .scene-glint{opacity:1}
.scene-route{position:absolute;inset:0;width:100%;height:100%;overflow:visible;pointer-events:none;transform:translateZ(38px);filter:drop-shadow(0 0 5px #b7ddffbb)}
.scene-route path{fill:none;stroke:#fff;stroke-width:1.4;stroke-linecap:round;stroke-dasharray:.018 .012;animation:route-drift 18s linear infinite;opacity:.78}
.scene-route circle{fill:#fff;filter:drop-shadow(0 0 6px #7fc2ff);animation:route-pulse 2.8s ease-in-out infinite}
.scene-stamp{position:absolute;top:30px;left:32px;padding:16px 20px 15px;border:1px solid #ffffff6b;border-radius:17px;background:#141b1f39;color:#fff;box-shadow:0 14px 38px #0c151c28;backdrop-filter:blur(16px) saturate(145%);-webkit-backdrop-filter:blur(16px) saturate(145%);transform:translateZ(58px);transition:transform .7s cubic-bezier(.2,.7,.15,1)}
.home-3d-scene.is-pointed .scene-stamp{transform:translate3d(0,-5px,68px)}
.scene-stamp>span,.scene-coordinate>span{display:block;font-size:10px;letter-spacing:.12em;color:#ffffffe0}
.scene-stamp>strong{display:block;margin-top:11px;font-size:23px;line-height:1.38;font-weight:550;letter-spacing:-.03em}
.scene-stamp>i{position:relative;display:block;width:100%;height:2px;margin-top:14px;border-radius:2px;background:#ffffff4a;overflow:hidden}
.scene-stamp>i>span{position:absolute;inset:0 37% 0 0;background:#fff;box-shadow:0 0 8px #fff}
.scene-coordinate{position:absolute;right:30px;top:31px;display:flex;align-items:flex-end;flex-direction:column;gap:8px;color:#fff;text-shadow:0 1px 14px #0008;transform:translateZ(46px);transition:transform .7s cubic-bezier(.2,.7,.15,1)}
.home-3d-scene.is-pointed .scene-coordinate{transform:translate3d(0,-4px,56px)}
.scene-coordinate>b{font-size:11px;font-weight:500;letter-spacing:.09em}
.zt-home .zt-services>a{transform-style:preserve-3d;transition:transform .38s cubic-bezier(.2,.7,.15,1),box-shadow .38s cubic-bezier(.2,.7,.15,1)}
.zt-home .zt-services>a>svg{transform:translateZ(16px);transition:transform .38s cubic-bezier(.2,.7,.15,1)}
.zt-home .zt-services>a h3,.zt-home .zt-services>a p,.zt-home .zt-services>a>span{transform:translateZ(8px);transition:transform .38s cubic-bezier(.2,.7,.15,1)}
@keyframes route-drift{to{stroke-dashoffset:-.6}}
@keyframes route-pulse{50%{opacity:.55;r:7px}}
@media(hover:hover) and (pointer:fine){.zt-home .zt-services>a:hover{transform:perspective(900px) rotateX(1.4deg) rotateY(-1.8deg) translateY(-5px);box-shadow:0 24px 46px #0000000d}.zt-home .zt-services>a:hover>svg{transform:translate3d(0,-2px,24px)}}
@media(max-width:600px){.home-3d-scene{perspective:1050px}.home-3d-scene .scene-plane{transition-duration:.34s}.home-3d-scene.is-scrolling .scene-plane{transition:none}.home-3d-scene .scene-plane>img{transform:translateZ(-10px) scale(1.075);object-position:56% center}.scene-stamp{top:16px;left:15px;padding:11px 13px;border-radius:13px;backdrop-filter:blur(12px)}.scene-stamp>span,.scene-coordinate>span{font-size:8px;letter-spacing:.08em}.scene-stamp>strong{font-size:17px;margin-top:7px}.scene-stamp>i{margin-top:9px}.scene-coordinate{top:17px;right:14px;gap:6px}.scene-coordinate>b{font-size:9px}.scene-route{transform:translateZ(19px)}.zt-home .zt-services>a:active{transform:perspective(700px) translateZ(8px) scale(.985)}.zt-home .zt-services>a>svg{transform:none}.zt-home .zt-services>a h3,.zt-home .zt-services>a p,.zt-home .zt-services>a>span{transform:none}}
@media(prefers-reduced-motion:reduce){.home-3d-scene .scene-plane,.home-3d-scene .scene-plane>img,.scene-glint,.scene-stamp,.scene-coordinate,.zt-home .zt-services>a,.zt-home .zt-services>a>*{transition:none!important;animation:none!important}.home-3d-scene .scene-plane,.home-3d-scene .scene-plane>img,.scene-route,.scene-stamp,.scene-coordinate{transform:none!important}.scene-glint{display:none}}
</style>
