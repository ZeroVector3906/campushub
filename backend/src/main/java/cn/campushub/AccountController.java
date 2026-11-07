package cn.campushub;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/admin/users")
public class AccountController {
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;private final AccountAuthority authority;private final UserRepository users;private final PasswordEncoder encoder;private final FindByIndexNameSessionRepository<? extends Session> sessions;
 public AccountController(AccountAuthority authority,UserRepository users,PasswordEncoder encoder,FindByIndexNameSessionRepository<? extends Session> sessions,org.springframework.jdbc.core.JdbcTemplate jdbc){this.jdbc=jdbc;this.authority=authority;this.users=users;this.encoder=encoder;this.sessions=sessions;}
 public record Input(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,32}") String username,@Size(max=500) String bio,@Size(max=64) String newPassword){}
 @PutMapping("/{id}/account") @Transactional public Map<String,Object> update(Authentication a,@PathVariable Long id,@Valid @RequestBody Input x){
  authority.requireSuper(a);jdbc.execute("SELECT pg_advisory_xact_lock(804211)");var u=users.findById(id).orElseThrow(ApiException::missing);String name=x.username().toLowerCase(Locale.ROOT),old=u.username;
  if(!name.equals(old)&&users.existsByUsername(name))throw ApiException.bad("用户名已被使用");
  boolean reset=x.newPassword()!=null&&!x.newPassword().isEmpty();if(reset){if(x.newPassword().length()<8)throw ApiException.bad("新密码至少8位");AuthController.checkPassword(x.newPassword());u.passwordHash=encoder.encode(x.newPassword());}
  u.username=name;u.bio=Objects.toString(x.bio(),"");users.save(u);
  if(reset||!old.equals(name))sessions.findByPrincipalName(old).keySet().forEach(sessions::deleteById);
  return ApiViews.user(u);
 }
}
