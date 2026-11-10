<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { brand, clubNews } from '../brand'
import { clubProfile } from '../clubProfile'

const root = ref<HTMLElement | null>(null)
const milestones = [
  { date: '2013', title: '征途从校园出发', summary: clubProfile.brief, url: '' },
  { date: '2014', title: '陕西省高校自行车联盟成立', summary: '由欧亚征途单车牵头，西安市多所高校自行车协会参与，联盟在西安欧亚学院成立。', url: '' },
  { date: '2015', title: '首届陕西省高校自行车交流赛', summary: '赛事在西安欧亚学院举行，宣传材料记载参赛人数 600 余人。', url: '' },
  { date: '2016', title: '承办骑闯天路西安挑战赛', summary: '依托西安欧亚学院举办赛事，并成立欧亚征途车队，开始参加专业自行车赛事。', url: '' },
  { date: '2017', title: '骑沪南下，骑行约 1500 公里', summary: '车队从欧亚校园出发，骑行前往上海同济大学交流。', url: '' },
  { date: '2018', title: '获授高校自行车训练基地', summary: '中国大学生体育协会自行车分会授予西安欧亚学院和西安交通大学自行车训练基地。', url: '' },
  { date: '2019', title: '一带一路 · 横贯欧亚', summary: '开展“逐梦敦煌”丝路高校联动骑行活动。', url: '' },
  { date: '2021', title: '参与第十四届全运会自行车项目执裁', summary: '14 名欧亚师生和校友参与小轮车、公路自行车、山地自行车项目执裁。', url: '' },
  { date: '2021', title: '中国大学生室内自行车云上挑战赛', summary: '征途成员获男子 3 公里计时赛亚军。', url: '' },
  { date: '2025', title: '全国大学生自行车锦标赛', summary: '征途成员李雅泽获场地 400 米淘汰赛亚军。', url: '' },
  { date: '2026', title: '国际大学生自行车赛事两项季军', summary: '征途成员李雅泽获混合团体接力计时赛、女子团体冲刺赛第三名。', url: '' },
  { date: '2026', title: '全国大学生自行车锦标赛', summary: '王晴天、刘书豪、方子睿获田径场男子团体竞速第三名。', url: '' },
  ...[...clubNews].reverse(),
]

let disposed = false
let cleanup: (() => void) | undefined

onMounted(async () => {
  const scene = root.value
  if (!scene || !window.matchMedia) return
  try {
    const [{ gsap }, { ScrollTrigger }] = await Promise.all([
      import('gsap'),
      import('gsap/ScrollTrigger'),
    ])
    if (disposed) return
    gsap.registerPlugin(ScrollTrigger)
    const media = gsap.matchMedia(scene)
    cleanup = () => media.revert()

    media.add({
      desktop: '(min-width: 900px) and (pointer: fine)',
      compact: '(max-width: 899px), (pointer: coarse), (hover: none)',
      reduce: '(prefers-reduced-motion: reduce)',
    }, ({ conditions }) => {
      if (conditions?.reduce) return
      const stage = scene.querySelector('.cinema-stage')!
      const desktop = conditions?.desktop && !conditions.compact
      const header = document.querySelector('.site-header')
      gsap.from('.cinema-intro', {
        opacity: 0.35, y: 18, stagger: 0.12, duration: 0.8, ease: 'power2.out',
      })
      gsap.from('.bike-entry', {
        scale: 0.92, rotation: -2, opacity: 0.55, duration: 1.15, ease: 'power3.out',
      })

      gsap.timeline({
        defaults: { ease: 'none' },
        scrollTrigger: {
          trigger: stage,
          start: desktop ? () => 'top ' + (header?.clientHeight ?? 0) + 'px' : 'top top',
          end: desktop ? () => '+=' + Math.round(stage.clientHeight * 1.25) : 'bottom top',
          pin: desktop,
          scrub: desktop ? 0.42 : true,
          invalidateOnRefresh: true,
        },
      })
        .to('.cinema-bike', {
          xPercent: desktop ? 2 : 0, yPercent: desktop ? 4 : -3,
          scale: desktop ? 1.035 : 1.015, rotation: desktop ? -2 : -1, opacity: 0.94,
        }, 0)
        .to('.cinema-copy', { yPercent: desktop ? -5 : 0 }, 0)
        .to('.cinema-glow', { xPercent: desktop ? -24 : -8, opacity: 0.4 }, 0)
        .fromTo('.cinema-meter i', { scaleX: 0 }, { scaleX: 1 }, 0)

      for (const card of scene.querySelectorAll('.history-card')) {
        gsap.from(card, {
          y: desktop ? 38 : 14, opacity: 0.48, ease: 'none',
          scrollTrigger: { trigger: card, start: 'top 95%', end: 'top 65%', scrub: true },
        })
      }
      gsap.fromTo('.history-progress', { scaleY: 0 }, {
        scaleY: 1, ease: 'none',
        scrollTrigger: { trigger: '.history-list', start: 'top 85%', end: 'bottom 75%', scrub: true },
      })

      // The status banner and public data can resize the page after initial mount.
      let frame = 0
      const observer = typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(() => {
        cancelAnimationFrame(frame)
        frame = requestAnimationFrame(() => ScrollTrigger.refresh())
      })
      if (observer) {
        observer.observe(stage)
        const banner = document.querySelector('.connection-status')
        if (banner) observer.observe(banner)
        const main = document.querySelector('#main-content')
        if (main) observer.observe(main)
      }
      return () => {
        observer?.disconnect()
        cancelAnimationFrame(frame)
      }
    })
  } catch (error) {
    cleanup?.()
    console.warn('首页动效未能初始化，继续显示静态场景。', error)
  }
})

