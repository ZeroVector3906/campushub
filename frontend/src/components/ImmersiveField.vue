<script setup lang="ts">
import {onMounted,onBeforeUnmount,ref} from 'vue'
const field=ref<HTMLDivElement>(),canvas=ref<HTMLCanvasElement>()
let dispose=()=>{}
onMounted(()=>{
 const host=field.value,el=canvas.value;if(!host||!el)return
 const ctx=el.getContext('2d');if(!ctx)return
 const preference=matchMedia('(prefers-reduced-motion: reduce)'),coarse=matchMedia('(pointer: coarse)')
 let width=0,height=0,frame=0,last=0,visible=true,previous=0
 let points:{x:number;y:number;speed:number;phase:number}[]=[]
 let pointer:{x:number;y:number}|null=null
 const parent=host.parentElement!
 function stop(){cancelAnimationFrame(frame);frame=0;previous=0}
 function start(){if(!frame&&visible&&!document.hidden&&!preference.matches){last=0;frame=requestAnimationFrame(draw)}}
 function resize(){const rect=host!.getBoundingClientRect();width=rect.width;height=rect.height;const ratio=Math.min(devicePixelRatio||1,1.5);el!.width=Math.round(width*ratio);el!.height=Math.round(height*ratio);ctx!.setTransform(ratio,0,0,ratio,0,0);points=Array.from({length:width<600||coarse.matches?22:54},()=>({x:Math.random()*width,y:Math.random()*height,speed:5+Math.random()*10,phase:Math.random()*Math.PI*2}));if(preference.matches)ctx!.clearRect(0,0,width,height)}
 function draw(time:number){frame=0;if(!visible||document.hidden||preference.matches)return
  if(time-last>=33){const dt=previous?Math.min((time-previous)/1000,.06):0;previous=time;last=time;ctx!.clearRect(0,0,width,height)
   if(pointer){host!.style.setProperty('--focus-x',pointer.x+'px');host!.style.setProperty('--focus-y',pointer.y+'px');pointer=null}
   for(const p of points){p.y-=p.speed*dt;p.x+=Math.sin(time*.0002+p.phase)*dt*3;if(p.y<0)p.y=height;if(p.x<0)p.x=width;if(p.x>width)p.x=0;ctx!.beginPath();ctx!.arc(p.x,p.y,1.2+Math.sin(p.phase)*.45,0,Math.PI*2);ctx!.fillStyle='rgba(0,113,227,.38)';ctx!.fill()}
   if(width>=600&&!coarse.matches)for(let i=0;i<points.length;i++){let linked=0;for(let j=i+1;j<points.length&&linked<2;j++){const a=points[i]!,b=points[j]!,d=Math.hypot(a.x-b.x,a.y-b.y);if(d<105){ctx!.beginPath();ctx!.moveTo(a.x,a.y);ctx!.lineTo(b.x,b.y);ctx!.strokeStyle='rgba(0,113,227,'+(.1*(1-d/105))+')';ctx!.lineWidth=.7;ctx!.stroke();linked++}}}
  }frame=requestAnimationFrame(draw)
 }
 function move(event:PointerEvent){if(coarse.matches||preference.matches)return;const r=host!.getBoundingClientRect();pointer={x:event.clientX-r.left,y:event.clientY-r.top}}
 function reset(){pointer={x:width/2,y:height/2}}
 function sync(){stop();ctx!.clearRect(0,0,width,height);start()}
 const sizeObserver=new ResizeObserver(resize);sizeObserver.observe(host)
 const observer=typeof IntersectionObserver==='function'?new IntersectionObserver(entries=>{visible=entries.some(e=>e.isIntersecting);if(visible)start();else stop()}):null
 observer?.observe(host);parent.addEventListener('pointermove',move,{passive:true});parent.addEventListener('pointerleave',reset,{passive:true});document.addEventListener('visibilitychange',sync);preference.addEventListener('change',sync);coarse.addEventListener('change',resize)
 resize();start()
 dispose=()=>{stop();observer?.disconnect();sizeObserver.disconnect();parent.removeEventListener('pointermove',move);parent.removeEventListener('pointerleave',reset);document.removeEventListener('visibilitychange',sync);preference.removeEventListener('change',sync);coarse.removeEventListener('change',resize)}
})
onBeforeUnmount(()=>dispose())
</script>
<template><div ref="field" class="immersive-field" aria-hidden="true"><canvas ref="canvas"/></div></template>
<style>
.zt-hero{isolation:isolate}.immersive-field{position:absolute;inset:0 0 auto;height:640px;overflow:hidden;pointer-events:none;z-index:0;--focus-x:50%;--focus-y:42%;mask-image:linear-gradient(#000 75%,transparent)}.immersive-field:before{content:"";position:absolute;inset:0;background:radial-gradient(ellipse 390px 280px at var(--focus-x) var(--focus-y),#0071e30c,transparent 80%),radial-gradient(ellipse at 75% 25%,#42c6ba0a,transparent 60%)}.immersive-field canvas{display:block;width:100%;height:100%}.zt-hero-copy,.zt-hero-visual{z-index:1}@media(hover:hover) and (pointer:fine){.zt-actions .zt-primary:hover{transform:translateY(-3px);box-shadow:0 8px 24px #0071e325}.zt-services>a:hover>svg{transform:translateY(-3px);transition:transform .25s}}@media(max-width:600px){.immersive-field{height:510px}.immersive-field:before{background:radial-gradient(ellipse at 50% 35%,#0071e309,transparent 70%)}}@media(prefers-reduced-motion:reduce){.immersive-field{display:none}.zt-actions .zt-primary:hover,.zt-services>a:hover>svg{transform:none!important}}
</style>
