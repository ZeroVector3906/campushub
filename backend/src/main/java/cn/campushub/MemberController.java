package cn.campushub;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/users")
public class MemberController {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;private final MemberPolicy members; private final AccountAuthority authority; private final UserRepository users;
    public MemberController(MemberPolicy members,AccountAuthority authority,UserRepository users,org.springframework.jdbc.core.JdbcTemplate jdbc){this.jdbc=jdbc;this.members=members;this.authority=authority;this.users=users;}
    @PutMapping("/{id}/profile") @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> update(org.springframework.security.core.Authentication a,@PathVariable Long id,@Valid @RequestBody Requests.MemberInput x){jdbc.execute("SELECT pg_advisory_xact_lock(804211)");authority.requireEditable(a,users.findById(id).orElseThrow(ApiException::missing));return members.view(members.save(id,x));}
}
