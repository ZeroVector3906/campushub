<template>
  <div class="atmosphere" aria-hidden="true">
    <div class="sky sky-road"></div>
    <div class="sky sky-team"></div>
    <div class="sky sky-anniversary"></div>
    <div class="sky-drift"></div>
    <div class="embers"></div>
    <svg class="light-road" viewBox="0 0 1500 840" preserveAspectRatio="none" focusable="false">
      <defs>
        <linearGradient id="zt-rail" x1="0" y1="1" x2="1" y2="0" gradientUnits="objectBoundingBox">
          <stop offset="0" stop-color="#1c968b"/><stop offset=".38" stop-color="var(--eurasia-green)"/>
          <stop offset=".58" stop-color="#d7ffe4"/><stop offset=".76" stop-color="var(--eurasia-green)"/>
          <stop offset="1" stop-color="#7cdccb"/>
        </linearGradient>
        <path id="zt-road" pathLength="1" d="M-174 805 C192 651 810 742 1190 532 C1460 384 1206 327 1004 344 C850 357 786 311 894 239 C1038 145 1378 119 1620 38"/>
      </defs>
      <g class="road-bed" stroke="url(#zt-rail)">
        <use href="#zt-road" class="rail-aura"/>
        <use href="#zt-road" class="rail-line"/>
        <use href="#zt-road" class="rail-shoulder" transform="translate(0 9)"/>
        <use href="#zt-road" class="rail-shoulder rail-far" transform="translate(0 23)"/>
      </g>
      <use href="#zt-road" class="rail-arrival"/>
      <use href="#zt-road" class="rail-runner"/>
      <use href="#zt-road" class="rail-runner rail-echo" transform="translate(0 9)"/>
    </svg>
  </div>
</template>

<style scoped>
.atmosphere, .sky, .sky-drift, .embers, .light-road {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.atmosphere { overflow: hidden; z-index: -1; background: #071614; }
.sky {
  transition: opacity .7s ease;
  transform: translate3d(0, calc(var(--scene-progress) * -2.2%), 0) scale(1.06);
}
.sky-road {
  background:
    radial-gradient(ellipse at 86% 48%, #15675e 0%, transparent 40%),
    radial-gradient(ellipse at 61% 24%, #17464c 0%, transparent 45%),
    radial-gradient(ellipse at 23% 98%, #173f32 0%, transparent 36%),
    linear-gradient(119deg, #061311 13%, #092425 51%, #0d3034 76%, #091d24);
}
.sky-team {
  opacity: var(--team-light, 0);
  background: radial-gradient(ellipse at 73% 37%, #456f4f, transparent 53%),
    radial-gradient(ellipse at 24% 86%, #244c39, transparent 47%), linear-gradient(120deg, #0a1b16, #16392f 67%, #293f31);
}
.sky-anniversary {
  opacity: var(--anniversary-light, 0);
  background: radial-gradient(ellipse at 77% 39%, #245b72, transparent 53%),
    radial-gradient(ellipse at 27% 89%, #4a4133, transparent 43%), linear-gradient(115deg, #092023, #153b43 67%, #112a35);
}
.sky-drift {
  inset: -9%;
  opacity: .68;
  background: radial-gradient(ellipse at 77% 79%, #53bda84a, transparent 25%),
    radial-gradient(ellipse at 44% 44%, #39a8903c, transparent 31%),
    radial-gradient(ellipse at 76% 21%, #639bbf46, transparent 24%);
  animation: drift 12s ease-in-out infinite alternate;
}
.embers {
  inset: -6%;
  opacity: .56;
  background-image:
    radial-gradient(circle at 17% 71%, #d9f6dc 0 1px, transparent 1.5px),
    radial-gradient(circle at 79% 22%, #badfda 0 .8px, transparent 1.3px),
    radial-gradient(circle at 36% 13%, #6accb2 0 .65px, transparent 1.4px);
  background-size: 207px 131px, 139px 173px, 97px 83px;
  mask-image: linear-gradient(123deg, transparent 5%, #0006 26%, #000 65%, #0007);
  animation: floating-dust 9s ease-in-out infinite alternate;
}
.light-road {
  width: 100%; height: 100%; overflow: visible;
  transform: translate3d(calc(var(--scene-x) * -.45), calc(var(--scene-progress) * 4%), 0) scale(1.04);
  fill: none; stroke-linecap: round;
}
.light-road use { vector-effect: non-scaling-stroke; }
.rail-aura { stroke-width: 13px; filter: blur(8px); opacity: .58; }
.rail-line { stroke-width: 2px; opacity: .85; }
.rail-shoulder { stroke-width: 1px; opacity: .55; }
.rail-far { opacity: .27; }
.rail-arrival { stroke: #e4ffe8; stroke-width: 2.8px; stroke-dasharray: 1; animation: enter-rail .9s cubic-bezier(.18,.67,.22,1) both; }
.rail-runner { stroke: var(--landing-mint); stroke-width: 2.8px; stroke-dasharray: .065 .935; animation: trace 4.4s linear infinite; }
.rail-echo { stroke: var(--eurasia-green); stroke-width: 1.3px; animation-delay: -2s; }
.sky-drift, .embers, .rail-runner { animation-play-state: var(--stage-play, running); }
@keyframes enter-rail { 0% { stroke-dashoffset: 1; opacity: 1; } 75% { opacity: 1; } 100% { stroke-dashoffset: 0; opacity: 0; } }
@keyframes trace { to { stroke-dashoffset: -1; } }
@keyframes drift { to { transform: translate3d(-3%, 3%, 0) scale(1.06); } }
@keyframes floating-dust { to { transform: translate3d(1.1%, -2%, 0); opacity: .74; } }
@media (max-width: 740px) {
  .embers { background-size: 293px 171px, 251px 191px, 197px 155px; }
  .light-road { width: 137%; left: -25%; }
  .rail-aura { stroke-width: 7px; filter: blur(4px); }
  .rail-far, .rail-echo { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .atmosphere *, .light-road use { animation: none; transition: none; transform: none; }
  .rail-runner, .rail-arrival { display: none; }
}
</style>
