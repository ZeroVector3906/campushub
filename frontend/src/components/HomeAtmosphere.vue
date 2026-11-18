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
          <stop offset="0" stop-color="#ff433e"/><stop offset=".38" stop-color="#ff783f"/>
          <stop offset=".58" stop-color="#ffd69c"/><stop offset=".76" stop-color="#ff613d"/>
          <stop offset="1" stop-color="#ff528e"/>
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
.atmosphere { overflow: hidden; z-index: -1; background: #090912; }
.sky {
  transition: opacity .7s ease;
  transform: translate3d(0, calc(var(--scene-progress) * -2.2%), 0) scale(1.06);
}
.sky-road {
  background:
    radial-gradient(ellipse at 90% 49%, #304290 0%, transparent 35%),
    radial-gradient(ellipse at 62% 30%, #432575 0%, transparent 43%),
    radial-gradient(ellipse at 27% 94%, #4a203f 0%, transparent 36%),
    linear-gradient(119deg, #090a13 13%, #14142d 51%, #111b3f 76%, #0a1024);
}
.sky-team {
  opacity: var(--team-light, 0);
  background: radial-gradient(ellipse at 72% 43%, #28597c, transparent 53%),
    radial-gradient(ellipse at 22% 83%, #413448, transparent 49%), linear-gradient(120deg, #0b1424, #142936 67%, #263450);
}
.sky-anniversary {
  opacity: var(--anniversary-light, 0);
  background: radial-gradient(ellipse at 78% 40%, #834454, transparent 52%),
    radial-gradient(ellipse at 42% 85%, #472353, transparent 46%), linear-gradient(115deg, #190e22, #39293c 67%, #37223b);
}
.sky-drift {
  inset: -9%;
  opacity: .68;
  background: radial-gradient(ellipse at 77% 79%, #e74b464d, transparent 25%),
    radial-gradient(ellipse at 44% 44%, #7a4bd749, transparent 31%),
    radial-gradient(ellipse at 76% 21%, #3a70e751, transparent 24%);
  animation: drift 12s ease-in-out infinite alternate;
}
.embers {
  inset: -6%;
  opacity: .56;
  background-image:
    radial-gradient(circle at 17% 71%, #ffdabe 0 1px, transparent 1.5px),
    radial-gradient(circle at 79% 22%, #b4baf6 0 .8px, transparent 1.3px),
    radial-gradient(circle at 36% 13%, #fba37d 0 .65px, transparent 1.4px);
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
.rail-arrival { stroke: #ffe0b8; stroke-width: 2.8px; stroke-dasharray: 1; animation: enter-rail .9s cubic-bezier(.18,.67,.22,1) both; }
.rail-runner { stroke: #ffd1a1; stroke-width: 2.8px; stroke-dasharray: .065 .935; animation: trace 4.4s linear infinite; }
.rail-echo { stroke: #ff995b; stroke-width: 1.3px; animation-delay: -2s; }
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
