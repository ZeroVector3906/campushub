package cn.campushub;
import cn.campushub.Domain.User;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@Service
public class AccountAuthority {
 private final AuthController auth;
 public AccountAuthority(AuthController auth){this.auth=auth;}
 public User actor(Authentication a){return auth.current(a);}
 public void requireSuper(Authentication a){if(!actor(a).role.equals("SUPER_ADMIN"))throw new ApiException(HttpStatus.FORBIDDEN,"仅超级管理员可执行此操作");}
 public void requireEditable(Authentication a,User target){var u=actor(a);if(!u.role.equals("SUPER_ADMIN")&&!target.role.equals("STUDENT"))throw new ApiException(HttpStatus.FORBIDDEN,"普通管理员不能修改管理员或超级管理员资料");}
}
