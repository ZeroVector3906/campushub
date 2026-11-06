export type Row = Record<string, any>
export interface User { id:number; username:string; name:string; email:string; studentNo:string; department:string; bio:string; role:'STUDENT'|'ADMIN'; enabled:boolean; createdAt:string }
export interface Club { id:number; name:string; category:string; summary:string; description:string; icon:string; color:string; location:string; contact:string; recruiting:boolean; active:boolean; memberCount:number }
export interface Activity { id:number; clubId:number; clubName:string; title:string; category:string; description:string; location:string; startsAt:string; endsAt:string; registrationDeadline:string; capacity:number; registrationCount:number; featured:boolean; status:string; canRegister:boolean; checkinCode?:string }
export interface Equipment { id:number; name:string; category:string; assetCode:string; description:string; location:string; totalQuantity:number; availableQuantity:number; active:boolean }
