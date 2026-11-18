<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { brand, clubNews } from '../brand'
import { clubHistoryItems } from '../clubHistory'
import Icon from './Icon.vue'
import HomeAtmosphere from './HomeAtmosphere.vue'

const scenes = [
  { label: '骑向征途', detail: '公路单车', image: '/images/zhengtu-neon-road-bike.jpg', thumb: '/images/zhengtu-neon-road-bike.jpg', alt: '霓虹蓝银公路赛车侧视图', width: 1200, height: 630 },
  { label: '与伙伴同行', detail: '团队合照', image: '/images/zhengtu-team.jpg', thumb: '/images/zhengtu-team-thumb.webp', alt: '征途单车俱乐部团队合影', width: 1616, height: 1080 },
  { label: '一路骑来', detail: '十周年现场', image: '/images/zhengtu-anniversary.jpg', thumb: '/images/zhengtu-anniversary-thumb.webp', alt: '征途单车俱乐部十周年自行车运动文化活动现场', width: 1269, height: 846 },
] as const
const milestones = [
  ...clubHistoryItems.map(item => ({ date: item.year, title: item.title, summary: item.summary, url: '' })),
  ...[...clubNews].reverse(),
]
const yearOf = (date: string) => date.match(/\d{4}/)?.[0] ?? date
const yearGroups = [...new Set(milestones.map(item => yearOf(item.date)))]
  .sort((a, b) => Number(a) - Number(b))
  .map(year => ({ year, items: milestones.filter(item => yearOf(item.date) === year) }))

const root = ref<HTMLElement | null>(null)
const track = ref<HTMLElement | null>(null)
const stage = ref<HTMLElement | null>(null)
const historyTitle = ref<HTMLElement | null>(null)
const historyIndex = ref<HTMLElement | null>(null)
const sceneIndex = ref(0)
const projectedScene = ref(0)
const readyImages = new Set<number>()
const loadedScenes = ref(new Set([0]))
const activeYear = ref(yearGroups[0]!.year)
const sceneNumber = (index: number) => String(index + 1).padStart(2, '0')
const clamp = (value: number) => Math.min(1, Math.max(0, value))

let frame = 0
let pointerX = 0
let pointerY = 0
let sceneOnscreen = true
let scrollChapter = 0
let scrollYAtLastFrame: number | undefined
let yearCards: HTMLElement[] = []
let header: HTMLElement | null = null
let motion: MediaQueryList | undefined
let finePointer: MediaQueryList | undefined
let compact: MediaQueryList | undefined
let cleanup: (() => void) | undefined

