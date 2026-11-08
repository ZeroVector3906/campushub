import type {ObjectDirective} from 'vue'

type RevealOptions = {delay?:number;kind?:'image'}
const pending = new Set<HTMLElement>()
const focusHandlers = new WeakMap<HTMLElement,()=>void>()
let observer:IntersectionObserver|undefined
let preference:MediaQueryList|undefined

function releaseIfIdle(){
  if(pending.size)return
  observer?.disconnect()
  preference?.removeEventListener('change',onPreferenceChange)
  observer=undefined
  preference=undefined
}
function releaseElement(el:HTMLElement){
  observer?.unobserve(el)
  pending.delete(el)
  const focus=focusHandlers.get(el)
  if(focus)el.removeEventListener('focusin',focus)
  focusHandlers.delete(el)
  releaseIfIdle()
}
function reveal(el:HTMLElement,instant=false){
  if(instant)el.dataset.revealInstant=''
  el.dataset.reveal='visible'
  releaseElement(el)
}
function onPreferenceChange(event:MediaQueryListEvent){
  if(event.matches)for(const el of [...pending])reveal(el,true)
}
function observe(el:HTMLElement){
  if(!observer){
    preference=window.matchMedia('(prefers-reduced-motion: reduce)')
    preference.addEventListener('change',onPreferenceChange)
    observer=new IntersectionObserver(entries=>{
      for(const entry of entries)if(entry.isIntersecting)reveal(entry.target as HTMLElement)
    },{threshold:0,rootMargin:'0px 0px -24px 0px'})
  }
  pending.add(el)
  const focus=()=>reveal(el,true)
  focusHandlers.set(el,focus)
  el.addEventListener('focusin',focus)
  observer.observe(el)
}

/** Only below-fold presentation blocks are observed; forms stay immediately usable. */
export const vReveal:ObjectDirective<HTMLElement,RevealOptions|undefined>={
  mounted(el,{value}){
    if(typeof window==='undefined'||typeof window.IntersectionObserver!=='function'||
      !window.matchMedia||window.matchMedia('(prefers-reduced-motion: reduce)').matches)return
    if(el.getBoundingClientRect().top<window.innerHeight*.9)return
    el.style.setProperty('--reveal-delay',Math.min(180,Math.max(0,value?.delay||0))+'ms')
    if(value?.kind)el.dataset.revealKind=value.kind
    el.dataset.reveal='pending'
    observe(el)
  },
  unmounted(el){releaseElement(el)}
}
