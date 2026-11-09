<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { brand, clubNews } from '../brand'
import { clubProfile } from '../clubProfile'

const root = ref<HTMLElement | null>(null)
const milestones = [
  { date: '2013', title: '从校园出发', summary: clubProfile.brief, url: '' },
  { date: '十周年', title: clubProfile.highlights[1], summary: '用自行车运动文化展览记录车队的成长。', url: '' },
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
        scale: 0.91, rotation: -5, opacity: 0.4, duration: 1.15, ease: 'power3.out',
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
          xPercent: desktop ? -22 : 0, yPercent: desktop ? 8 : -8,
          scale: desktop ? 1.17 : 1.05, rotation: desktop ? -9 : -3, opacity: 0.82,
        }, 0)
        .to('.cinema-copy', { yPercent: desktop ? -9 : -3 }, 0)
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
          <svg class="bike-entry" viewBox="0 0 660 370" fill="none" role="img" aria-labelledby="road-bike-title">
            <title id="road-bike-title">专业公路车侧视图</title>
            <defs>
              <linearGradient id="road-frame" x1="210" y1="99" x2="430" y2="295" gradientUnits="userSpaceOnUse">
                <stop stop-color="#effaff"/><stop offset=".37" stop-color="#9dbacb"/>
                <stop offset=".63" stop-color="#405a6e"/><stop offset="1" stop-color="#78d6e4"/>
              </linearGradient>
              <linearGradient id="road-rim" x2="1" y2="1">
                <stop stop-color="#b4d5e1"/><stop offset=".52" stop-color="#253e50"/><stop offset="1" stop-color="#91b7c8"/>
              </linearGradient>
              <g id="road-wheel">
                <circle r="104" fill="#091724" fill-opacity=".36" stroke="#1a2937" stroke-width="9"/>
                <circle r="103" stroke="#aec7d2" stroke-opacity=".65" stroke-width="1.5"/>
                <circle r="97" stroke="url(#road-rim)" stroke-width="8"/>
                <path d="M0-92V92M-92 0H92M-65-65L65 65M-65 65L65-65M-35-85L35 85M-85-35L85 35M-85 35L85-35M-35 85L35-85" stroke="#a4c6d4" stroke-opacity=".46"/>
                <circle r="22" stroke="#9dbcc8" stroke-width="3" stroke-dasharray="5 2"/>
                <circle r="6" fill="#c2dae2" stroke="#314858" stroke-width="2"/>
              </g>
            </defs>
            <path d="M16 346H642" stroke="#83d8e7" stroke-opacity=".24"/>
            <use href="#road-wheel" x="128" y="238"/>
            <use href="#road-wheel" x="533" y="238"/>
            <g stroke-linecap="round" stroke-linejoin="round">
              <path d="M128 238L240 112L280 266L128 238Z" stroke="url(#road-frame)" stroke-width="10"/>
              <path d="M235 107Q312 96 448 79L461 121Q443 131 423 151L289 271L276 259L426 114Q437 102 415 103L242 122Z"
                fill="url(#road-frame)" stroke="#c5e1e9" stroke-opacity=".55" stroke-width="1.5"/>
              <path d="M240 111L219 53M459 119Q471 129 481 149L533 238" stroke="url(#road-frame)" stroke-width="14"/>
              <path d="M459 119Q471 129 481 149L533 238" stroke="#b7d3e0" stroke-opacity=".58" stroke-width="3"/>
              <path d="M193 46Q208 43 227 48L252 48Q256 48 260 54L220 58Q203 55 193 46Z" fill="#c0d6df" stroke="#304b5d" stroke-width="2"/>
              <path d="M446 79L439 61L483 49" stroke="#8faab8" stroke-width="10"/>
              <path d="M479 50C502 39 536 40 550 52C566 66 556 94 521 101" stroke="#8ca7b7" stroke-width="10"/>
              <path d="M481 50C503 41 536 42 550 52C566 66 556 94 521 101" stroke="#273e50" stroke-width="7" stroke-dasharray="5 2"/>
              <path d="M548 50Q567 45 573 34Q583 30 585 41L575 55L578 80" stroke="#bed1dc" stroke-width="5"/>
              <path d="M575 55L583 89" stroke="#6f8d9f" stroke-width="3"/>
              <path d="M128 225L278 247M128 252L278 285" stroke="#9bb4bf" stroke-width="2" stroke-dasharray="3 2"/>
              <circle cx="280" cy="266" r="23" fill="#122531" stroke="#abc6d1" stroke-width="4" stroke-dasharray="4 1"/>
              <circle cx="280" cy="266" r="14" stroke="#7290a2" stroke-width="3"/>
              <path d="M280 266L316 273H329M118 246L111 269L122 271" stroke="#a6c1cc" stroke-width="5"/>
            </g>
            <text x="332" y="222" transform="rotate(-43 332 222)" fill="#132d3c" font-size="10" font-weight="700" letter-spacing="2">ZHENGTU</text>
          </svg>
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
.cinema-glow { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.cinema-glow { background: radial-gradient(ellipse at 72% 50%, #1a708d80, transparent 66%), linear-gradient(130deg, #0b1724, #101f2c 65%, #071019); }
.cinema-stage::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(90deg, #081018 3%, #081018ca 42%, transparent 80%),
    linear-gradient(0deg, #081018, transparent 40%, #08101855); z-index: 0;
}
.cinema-inner {
  z-index: 1; position: relative; width: 100%; display: grid; align-items: center;
  grid-template-columns: 1fr 1.18fr; gap: 1rem; padding: clamp(24px, 5vw, 84px);
}
.cinema-copy { z-index: 1; min-width: 0; }
.cinema-label { color: #8ed7e4; font: 600 .73rem/1.5 ui-monospace, monospace; letter-spacing: .15em; }
.cinema-copy h1 { color: #fff; font-size: clamp(3rem, 7vw, 7rem); line-height: 1; margin: .24em 0; letter-spacing: -.07em; }
.cinema-copy h1 span { color: #93eaff; background: linear-gradient(110deg,#edfcff,#56d7f0); background-clip: text; -webkit-text-fill-color: transparent; }
.cinema-description, .history-card p { color: #bccfd7; line-height: 1.7; }
.cinema-inner .cinema-actions { justify-content: flex-start; display: flex; gap: 12px; flex-wrap: wrap; margin-top: 24px; }
.cinema-actions :deep(.zt-secondary) { color: #e5f9ff; border-color: #81bfce80; background: #0b1d28; }
.cinema-bike { min-width: 0; pointer-events: none; }
.bike-entry { display: block; width: 100%; height: auto; filter: drop-shadow(0 0 12px #41c6ee50); }
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
  .cinema-inner { grid-template-columns: 1fr; gap: 10px; }
  .cinema-bike { max-width: 480px; justify-self: center; width: 100%; }
  .cinema-stage::after { background: linear-gradient(180deg,#081018e6, #0810188c 64%, #081018); }
  .history-card { grid-template-columns: 1fr; gap: 8px; padding: 16px; }
}
</style>
