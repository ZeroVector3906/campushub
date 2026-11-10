<script setup lang="ts">
import { computed, ref } from 'vue'
import { clubHistoryItems, type ClubHistoryCategory } from '../clubHistory'

type HistoryFilter = '全部' | ClubHistoryCategory

const selectedFilter = ref<HistoryFilter>('全部')
const filters: HistoryFilter[] = ['全部', '发展历程', '赛事荣誉']
const yearGroups = computed(() => {
  const visible = selectedFilter.value === '全部'
    ? clubHistoryItems
    : clubHistoryItems.filter(item => item.category === selectedFilter.value)
  const grouped = new Map<string, typeof visible>()
  for (const item of visible) {
    const yearItems = grouped.get(item.year) ?? []
    yearItems.push(item)
    grouped.set(item.year, yearItems)
  }
  return [...grouped.entries()].map(([year, items]) => ({ year, items }))
})
</script>

<template>
  <section id="club-history" class="club-history-space" aria-labelledby="club-history-title">
    <header class="archive-heading">
      <p class="archive-kicker">ZHENGTU ARCHIVE · 2013—2026</p>
      <h2 id="club-history-title">把走过的路，整理成一部史。</h2>
      <p class="archive-intro">从校园相聚、长途骑行到赛场与高校交流，按时间翻阅征途的成长记录。</p>
      <div class="archive-meta"><span>{{ clubHistoryItems.length }} 个档案节点</span><i aria-hidden="true"/><span>4 份来源材料</span></div>
    </header>

    <nav class="archive-filters" aria-label="征途历史分类">
      <button
        v-for="filter in filters"
        :key="filter"
        type="button"
        :class="{ active: selectedFilter === filter }"
        :aria-pressed="selectedFilter === filter"
        @click="selectedFilter = filter"
      >{{ filter }}</button>
    </nav>

    <div class="archive-timeline" aria-live="polite">
      <section v-for="group in yearGroups" :key="group.year" class="archive-year">
        <div class="archive-year-label"><span>{{ group.year }}</span><i aria-hidden="true"/></div>
        <ol class="archive-cards">
          <li v-for="item in group.items" :key="`${item.year}-${item.title}`" class="archive-card">
            <span class="archive-category">{{ item.category }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary }}</p>
          </li>
        </ol>
      </section>
    </div>

    <footer class="archive-source">资料整理自《俱乐部简介》《征途发展史》《奖项》及《征途》文化材料。</footer>
  </section>
</template>

<style scoped>
.club-history-space {
  position: relative;
  overflow: hidden;
  margin: 34px 0;
  padding: clamp(25px, 5vw, 58px);
  border: 1px solid #29483a;
  border-radius: 26px;
  color: #f2f4ec;
  background: radial-gradient(ellipse at 88% 0%, #345344 0, transparent 38%), linear-gradient(145deg, #13251d, #0d1915 72%);
}
.archive-heading { max-width: 760px; }
.archive-kicker { margin: 0 0 13px; color: #a6c4a2; font: 600 11px/1.5 ui-monospace, monospace; letter-spacing: .16em; }
.archive-heading h2 { margin: 0; font-size: clamp(25px, 4vw, 42px); font-weight: 560; letter-spacing: -.045em; line-height: 1.25; }
.archive-intro { max-width: 590px; margin: 15px 0 0; color: #bccbbd; font-size: 13px; line-height: 1.9; }
.archive-meta { display: flex; align-items: center; gap: 12px; margin-top: 21px; color: #95aa98; font-size: 11px; }
.archive-meta i { width: 3px; height: 3px; border-radius: 50%; background: #89b28e; }
.archive-filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 31px 0 17px; }
.archive-filters button { border: 1px solid #ffffff21; border-radius: 999px; padding: 9px 15px; color: #c6d4c8; background: #ffffff08; font: inherit; font-size: 12px; cursor: pointer; transition: color .2s ease, background .2s ease, border-color .2s ease; }
.archive-filters button:hover, .archive-filters button.active { border-color: #a5c5a0; color: #122219; background: #c4dabd; }
.archive-timeline { position: relative; }
.archive-year { display: grid; grid-template-columns: 106px minmax(0, 1fr); gap: 20px; padding: 17px 0; }
.archive-year + .archive-year { border-top: 1px solid #ffffff12; }
.archive-year-label { display: flex; align-items: flex-start; gap: 12px; padding-top: 18px; color: #c8dbc2; font-size: 18px; font-weight: 650; }
.archive-year-label i { flex: 0 0 7px; width: 7px; height: 7px; margin-top: 7px; border: 1px solid #add3a7; border-radius: 50%; background: #193026; box-shadow: 0 0 0 4px #add3a71c; }
.archive-cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 11px; margin: 0; padding: 0; list-style: none; }
.archive-card { min-width: 0; padding: 18px 19px; border: 1px solid #ffffff16; border-radius: 16px; background: #ffffff08; transition: transform .2s ease, border-color .2s ease, background .2s ease; }
.archive-card:hover { transform: translateY(-2px); border-color: #a5c5a04d; background: #ffffff0c; }
.archive-category { color: #a9c6a5; font-size: 10px; letter-spacing: .04em; }
.archive-card h3 { margin: 8px 0 6px; color: #f1f3ec; font-size: 15px; font-weight: 580; line-height: 1.5; }
.archive-card p { margin: 0; color: #b2c1b3; font-size: 12px; line-height: 1.85; }
.archive-source { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ffffff12; color: #829789; font-size: 10px; line-height: 1.7; }
@media (max-width: 680px) {
  .club-history-space { margin: 26px 0; padding: 25px 19px; border-radius: 21px; }
  .archive-intro { font-size: 12px; }
  .archive-year { grid-template-columns: 1fr; gap: 10px; padding: 14px 0; }
  .archive-year-label { align-items: center; padding: 0; font-size: 16px; }
  .archive-year-label i { margin-top: 0; }
  .archive-cards { grid-template-columns: 1fr; }
  .archive-card { padding: 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .archive-filters button, .archive-card { transition: none; }
}
</style>
