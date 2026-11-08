// @vitest-environment happy-dom
import {beforeEach,it,expect,vi} from 'vitest'
import {state,initialize,refreshPublic} from './store'
import {api,ApiError} from './api'
vi.mock('./api',()=>({api:vi.fn(),resetCsrf:vi.fn(),wakeService:vi.fn(async()=>{}),ApiError:class extends Error{constructor(public status:number,message:string){super(message)}}}))
beforeEach(()=>{vi.clearAllMocks();Object.assign(state,{user:null,ready:false,error:'',connecting:false,publicLoaded:false,clubs:[],activities:[],equipment:[],overview:{}})})
it('one failed public endpoint does not discard the successful public results',async()=>{vi.mocked(api).mockImplementation(async path=>{if(path==='/activities')throw new Error('offline');return path==='/clubs'?[{id:7}]:[]});await expect(refreshPublic()).rejects.toThrow('offline');expect(state.clubs[0]?.id).toBe(7);expect(state.publicLoaded).toBe(false)})
it('startup failures settle and remain retryable, without clearing an existing user',async()=>{state.user={id:9,name:'成员'} as any;vi.mocked(api).mockRejectedValue(new Error('offline'));await initialize();expect(state.ready).toBe(true);expect(state.connecting).toBe(false);expect(state.error).toBe('offline');expect(state.user?.id).toBe(9);vi.mocked(api).mockImplementation(async path=>path==='/auth/me'?{id:9,name:'成员'}:path==='/me/dashboard'?{memberships:[],registrations:[],loans:[],repairs:[]}:[]);await initialize();expect(state.error).toBe('');expect(state.publicLoaded).toBe(true)})
it('only an explicit 401 clears the restored session and private cached records',async()=>{state.user={id:9} as any;state.mine.memberships=[{id:77}] as any;vi.mocked(api).mockImplementation(async path=>{if(path==='/auth/me')throw new ApiError(401,'请登录');return []});await initialize();expect(state.user).toBeNull();expect(state.mine.memberships).toHaveLength(0);expect(state.error).toBe('')})

