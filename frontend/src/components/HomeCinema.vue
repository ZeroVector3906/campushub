<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { brand, clubNews } from '../brand'
import { clubHistoryItems } from '../clubHistory'

const root = ref<HTMLElement | null>(null)
const milestones = [
  ...clubHistoryItems.map(item => ({ date: item.year, title: item.title, summary: item.summary, url: '' })),
  ...[...clubNews].reverse(),
]
const yearOf = (date: string) => date.match(/\d{4}/)?.[0] ?? date
const yearGroups = [...new Set(milestones.map(item => yearOf(item.date)))]
  .sort((a, b) => Number(a) - Number(b))
  .map(year => ({ year, items: milestones.filter(item => yearOf(item.date) === year) }))

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

      for (const card of scene.querySelectorAll('.history-year-card')) {
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
          <li v-for="group in yearGroups" :key="group.year" class="history-year-card">
            <span class="history-year-label">{{ group.year }}</span>
            <ul class="history-year-facts">
              <li v-for="item in group.items" :key="`${item.date}-${item.title}-${item.summary}`" class="history-year-fact">
                <h3>{{ item.title }}</h3><p>{{ item.summary }}</p>
                <a v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer">阅读学校报道 ↗</a>
              </li>
            </ul>
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
.cinema-description, .history-year-fact p { color: #bccfd7; line-height: 1.7; }
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
.history-year-card { display: grid; grid-template-columns: minmax(74px,.2fr) minmax(0,1fr); align-items: start; gap: 18px; padding: 20px; margin: 0 0 12px; border: 1px solid #93d1e029; border-radius: 16px; background: #c4eeff08; }
.history-year-label { color: #94e8f7; font-size: 1.15rem; font-weight: 650; }
.history-year-facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 250px), 1fr)); gap: 14px 22px; margin: 0; padding: 0; list-style: none; }
.history-year-fact h3 { margin: 0; color: #edf7fa; font-size: 1rem; }
.history-year-fact p { margin: .45em 0; }
.history-year-fact a { color: #94e8f7; }
@media (max-width: 899px) {
  .cinema-inner { grid-template-columns: minmax(0, 1fr); gap: 16px; }
  .cinema-bike { max-width: 480px; justify-self: center; width: 100%; padding: 4%; }
  .cinema-stage::after { background: linear-gradient(180deg,#081018e6, #0810188c 64%, #081018); }
  .history-year-card { grid-template-columns: 1fr; gap: 10px; padding: 16px; }
  .history-year-facts { grid-template-columns: 1fr; gap: 14px; }
}
</style>
