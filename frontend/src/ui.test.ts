// @vitest-environment happy-dom
import {beforeEach,afterEach,describe,it,expect,vi} from 'vitest'
import {mount,flushPromises,type VueWrapper} from '@vue/test-utils'
import {createRouter,createMemoryHistory} from 'vue-router'
import ElementPlus from 'element-plus'
import {nextTick} from 'vue'
import Clubs from './pages/Clubs.vue'
import Home from './pages/Home.vue'
import Activities from './pages/Activities.vue'
import Equipment from './pages/Equipment.vue'
import Login from './pages/Login.vue'
import EntityEditor from './components/EntityEditor.vue'
import {state} from './store'
import {api} from './api'
vi.mock('./api',()=>({api:vi.fn(async()=>({})),run:async(fn:()=>Promise<unknown>)=>{await fn();return true},resetCsrf:vi.fn(),downloadReport:vi.fn()}))
vi.mock('./store',async(importOriginal)=>{const actual=await importOriginal<typeof import('./store')>();return{...actual,refreshPublic:vi.fn(),refreshMine:vi.fn(),login:vi.fn()}})
let wrapper:VueWrapper|undefined
const clubs=[{id:1,name:'光影摄影社',category:'文化艺术',summary:'用镜头记录校园',description:'欢迎零基础同学',icon:'camera',color:'#547D6A',location:'艺术楼',contact:'photo@campus.edu',recruiting:true,active:true,memberCount:8},{id:2,name:'山野户外社',category:'体育运动',summary:'一起走进自然',description:'安全同行',icon:'mountain',color:'#8C8960',location:'体育馆',contact:'outdoor@campus.edu',recruiting:true,active:true,memberCount:5}]
beforeEach(()=>{vi.clearAllMocks();Object.assign(state,{user:{id:1,username:'student',name:'林同学',email:'student@campus.edu',studentNo:'2026001',department:'计算机学院',bio:'',role:'STUDENT',enabled:true,createdAt:new Date().toISOString()},clubs:structuredClone(clubs),activities:[{id:1,clubId:1,clubName:'光影摄影社',title:'校园摄影漫步',category:'文化艺术',description:'一起寻找校园里的光影',location:'校园广场',startsAt:new Date(Date.now()+600000).toISOString(),endsAt:new Date(Date.now()+7200000).toISOString(),registrationDeadline:new Date(Date.now()+600000).toISOString(),capacity:20,registrationCount:4,featured:true,status:'PUBLISHED',canRegister:true}],equipment:[{id:1,name:'测试相机',category:'摄影器材',assetCode:'CH001',description:'相机与镜头',location:'艺术楼',totalQuantity:3,availableQuantity:2,active:true}],overview:{demoEnabled:true},mine:{memberships:[],registrations:[],loans:[],repairs:[]},ready:true})})
afterEach(()=>{wrapper?.unmount();wrapper=undefined;document.body.innerHTML=''})
async function show(component:any,props:Record<string,unknown>={}){const router=createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{template:'<div />'}}]});await router.push('/');await router.isReady();wrapper=mount(component,{props,attachTo:document.body,global:{plugins:[router,ElementPlus],stubs:{teleport:true}}});await flushPromises();return wrapper}
function button(text:string){const item=wrapper!.findAll('button').find(b=>b.text()===text);expect(item,`按钮 ${text} 应存在`).toBeTruthy();return item!}
describe('真实 Vue 组件交互（模拟 DOM，不是真实浏览器）',()=>{
 it('征途专属首页显示学校与俱乐部身份，入口连接真实业务路由',async()=>{await show(Home);expect(wrapper!.text()).toContain('西安欧亚学院');expect(wrapper!.text()).toContain('征途单车俱乐部');expect(wrapper!.find('.zt-hero img').attributes('src')).toBe('/images/cycling.svg');expect(wrapper!.find('.zt-primary').attributes('href')).toBe('/clubs');expect(wrapper!.findAll('a').some(a=>a.attributes('href')==='/equipment')).toBe(true);expect(wrapper!.text()).not.toContain('热门社团')})
 it('没有已发布骑行活动时显示真实空状态与活动记录入口',async()=>{state.activities=[];await show(Home);expect(wrapper!.text()).toContain('下一次出发，正在准备中');expect(wrapper!.findAll('.activity-card')).toHaveLength(0);expect(wrapper!.findAll('a').some(a=>a.text().includes('查看活动记录')&&a.attributes('href')==='/activities')).toBe(true)})
 it('社团分类筛选和搜索只保留匹配项',async()=>{await show(Clubs);expect(wrapper!.findAll('.club-card')).toHaveLength(2);await button('体育运动').trigger('click');expect(wrapper!.findAll('.club-card')).toHaveLength(1);expect(wrapper!.find('.club-card').text()).toContain('山野户外社');await button('全部社团').trigger('click');await wrapper!.find('input[placeholder="搜索社团名称或关键词"]').setValue('摄影');expect(wrapper!.findAll('.club-card')).toHaveLength(1);expect(wrapper!.find('.club-card').text()).toContain('光影摄影社')})
 it('填写加入理由后提交正确的社团申请',async()=>{await show(Clubs);await wrapper!.find('.club-card').trigger('click');await flushPromises();await wrapper!.find('textarea').setValue('希望学习摄影并参与校园记录');await button('提交加入申请').trigger('click');await flushPromises();expect(api).toHaveBeenCalledWith('/clubs/1/applications','POST',{reason:'希望学习摄影并参与校园记录'})})
 it('活动详情能打开和关闭，清空选中项不会导致渲染错误',async()=>{await show(Activities);await wrapper!.find('.activity-card').trigger('click');await flushPromises();expect(wrapper!.find('.detail-description').text()).toContain('寻找校园里的光影');await button('关闭').trigger('click');await nextTick();expect(wrapper!.text()).toContain('骑行活动')})
 it('活动报名按钮调用真实接口契约',async()=>{await show(Activities);await wrapper!.find('.activity-card').trigger('click');await flushPromises();await button('立即报名').trigger('click');await flushPromises();expect(api).toHaveBeenCalledWith('/activities/1/registrations','POST')})
 it('器材借用弹窗要求用途和归还日期，未填写不能提交',async()=>{await show(Equipment);await button('申请借用').trigger('click');await flushPromises();expect(wrapper!.text()).toContain('借用申请 · 测试相机');expect(button('提交借用申请').attributes('disabled')).toBeDefined()})
 it('演示入口只填充账号，由用户主动提交登录',async()=>{await show(Login);await button('学生账号').trigger('click');const inputs=wrapper!.findAll('input');expect((inputs[0]!.element as HTMLInputElement).value).toBe('student');expect((inputs[1]!.element as HTMLInputElement).value).toBe('CampusHub@2026')})
 it('管理员器材编辑器提交实际字段而不允许客户端直接改可用库存',async()=>{await show(EntityEditor,{kind:'equipment',item:{id:9,name:'测试投影仪',category:'活动设备',assetCode:'CH009',description:'含电源与连接线',location:'信息楼',totalQuantity:5,availableQuantity:2,active:true},clubs});await button('保存器材').trigger('click');await flushPromises();expect(api).toHaveBeenCalledWith('/admin/equipment/9','PUT',expect.objectContaining({totalQuantity:5,assetCode:'CH009'}));const body=vi.mocked(api).mock.calls.at(-1)?.[2] as Record<string,unknown>;expect(body).not.toHaveProperty('availableQuantity');expect(wrapper!.emitted('saved')).toHaveLength(1)})
})
