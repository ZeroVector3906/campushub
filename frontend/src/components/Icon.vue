<script setup lang="ts">
import { computed } from 'vue'
import { Bike,GraduationCap,LayoutDashboard,Compass,CalendarDays,Package, Wrench,UserRound,ShieldCheck,Search,Bell,ArrowUpRight,ArrowRight,MapPin,Clock,Users,Plus,Check,ChevronRight,ChevronDown,LogOut,Menu,X,Camera,Mountain,Music2,Code2,HeartHandshake,BookOpen,Sparkles,Activity,Download,Settings,Mail,CheckCircle2,AlertCircle,Leaf,TrendingUp,ClipboardList,ArrowLeft,Ticket,Send,MonitorPlay,Mic2,Tent,Target } from 'lucide-vue-next'
const props=withDefaults(defineProps<{name:string;size?:number}>(),{size:20})
const icons:Record<string,any>={bike:Bike,graduation:GraduationCap,dashboard:LayoutDashboard,compass:Compass,calendar:CalendarDays,package:Package,wrench:Wrench,user:UserRound,shield:ShieldCheck,search:Search,bell:Bell,arrowup:ArrowUpRight,arrow:ArrowRight,pin:MapPin,clock:Clock,users:Users,plus:Plus,check:Check,right:ChevronRight,down:ChevronDown,logout:LogOut,menu:Menu,x:X,camera:Camera,mountain:Mountain,music:Music2,code:Code2,heart:HeartHandshake,book:BookOpen,sparkles:Sparkles,activity:Activity,download:Download,settings:Settings,mail:Mail,success:CheckCircle2,alert:AlertCircle,leaf:Leaf,trend:TrendingUp,clipboard:ClipboardList,back:ArrowLeft,ticket:Ticket,send:Send,monitor:MonitorPlay,mic:Mic2,tent:Tent,racket:Target}
const component=computed(()=>icons[props.name]||Compass)
</script>
<template><component :is="component" :size="size" :stroke-width="1.7" aria-hidden="true" /></template>
