<script setup lang="ts">
import {computed,ref} from 'vue'
import {useRouter} from 'vue-router'
import {state} from '../store'
import {vReveal} from '../motion'
import {clubNews,brand} from '../brand'
import {clubProfile as profile} from '../clubProfile'
import MemberComparison from '../components/MemberComparison.vue'
import Icon from '../components/Icon.vue'
import ActivityCard from '../components/ActivityCard.vue'
import ImmersiveField from '../components/ImmersiveField.vue'
const router=useRouter(),guide=ref(false)
const upcoming=computed(()=>state.activities.filter(a=>a.status==='PUBLISHED'&&new Date(a.endsAt).getTime()>Date.now()).sort((a,b)=>+new Date(a.startsAt)-+new Date(b.startsAt)))
const stats=computed(()=>[{label:'同行骑友',value:state.overview.memberCount,unit:'人'},{label:'骑行与训练',value:state.overview.activityCount,unit:'场'},{label:'热爱，从这里开始',value:profile.founded,unit:'年'},{label:'共享骑行器材',value:state.overview.equipmentCount,unit:'件'}])
const services=[{path:'/rooms',icon:'bike',title:'骑行室使用',text:'存取车辆、训练与休息，提前安排好你的骑行空间。'},{path:'/equipment',icon:'package',title:'器材借用',text:'按实际库存提交申请，让合适的装备陪你出发。'},{path:'/repairs',icon:'wrench',title:'在线报修',text:'从问题提交到处理完成，维修进度随时可查。'},{path:'/suggestions',icon:'compass',title:'活动建议',text:'有想去的路线？把你的下一程告诉我们。'}]
</script>

