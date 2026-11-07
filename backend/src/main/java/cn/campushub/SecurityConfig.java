package cn.campushub;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username).map(u -> User.withUsername(u.username)
            .password(u.passwordHash).roles(u.role).disabled(!u.enabled).build())
            .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService service, PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, UserRepository users,
            SecurityContextRepository contextRepository, CsrfTokenRepository csrfRepository) throws Exception {
        http.securityContext(c -> c.securityContextRepository(contextRepository))
            .csrf(c -> c.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(c -> c
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/api/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/public/**", "/api/clubs", "/api/activities", "/api/equipment").permitAll()
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
            .exceptionHandling(c -> c.authenticationEntryPoint((req,res,e) -> jsonError(res,401,"请先登录"))
                .accessDeniedHandler((req,res,e) -> jsonError(res,403,"权限不足或安全令牌已失效，请刷新页面")))
            .headers(c -> c.contentSecurityPolicy(p -> p.policyDirectives("default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'")))
            .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable());
        // Re-read access on each authenticated request: revocation takes effect immediately.
        http.addFilterAfter(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                    var user=users.findByUsername(auth.getName()).orElse(null);
                    if(user==null || !user.enabled) {
                        SecurityContextHolder.clearContext();
                        if(request.getSession(false)!=null) request.getSession(false).invalidate();
                    } else {
                        var principal=User.withUsername(user.username).password(user.passwordHash).roles(user.role).build();
                        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
                    }
                }
                chain.doFilter(request,response);
            }
        }, SecurityContextHolderFilter.class);
        return http.build();
    }
    private static void jsonError(HttpServletResponse response,int status,String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\""+message+"\"}");
    }
}
