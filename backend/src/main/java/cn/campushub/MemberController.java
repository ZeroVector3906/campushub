package cn.campushub;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/users")
public class MemberController {
    private final MemberPolicy members;
    public MemberController(MemberPolicy members){this.members=members;}
    @PutMapping("/{id}/profile")
    public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody Requests.MemberInput x){return members.view(members.save(id,x));}
}