function selectScene(index: number) {
  sceneIndex.value = (index + scenes.length) % scenes.length
  loadedScenes.value.add(sceneIndex.value)
  if (readyImages.has(sceneIndex.value)) projectedScene.value = sceneIndex.value
}
function imageReady(index: number) {
  readyImages.add(index)
  if (index === sceneIndex.value) projectedScene.value = index
}
function sceneKey(event: KeyboardEvent, index: number) {
  const next = event.key === 'ArrowRight' ? index + 1 : event.key === 'ArrowLeft' ? index - 1
    : event.key === 'Home' ? 0 : event.key === 'End' ? scenes.length - 1 : null
  if (next === null) return
  event.preventDefault()
  selectScene(next)
  root.value?.querySelectorAll<HTMLButtonElement>('.stage-choice')[sceneIndex.value]?.focus({ preventScroll: true })
}
function pageArrow(event: KeyboardEvent) {
  if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey ||
      !sceneOnscreen || (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight')) return
  // Leave typed input, navigation, dialogs and other focused controls in charge of their keys.
  if (document.activeElement !== document.body && document.activeElement !== document.querySelector('#main-content')) return
  if (!stage.value || stage.value.getBoundingClientRect().bottom < window.innerHeight * .55) return
  event.preventDefault()
  selectScene(sceneIndex.value + (event.key === 'ArrowRight' ? 1 : -1))
}
function movePointer(event: PointerEvent) {
  if (!finePointer?.matches || motion?.matches || event.pointerType !== 'mouse' || !stage.value) return
  const bounds = stage.value.getBoundingClientRect()
  pointerX = (clamp((event.clientX - bounds.left) / bounds.width) - 0.5) * 2
  pointerY = (clamp((event.clientY - bounds.top) / bounds.height) - 0.5) * 2
  queueFrame()
}
function resetPointer() {
  pointerX = 0
  pointerY = 0
  queueFrame()
}
function queueFrame() {
  if (!frame) frame = requestAnimationFrame(updateScroll)
}
function updateScroll() {
  frame = 0
  if (!stage.value || !track.value || !root.value) return
  // Read positions together, then update presentation without driving scroll.
  const trackRect = track.value.getBoundingClientRect()
  const stageHeight = stage.value.clientHeight
  const stickyTravel = Math.max(0, trackRect.height - stageHeight)
  const progress = clamp(-trackRect.top / (stickyTravel || stageHeight || 1))
  const scrollOffset = (header?.offsetHeight ?? 76) + 14 +
    (compact?.matches ? (historyIndex.value?.offsetHeight ?? 0) + 6 : 0)
  let year = yearGroups[0]!.year
  for (const card of yearCards) {
    if (card.getBoundingClientRect().top > scrollOffset + 2) break
    year = card.dataset.historyYear ?? year
  }
  activeYear.value = year
  root.value.style.setProperty('--history-stop', scrollOffset + 'px')
  // Re-evaluate the chapter only when native scrolling crosses a boundary.
  // A manual choice remains in place during pointer movement or small scrolls within a chapter.
  if (scrollYAtLastFrame !== window.scrollY && !motion?.matches) {
    const nextChapter = progress < .30 ? 0 : progress < .70 ? 1 : 2
    if (nextChapter !== scrollChapter) selectScene(nextChapter)
    scrollChapter = nextChapter
  }
  scrollYAtLastFrame = window.scrollY
  const moving = !motion?.matches && sceneOnscreen
  stage.value.style.setProperty('--scene-progress', moving ? progress.toFixed(3) : '0')
  stage.value.style.setProperty('--scene-x', moving ? (pointerX * 13).toFixed(2) + 'px' : '0px')
  stage.value.style.setProperty('--scene-y', moving ? (pointerY * 9).toFixed(2) + 'px' : '0px')
}
function scrollTo(target: HTMLElement | null | undefined) {
  if (!target) return
  target.focus({ preventScroll: true })
  target.scrollIntoView?.({ block: 'start', behavior: motion?.matches ? 'auto' : 'smooth' })
}
function jumpToYear(year: string) {
  scrollTo(root.value?.querySelector<HTMLElement>('[data-history-year="' + year + '"]'))
}

onMounted(() => {
  motion = window.matchMedia?.('(prefers-reduced-motion: reduce)')
  finePointer = window.matchMedia?.('(hover: hover) and (pointer: fine)')
  compact = window.matchMedia?.('(max-width: 740px)')
  header = document.querySelector('.site-header')
  yearCards = Array.from(root.value?.querySelectorAll<HTMLElement>('[data-history-year]') ?? [])
  const preloadTimer = window.setTimeout(() => {
    for (let index = 1; index < scenes.length; index++) loadedScenes.value.add(index)
  }, 650)
  motion?.addEventListener?.('change', resetPointer)
  finePointer?.addEventListener?.('change', resetPointer)
  window.addEventListener('keydown', pageArrow)
  window.addEventListener('scroll', queueFrame, { passive: true })
  window.addEventListener('resize', queueFrame, { passive: true })
  const resize = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(queueFrame)
  if (root.value) resize?.observe(root.value)
  if (header) resize?.observe(header)
  if (historyIndex.value) resize?.observe(historyIndex.value)
  const visibility = typeof IntersectionObserver === 'undefined' ? null : new IntersectionObserver(([entry]) => {
    sceneOnscreen = entry?.isIntersecting ?? true
    stage.value?.style.setProperty('--stage-play', sceneOnscreen ? 'running' : 'paused')
    if (!sceneOnscreen) resetPointer()
    else queueFrame()
  })
  if (stage.value) visibility?.observe(stage.value)
  queueFrame()
  cleanup = () => {
    window.clearTimeout(preloadTimer)
    window.removeEventListener('keydown', pageArrow)
    window.removeEventListener('scroll', queueFrame)
    window.removeEventListener('resize', queueFrame)
    motion?.removeEventListener?.('change', resetPointer)
    finePointer?.removeEventListener?.('change', resetPointer)
    resize?.disconnect()
    visibility?.disconnect()
    cancelAnimationFrame(frame)
  }
})
onBeforeUnmount(() => cleanup?.())
</script>

<template>
  <div ref="root" class="home-cinema">
    <div ref="track" class="cinema-track">
      <section ref="stage" class="zt-hero cinema-stage" :data-scene="projectedScene" aria-labelledby="home-title"
        @pointermove="movePointer" @pointerleave="resetPointer">
        <HomeAtmosphere />
        <div class="stage-layout">
          <div class="stage-copy">
            <p class="stage-kicker"><span aria-hidden="true"></span>{{ brand.fullName }}</p>
            <h1 id="home-title" :aria-label="brand.motto">
              <span class="title-line" aria-hidden="true"><b>把热爱，</b></span>
              <span class="title-line title-line--warm" aria-hidden="true"><b>骑成征途。</b></span>
            </h1>
            <p class="stage-description">从西安欧亚学院出发。<br>一辆单车，一群伙伴，一段共同的征途。</p>
            <div class="zt-actions cinema-actions"><slot /></div>
          </div>
          <figure id="club-cinema-frame" class="stage-art" aria-label="征途影像">
            <div class="stage-scene-frame">
              <div v-for="(scene,index) in scenes" :key="scene.image" class="stage-image"
                :class="{'stage-image--bike':index===0,'stage-image--current':sceneIndex===index,'is-projected':projectedScene===index}"
                :aria-hidden="projectedScene!==index">
                <div v-if="loadedScenes.has(index)" class="scene-film">
                  <img :class="{'bike-entry':index===0}" :src="scene.image"
                    :width="scene.width" :height="scene.height" :alt="scene.alt"
                    :fetchpriority="index===0?'high':'auto'" decoding="async" @load="imageReady(index)">
                  <svg v-if="index===0" class="bike-edges" viewBox="0 0 1200 630" aria-hidden="true" focusable="false">
                    <!-- Traces use the pixel coordinates of this club photograph. -->
                    <path pathLength="1" d="M269 412 L428 211 Q438 186 474 181 L775 137 M827 194 L925 414 M758 201 L542 445"/>
                    <circle pathLength="1" cx="273" cy="414" r="198"/>
                    <circle pathLength="1" cx="919" cy="415" r="198"/>
                  </svg>
                </div>
              </div>
            </div>
            <figcaption class="stage-caption" aria-live="polite">
              <span>{{ sceneNumber(projectedScene) }} / 0{{ scenes.length }}</span>
              {{ scenes[projectedScene]?.detail }} · {{ scenes[projectedScene]?.label }}
            </figcaption>
          </figure>
        </div>
        <div class="stage-dock">
          <div class="stage-choices" role="group" aria-label="切换首屏影像">
            <button v-for="(scene,index) in scenes" :key="scene.label" type="button" class="stage-choice"
              :aria-pressed="sceneIndex===index" aria-controls="club-cinema-frame"
              @click="selectScene(index)" @keydown="sceneKey($event,index)">
              <img :src="scene.thumb" width="54" height="36" alt="" decoding="async" fetchpriority="low">
              <span>{{ scene.detail }}</span>
            </button>
          </div>
          <button type="button" class="stage-explore" @click="scrollTo(historyTitle)">
            <span>向下探索<small>热爱不设终点</small></span><span class="stage-explore-icon"><Icon name="down" :size="17"/></span>
          </button>
        </div>
        <div class="stage-progress" aria-hidden="true"><i></i></div>
      </section>
    </div>

    <section class="cinema-history" aria-labelledby="history-title">
      <div class="history-inner">
        <header class="history-header">
          <p class="stage-kicker"><span aria-hidden="true"></span>ZHENGTU / MILESTONES</p>
          <h2 id="history-title" ref="historyTitle" tabindex="-1">一路骑来<span>。</span></h2>
          <p>从校园出发，沿着时间回看征途的每一程。</p>
        </header>
        <div class="history-layout">
          <aside ref="historyIndex" class="history-index">
            <p class="history-index-label">征途历程 <span aria-hidden="true">/</span> 选择年份</p>
            <span class="history-current" aria-hidden="true">{{ activeYear }}</span>
            <nav class="history-buttons" aria-label="按年份浏览征途历程">
              <button v-for="group in yearGroups" :key="group.year" type="button"
                :aria-current="activeYear===group.year?'true':undefined" @click="jumpToYear(group.year)">{{ group.year }}</button>
            </nav>
          </aside>
          <ol class="history-list">
            <li v-for="group in yearGroups" :key="group.year" :data-history-year="group.year"
              class="history-year-card" :class="{'history-year-card--active':activeYear===group.year}" tabindex="-1">
              <h3 class="history-year-label">{{ group.year }}</h3>
              <ul class="history-year-facts">
                <li v-for="item in group.items" :key="`${item.date}-${item.title}-${item.summary}`" class="history-year-fact">
                  <h4>{{ item.title }}</h4><p>{{ item.summary }}</p>
                  <a v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer">阅读学校报道 <Icon name="arrowup" :size="14"/></a>
                </li>
              </ul>
            </li>
          </ol>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped src="./home-cinema.css"></style>
