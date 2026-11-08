// @vitest-environment happy-dom
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest'
import type {DirectiveBinding,VNode} from 'vue'
import {vReveal} from './motion'

let callback:IntersectionObserverCallback
let media:{matches:boolean;addEventListener:ReturnType<typeof vi.fn>;removeEventListener:ReturnType<typeof vi.fn>}
let instances:FakeObserver[]
const elements:HTMLElement[]=[]
class FakeObserver {
  observe=vi.fn();unobserve=vi.fn();disconnect=vi.fn()
  constructor(fn:IntersectionObserverCallback){callback=fn;instances.push(this)}
}
function mount(top=2000,delay=0){
  const el=document.createElement('section');document.body.append(el);elements.push(el)
  vi.spyOn(el,'getBoundingClientRect').mockReturnValue({top} as DOMRect)
  vReveal.mounted?.(el,{value:{delay}} as DirectiveBinding,{} as VNode<HTMLElement,HTMLElement>,null)
  return el
}
beforeEach(()=>{
  instances=[]
  media={matches:false,addEventListener:vi.fn(),removeEventListener:vi.fn()}
  vi.stubGlobal('matchMedia',vi.fn(()=>media))
  vi.stubGlobal('IntersectionObserver',FakeObserver)
})
afterEach(()=>{
  for(const el of elements.splice(0)){vReveal.unmounted?.(el,{} as DirectiveBinding,{} as VNode<HTMLElement,HTMLElement>,null);el.remove()}
  vi.unstubAllGlobals();vi.restoreAllMocks()
})
describe('scroll reveal lifecycle',()=>{
  it('keeps content visible when reduced motion is requested',()=>{
    media.matches=true
    expect(mount().dataset.reveal).toBeUndefined();expect(instances).toHaveLength(0)
  })
  it('keeps content visible if the browser has no observer',()=>{
    vi.stubGlobal('IntersectionObserver',undefined)
    expect(mount().dataset.reveal).toBeUndefined()
  })
  it('does not hide the initial viewport',()=>{
    expect(mount(0).dataset.reveal).toBeUndefined();expect(instances).toHaveLength(0)
  })
  it('reveals once on entry, bounds stagger, and releases the observer',()=>{
    const el=mount(2000,900),io=instances[0]!
    expect(el.dataset.reveal).toBe('pending');expect(el.style.getPropertyValue('--reveal-delay')).toBe('180ms')
    callback([{target:el,isIntersecting:true} as unknown as IntersectionObserverEntry],io as unknown as IntersectionObserver)
    expect(el.dataset.reveal).toBe('visible');expect(io.unobserve).toHaveBeenCalledWith(el);expect(io.disconnect).toHaveBeenCalledOnce()
  })
  it('reveals focused content immediately and detaches listeners on unmount',()=>{
    const el=mount();el.dispatchEvent(new Event('focusin'))
    expect(el.dataset.reveal).toBe('visible');expect(el.hasAttribute('data-reveal-instant')).toBe(true)
    const removed=mount(),io=instances.at(-1)!
    vReveal.unmounted?.(removed,{} as DirectiveBinding,{} as VNode<HTMLElement,HTMLElement>,null)
    removed.dispatchEvent(new Event('focusin'))
    expect(removed.dataset.reveal).toBe('pending');expect(io.unobserve).toHaveBeenCalledWith(removed)
  })
  it('shows all waiting content if the system switches to reduced motion',()=>{
    const first=mount(),second=mount(),change=media.addEventListener.mock.calls[0]![1] as (event:MediaQueryListEvent)=>void
    change({matches:true} as MediaQueryListEvent)
    expect([first,second].every(el=>el.dataset.reveal==='visible'&&el.hasAttribute('data-reveal-instant'))).toBe(true)
    expect(media.removeEventListener).toHaveBeenCalled()
  })
})