onBeforeUnmount(() => {
  disposed = true
  cleanup?.()
})
</script>

<template>
  <div ref="root" class="home-cinema">
    <section class="zt-hero cinema-stage" aria-labelledby="home-title">
      <div class="cinema-glow" aria-hidden="true"></div>
      <div class="cinema-inner">
        <div class="cinema-copy">
          <p class="cinema-label cinema-intro">{{ brand.fullName }} / EST. 2013</p>
          <h1 id="home-title" class="cinema-intro">把热爱，<br><span>骑成征途。</span></h1>
          <p class="cinema-description cinema-intro">从西安欧亚学院出发。<br>一辆单车，一群伙伴，一段共同的征途。</p>
          <div class="zt-actions cinema-actions"><slot /></div>
        </div>
        <div class="cinema-bike">
          <img class="bike-entry" src="/images/zhengtu-neon-road-bike.jpg"
            width="1200" height="630" alt="霓虹蓝银专业公路赛车侧视图"
            fetchpriority="high" decoding="async">
        </div>
        <div class="cinema-meter" aria-hidden="true"><span>向下滚动 · 进入征途</span><i></i></div>
      </div>
    </section>

    <section class="cinema-history" aria-labelledby="history-title">
      <div class="history-inner">
        <header><p class="cinema-label">ZHENGTU / MILESTONES</p><h2 id="history-title">一路骑来。</h2></header>
        <ol class="history-list">
          <li class="history-track" aria-hidden="true"><i class="history-progress"></i></li>
          <li v-for="item in milestones" :key="item.date" class="history-card">
            <span>{{ item.date }}</span>
            <div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p>
              <a v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer">阅读学校报道 ↗</a>
            </div>
          </li>
        </ol>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-cinema { background: #081018; color: #edf7fa; border-radius: 24px; }
.zt-hero.cinema-stage {
  position: relative; display: grid; align-items: center; isolation: isolate;
  overflow: clip; min-height: calc(100vh - 66px); min-height: calc(100svh - 66px);
  margin: 0; padding: 0; border-radius: 24px 24px 0 0; background: #081018;
}
.cinema-glow { z-index: -2; position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.cinema-glow { background: radial-gradient(ellipse at 72% 50%, #1a708d80, transparent 66%), linear-gradient(130deg, #0b1724, #101f2c 65%, #071019); }
.cinema-stage::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(90deg, #081018 3%, #081018ca 42%, transparent 80%),
    linear-gradient(0deg, #081018, transparent 40%, #08101855); z-index: -1;
}
.cinema-inner {
  position: relative; width: 100%; display: grid; align-items: center;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.18fr); gap: clamp(16px, 3vw, 48px); padding: clamp(24px, 5vw, 84px);
}
.cinema-copy { z-index: 1; min-width: 0; }
.cinema-label { color: #8ed7e4; font: 600 .73rem/1.5 ui-monospace, monospace; letter-spacing: .15em; }
.cinema-copy h1 { color: #fff; font-size: clamp(3rem, 7vw, 7rem); line-height: 1; margin: .24em 0; letter-spacing: -.07em; }
.cinema-copy h1 span { color: #93eaff; background: linear-gradient(110deg,#edfcff,#56d7f0); background-clip: text; -webkit-text-fill-color: transparent; }
.cinema-description, .history-card p { color: #bccfd7; line-height: 1.7; }
.cinema-inner .cinema-actions { justify-content: flex-start; display: flex; gap: 12px; flex-wrap: wrap; margin-top: 24px; }
.cinema-actions :deep(.zt-secondary) { color: #e5f9ff; border-color: #81bfce80; background: #0b1d28; }
.cinema-bike { min-width: 0; padding: 5%; pointer-events: none; mix-blend-mode: screen; }
.bike-entry { display: block; width: 100%; height: auto; }
.cinema-meter { grid-column: 1 / -1; margin-top: clamp(0px, 3vw, 36px); display: grid; gap: 12px; font-size: .75rem; color: #bad2de; }
.cinema-meter i { display: block; width: 100%; height: 1px; transform-origin: left; background: linear-gradient(90deg,#77edff,#77edff22); }
.cinema-history { background: linear-gradient(#081018,#10232f 60%,#142c38); padding: clamp(32px, 7vw, 100px) clamp(20px, 6vw, 96px); border-radius: 0 0 24px 24px; }
.history-inner { max-width: 920px; margin: auto; }
.history-inner h2 { font-size: clamp(2rem,5vw,4rem); margin: .2em 0 .6em; }
.history-list { position: relative; margin: 0; padding: 0 0 0 20px; list-style: none; }
.history-track { position: absolute; inset: 0 auto 0 0; width: 1px; background: #3d5a65; }
.history-progress { display: block; height: 100%; transform-origin: top; background: #68e6ff; }
.history-card { display: grid; grid-template-columns: minmax(90px,.25fr) 1fr; gap: 14px; padding: 20px; margin: 0 0 12px; border: 1px solid #93d1e029; border-radius: 16px; background: #c4eeff08; }
.history-card > span, .history-card a { color: #94e8f7; }
.history-card h3 { margin: 0; color: #edf7fa; }
.history-card p { margin: .45em 0; }
@media (max-width: 899px) {
  .cinema-inner { grid-template-columns: minmax(0, 1fr); gap: 16px; }
  .cinema-bike { max-width: 480px; justify-self: center; width: 100%; padding: 4%; }
  .cinema-stage::after { background: linear-gradient(180deg,#081018e6, #0810188c 64%, #081018); }
  .history-card { grid-template-columns: 1fr; gap: 8px; padding: 16px; }
}
</style>
