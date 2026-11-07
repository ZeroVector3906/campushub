package cn.campushub;

import cn.campushub.Domain.User;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final CampusService service;
    private final MemberPolicy members;
    private final UserRepository users;
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrf;
    private final Map<String,Attempt> attempts=new ConcurrentHashMap<>();
    private record Attempt(int count, Instant since) {}
    public AuthController(UserRepository users, AuthenticationManager manager, PasswordEncoder encoder,
                          SecurityContextRepository contexts, CsrfTokenRepository csrf,CampusService service,MemberPolicy members) {
        this.service=service;this.members=members;
        this.users=users; this.manager=manager; this.encoder=encoder; this.contexts=contexts; this.csrf=csrf;
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Map<String,Object> register(@Valid @RequestBody Requests.Register input) {
        checkPassword(input.password());
        MemberPolicy.validateType(input.joinType());MemberPolicy.validateDivision(input.joinType(),input.clubDivision());
        if(users.existsByUsername(input.username().toLowerCase(Locale.ROOT)) || users.existsByEmail(input.email().toLowerCase(Locale.ROOT)))
            throw ApiException.bad("用户名或邮箱已被使用");
        var u=new User(); u.username=input.username().toLowerCase(Locale.ROOT); u.passwordHash=encoder.encode(input.password());
        u.name=input.name().trim(); u.email=input.email().toLowerCase(Locale.ROOT).trim(); u.studentNo=input.studentNo().trim(); u.department=input.department().trim();
        u.joinType=input.joinType();u.contact=input.contact().trim();u.grade=input.grade().trim();users.save(u);
        members.primaryClub().filter(c->c.recruiting).ifPresent(c->service.apply(u,c.id,new Requests.Application("注册时申请加入"+(u.joinType.equals("CLUB")?"俱乐部":"社团"),u.contact,u.grade,u.department,u.joinType,input.clubDivision())));
        return members.view(u);
    }
    @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody Requests.Login input, HttpServletRequest req, HttpServletResponse res) {
        String key=req.getRemoteAddr(); var now=Instant.now();
        attempts.entrySet().removeIf(e -> e.getValue().since().isBefore(now.minusSeconds(900)));
        var attempt=attempts.get(key);
        if(attempt!=null && attempt.count()>=10) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"尝试次数过多，请 15 分钟后再试");
        try {
            var auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username().toLowerCase(Locale.ROOT),input.password()));
            if(req.getSession(false)!=null) req.changeSessionId();
            var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
            SecurityContextHolder.setContext(context); contexts.saveContext(context,req,res);
            csrf.saveToken(null,req,res); attempts.remove(key);
            return ApiViews.user(users.findByUsername(auth.getName()).orElseThrow());
        } catch(AuthenticationException e) {
            attempts.compute(key,(k,v) -> v==null ? new Attempt(1,now) : new Attempt(v.count()+1,v.since()));
            throw new ApiException(HttpStatus.UNAUTHORIZED,"用户名或密码错误，或账号已停用");
        }
    }
    @GetMapping("/me") Map<String,Object> me(Authentication auth) { return ApiViews.user(current(auth)); }
    @PostMapping("/logout") Map<String,String> logout(HttpServletRequest req,HttpServletResponse res) {
        SecurityContextHolder.clearContext(); csrf.saveToken(null,req,res);
        if(req.getSession(false)!=null) req.getSession(false).invalidate();
        return Map.of("message","已安全退出");
    }
    User current(Authentication auth) {
        if(auth==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"请先登录");
        return users.findByUsername(auth.getName()).orElseThrow(ApiException::missing);
    }
    static void checkPassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw ApiException.bad("密码 UTF-8 长度不能超过 72 字节");
    }
}
