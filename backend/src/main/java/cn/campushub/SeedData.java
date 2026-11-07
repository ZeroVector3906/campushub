package cn.campushub;
import cn.campushub.Domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements ApplicationRunner {
    final CampusService s; final PasswordEncoder encoder;
    @Value("${campushub.seed-demo}") boolean demo;
    @Value("${campushub.admin-username}") String adminUsername;
    @Value("${campushub.admin-password}") String adminPassword;
    public SeedData(CampusService s,PasswordEncoder encoder){this.s=s;this.encoder=encoder;}
    @Override @Transactional public void run(ApplicationArguments args){
        if(!s.users().existsByUsername(adminUsername)){
            String password=adminPassword.isBlank()&&demo?"CampusHub@2026":adminPassword;
            if(password.length()<12)throw new IllegalStateException("首次启动请设置至少 12 位 ADMIN_PASSWORD，或仅在本地设置 SEED_DEMO=true");
            AuthController.checkPassword(password);
            var admin=user(adminUsername,"征途俱乐部管理员","admin@campushub.local","ADMIN001",encoder.encode(password));admin.role="SUPER_ADMIN";s.users().save(admin);
        }
        if(demo&&!s.users().existsByUsername("student"))s.users().save(user("student","演示骑友","student@example.test","DEMO001",encoder.encode("CampusHub@2026")));
    }
    User user(String username,String name,String email,String studentNo,String hash){var u=new User();u.username=username;u.name=name;u.email=email;u.studentNo=studentNo;u.department="西安欧亚学院";u.passwordHash=hash;return u;}
}
