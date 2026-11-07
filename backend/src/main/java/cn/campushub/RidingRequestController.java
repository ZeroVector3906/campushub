package cn.campushub;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="riding_requests")
class RidingRequest extends Domain.Base {
    public Long userId;
    public String kind, title, description, contact, room, usage, otherUsage;
    public Instant startsAt, endsAt;
    public String status="PENDING", reviewNote="";
    public Instant reviewedAt;
}
interface RidingRequestRepository extends JpaRepository<RidingRequest,Long> {
    List<RidingRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RidingRequest> findAllByOrderByCreatedAtDesc();
}

@RestController @RequestMapping("/api")
public class RidingRequestController {
    private final MemberPolicy members;
    private final RidingRequestRepository requests;
    private final AuthController auth;
    private final UserRepository users;
    public RidingRequestController(RidingRequestRepository requests,AuthController auth,UserRepository users,MemberPolicy members){this.members=members;this.requests=requests;this.auth=auth;this.users=users;}
    public record Input(@NotBlank String kind,@NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=2000) String description,@NotBlank @Size(max=120) String contact,
        @Size(max=40) String room,@Size(max=20) String usage,@Size(max=200) String otherUsage,
        Instant startsAt,Instant endsAt) {}
    private Map<String,Object> view(RidingRequest r){return ApiViews.v("id",r.id,"user",users.findById(r.userId).map(ApiViews::user).orElse(null),"kind",r.kind,"title",r.title,"description",r.description,"contact",r.contact,"room",r.room,"usage",MemberPolicy.normalizeUsage(r.usage),"otherUsage",r.otherUsage,"startsAt",r.startsAt,"endsAt",r.endsAt,"status",r.status,"reviewNote",r.reviewNote,"reviewedAt",r.reviewedAt,"createdAt",r.createdAt);}
    @GetMapping("/riding-requests") public List<?> mine(Authentication a){return requests.findByUserIdOrderByCreatedAtDesc(auth.current(a).id).stream().map(this::view).toList();}
    @GetMapping("/admin/riding-requests") public List<?> all(){return requests.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();}
    @PostMapping("/riding-requests") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Map<String,Object> create(Authentication a,@Valid @RequestBody Input x){
        if(!Set.of("ROOM","SUGGESTION").contains(x.kind()))throw ApiException.bad("申请类型无效");
        var r=new RidingRequest();r.userId=auth.current(a).id;r.kind=x.kind();r.title=x.title().trim();r.description=x.description().trim();r.contact=x.contact().trim();
        r.room="";r.usage="";r.otherUsage="";
        if(x.kind().equals("ROOM")){
            if(!Set.of(MemberPolicy.HEGUANG,"和光文体中心俱乐部",MemberPolicy.NORTH).contains(Objects.toString(x.room(),"")))throw ApiException.bad("请选择有效骑行室");
            if(!Set.of("训练","存放车辆","拿车","维修","休息","其他").contains(Objects.toString(MemberPolicy.normalizeUsage(x.usage()),"")))throw ApiException.bad("请选择使用方式");
            if(x.usage().equals("其他")&&(x.otherUsage()==null||x.otherUsage().isBlank()))throw ApiException.bad("请填写其他使用方式");
            if(x.startsAt()==null||x.endsAt()==null||!x.startsAt().isAfter(Instant.now())||!x.endsAt().isAfter(x.startsAt()))throw ApiException.bad("开始时间须晚于当前时间，结束时间须晚于开始时间");
            r.room=x.room().equals("和光文体中心俱乐部")?MemberPolicy.HEGUANG:x.room();r.usage=MemberPolicy.normalizeUsage(x.usage());members.requireRoom(auth.current(a),r.room,r.usage);r.otherUsage=x.usage().equals("其他")?x.otherUsage().trim():"";r.startsAt=x.startsAt();r.endsAt=x.endsAt();
        }
        return view(requests.save(r));
    }
    @PutMapping("/admin/riding-requests/{id}/review") @Transactional
    public Map<String,Object> review(@PathVariable Long id,@Valid @RequestBody Requests.Review x){
        if(!Set.of("APPROVED","REJECTED").contains(x.status()))throw ApiException.bad("审核状态无效");
        var r=requests.findById(id).orElseThrow(ApiException::missing);
        if(x.status().equals("APPROVED")&&r.kind.equals("ROOM"))members.requireRoom(users.findById(r.userId).orElseThrow(ApiException::missing),r.room.equals("和光文体中心俱乐部")?MemberPolicy.HEGUANG:r.room,r.usage);
        r.status=x.status();r.reviewNote=Objects.toString(x.note(),"");r.reviewedAt=Instant.now();return view(requests.save(r));
    }
}
