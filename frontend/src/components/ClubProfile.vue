<script setup lang="ts">
import{computed}from'vue';import{vReveal}from'../motion';import{showGuestRecruitment}from'../store';import type{Club}from'../types';import{clubProfile as profile}from'../clubProfile';import ClubHistory from'./ClubHistory.vue';import Icon from'./Icon.vue';const props=defineProps<{club:Club}>();defineEmits<{join:[]}>();const introduction=computed(()=>props.club.description.match(/[^。！？]+[。！？]?/g)?.slice(0,2).join('')||profile.introduction)
</script>
<template><div class="club-profile"><section class="profile-lead panel"><div class="profile-lead-copy"><div class="profile-logos"><img src="/images/eurasia-logo.png" alt="西安欧亚学院标识"><span/><img src="/images/zhengtu-logo.png" alt="征途单车俱乐部标识"></div><span class="eyebrow">始于 {{profile.foundingDay}}</span><h2>一群热爱单车的人，<br>一段不断向前的征途。</h2><p>{{introduction}}</p><button class="zt-primary" @click="$emit('join')">{{showGuestRecruitment?'了解招新 · 申请加入':'填写入社申请'}}<Icon name="arrowup" :size="18"/></button><router-link to="/clubs#club-history" class="history-anchor">浏览征途发展史<Icon name="arrow" :size="16"/></router-link></div><figure><img src="/images/zhengtu-team.jpg" alt="征途单车俱乐部车队在西安欧亚学院的合影"><figcaption>俱乐部车队合影 · 简介资料图</figcaption></figure></section>
<div class="profile-context"><span><Icon name="graduation"/>西安欧亚学院通识教育学院体育中心</span><p>{{profile.development}}</p></div>
<section class="profile-direction-grid"><article v-for="(d,i) in profile.directions" :key="d.title" v-reveal="{delay:i*70}" class="panel"><span class="club-icon"><Icon :name="d.icon" :size="25"/></span><h3>{{d.title}}</h3><p>{{d.text}}</p></article></section>
<ClubHistory />
<section class="profile-partners" v-reveal><div><h3>一起支持热爱的力量</h3><p>简介所列合作支持，具体合作以俱乐部后续公布的信息为准。</p></div><div class="partner-tags"><span v-for="p in profile.partners" :key="p">{{p}}</span></div></section>
<div class="profile-apply panel" v-reveal><div><h3>下一段征途，期待有你。</h3><p>注册账号后填写加入理由，审核结果可在成员中心查看。</p></div><button class="zt-primary" @click="$emit('join')">提交入社申请<Icon name="arrowup" :size="18"/></button></div></div></template>

<style scoped>
.history-anchor{display:inline-flex;align-items:center;gap:7px;margin-top:17px;color:#56744f;font-size:12px;text-decoration:none}.history-anchor:hover{color:#264b32}
</style>