<template>
 <div class="page-content zt-home">
  <section class="zt-hero" aria-labelledby="home-title"><ImmersiveField/>
   <div class="zt-hero-copy">
    <div class="zt-kicker">{{brand.fullName}}</div>
    <span class="zt-pill">2013 年启程 · 与热爱同行</span>
    <h1 id="home-title">把热爱，<br><span>骑成征途。</span></h1>
    <p>一辆单车，一群伙伴，一段共同的征途。<br>在骑行中遇见风景，也遇见更好的自己。</p>
    <div class="zt-actions"><router-link to="/clubs" class="zt-primary">了解征途 · 申请加入<Icon name="right" :size="17"/></router-link><router-link to="/activities" class="zt-secondary">查看骑行活动<Icon name="right" :size="17"/></router-link></div>
   </div>
   <figure class="zt-hero-visual" v-reveal="{kind:'image'}"><img src="/images/zhengtu-team.jpg" alt="征途单车俱乐部车队合影（简介资料图）" fetchpriority="high"><figcaption><span>西安欧亚学院 · 征途单车俱乐部</span><span>俱乐部车队合影 · 简介资料图</span></figcaption></figure>
  </section>
  <div class="zt-stat-grid"><div v-for="(s,i) in stats" :key="s.label" v-reveal="{delay:i*60}"><b>{{s.value??0}}<small>{{s.unit}}</small></b><span>{{s.label}}</span></div></div>
  <section class="zt-story"><div class="story-copy" v-reveal><div class="eyebrow">关于征途</div><h2>从校园出发。<br><span>让世界成为你的风景。</span></h2><p>{{profile.brief}}</p><router-link to="/clubs" class="text-link">认识征途单车俱乐部<Icon name="right" :size="17"/></router-link><div class="zt-values"><span><Icon name="shield" :size="18"/>安全同行</span><span><Icon name="users" :size="18"/>团队协作</span><span><Icon name="mountain" :size="18"/>探索成长</span></div></div><figure v-reveal="{kind:'image'}"><img src="/images/zhengtu-anniversary.jpg" alt="征途单车俱乐部十周年自行车运动文化活动现场" loading="lazy"><figcaption>十周年活动现场 · 简介资料图</figcaption></figure></section>
  <MemberComparison/>
  <section class="home-section"><div class="section-heading" v-reveal><div><div class="eyebrow">下一程，一起出发</div><h2>下一场，骑向哪里？</h2><p>活动发布、在线报名、现场签到，每一次参与都有记录。</p></div><router-link to="/activities" class="text-link">全部活动<Icon name="right" :size="17"/></router-link></div><div v-if="upcoming.length" class="activity-grid" v-reveal><ActivityCard v-for="a in upcoming.slice(0,3)" :key="a.id" :activity="a" @select="router.push({path:'/activities',query:{activity:a.id}})"/></div><div v-else class="zt-empty-ride panel" v-reveal><span class="empty-ride-icon"><Icon name="bike" :size="46"/></span><div><h3>下一次出发，正在准备中。</h3><p>新的相遇，值得期待。正式时间、路线与集合地点以活动详情为准。</p></div><router-link :to="['ADMIN','SUPER_ADMIN'].includes(state.user?.role||'')?'/admin':'/activities'" class="text-link">{{['ADMIN','SUPER_ADMIN'].includes(state.user?.role||'')?'前往后台发布活动':'查看活动记录'}}<Icon name="right" :size="17"/></router-link></div></section>
  <section class="home-section service-section"><div class="section-heading" v-reveal><div><div class="eyebrow">骑行之外，也为你想好</div><h2>专注热爱。其他，交给征途。</h2></div></div><div class="zt-services"><router-link v-for="(service,i) in services" :key="service.path" v-reveal="{delay:i*60}" :to="service.path"><Icon :name="service.icon" :size="34"/><h3>{{service.title}}</h3><p>{{service.text}}</p><span>了解服务<Icon name="right" :size="16"/></span></router-link></div></section>
  <section class="zt-news home-section"><div class="section-heading" v-reveal><div><div class="eyebrow">被记录的热爱</div><h2>征途，有故事。</h2><p>来自学校官网的俱乐部报道。</p></div></div><div class="zt-news-grid"><a v-for="(n,i) in clubNews" :key="n.url" :href="n.url" target="_blank" rel="noopener noreferrer" class="zt-news-card" v-reveal="{delay:i*90}"><div class="zt-news-number"><span>0{{i+1}}</span><Icon name="arrowup" :size="24"/></div><span class="category-label">{{n.tag}} · {{n.date}}</span><h3>{{n.title}}</h3><p>{{n.summary}}</p><span class="text-link">阅读学校原文<Icon name="arrowup" :size="16"/></span></a></div></section>
  <section class="zt-join-banner" v-reveal><span class="eyebrow">不必一个人出发</span><h2>下一段征途，<br>期待有你。</h2><p>找到你的伙伴，开启属于你的骑行生活。</p><button class="zt-primary" @click="guide=true">第一次加入？查看指引<Icon name="right" :size="17"/></button></section>
 </div>
 <el-dialog v-model="guide" title="征途入社与活动指引" width="min(590px,94vw)"><div class="guide-content"><h3>01 · 提交入社申请</h3><p>注册时选择加入俱乐部或社团并填写资料，招新开放时会提交待审核申请。已有账号可在俱乐部展示申请加入或查看审核结果；成员中心仅保留身份状态和活动、借还记录。</p><h3>02 · 报名与签到</h3><p>查看已发布活动的时间、集合地点和说明，按需报名。签到开放期间，在成员中心输入现场负责人提供的签到码。</p><h3>03 · 器材借还与报修</h3><p>按实际库存申请器材，经审核后领取；使用后提交归还申请，等待管理员验收。器材问题可在线提交报修工单。</p><h3>出发前</h3><p>请阅读具体活动安排，检查车辆与装备，遵守交通规则。未发布的路线、时间和联系方式以俱乐部确认信息为准。</p></div><template #footer><el-button type="primary" @click="guide=false;router.push('/clubs')">前往入社申请</el-button></template></el-dialog>
</template>
