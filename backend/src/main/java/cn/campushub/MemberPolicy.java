package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** Member identity is separate from the administrator security role. */
@Service @Transactional
public class MemberPolicy {
    public static final String CLUB_NAME="征途单车俱乐部";
    public static final String HEGUANG="和光文体中心单车俱乐部", NORTH="北三骑行室";
    private final UserRepository users;
    private final ClubRepository clubs;
    private final MembershipRepository memberships;
    public MemberPolicy(UserRepository users,ClubRepository clubs,MembershipRepository memberships){this.users=users;this.clubs=clubs;this.memberships=memberships;}
    public Optional<Club> primaryClub(){return clubs.findAll().stream().filter(c->c.active&&c.name.equals(CLUB_NAME)).findFirst();}
    public Optional<Membership> membership(Long userId){return primaryClub().flatMap(c->memberships.findByUserIdAndClubId(userId,c.id));}
    public String approvedType(User u){return membership(u.id).filter(m->m.status.equals("APPROVED")).map(m->m.joinType).orElse("UNASSIGNED");}
    public List<String> rooms(User u){if(u.role.equals("ADMIN")||approvedType(u).equals("CLUB"))return List.of(HEGUANG,NORTH);return approvedType(u).equals("SOCIETY")?List.of(NORTH):List.of();}
    public List<String> usages(User u){if(u.role.equals("ADMIN")||approvedType(u).equals("CLUB"))return List.of("训练","存车","拿车","维修","休息","其他");return approvedType(u).equals("SOCIETY")?List.of("存车","拿车"):List.of();}
    public void requireRoom(User u,String room,String usage){if(!rooms(u).contains(room)||!usages(u).contains(usage))throw new ApiException(HttpStatus.FORBIDDEN,"当前成员身份不允许此场所或用途；社团成员仅可在北三骑行室存取车辆，特殊情况请提前联系管理员");}
    public static void validateType(String type){if(!Set.of("CLUB","SOCIETY").contains(Objects.toString(type,"")))throw ApiException.bad("请选择加入俱乐部或社团");}
    public static void validateDivision(String type,String division){if(!Set.of("","课程部","活动部","宣传部").contains(Objects.toString(division,"")))throw ApiException.bad("俱乐部部门无效");if(!type.equals("CLUB")&&division!=null&&!division.isBlank())throw ApiException.bad("社团成员不分配俱乐部部门");}
    public Map<String,Object> view(User u){
        var out=ApiViews.user(u);var m=membership(u.id);
        out.put("memberStatus",m.map(x->x.status).orElse("NONE"));out.put("memberType",m.map(x->x.joinType).orElse(u.joinType));
        out.put("clubDivision",m.map(x->x.clubDivision).orElse(""));out.put("memberNote",m.map(x->x.reviewNote).orElse(""));
        String type=m.filter(x->x.status.equals("APPROVED")).map(x->x.joinType).orElse("UNASSIGNED");
        boolean club=u.role.equals("ADMIN")||type.equals("CLUB"),society=type.equals("SOCIETY");
        out.put("canBorrowEquipment",u.enabled);out.put("allowedRooms",club?List.of(HEGUANG,NORTH):society?List.of(NORTH):List.of());
        out.put("allowedUsages",club?List.of("训练","存车","拿车","维修","休息","其他"):society?List.of("存车","拿车"):List.of());return out;
    }
    public User save(Long id,Requests.MemberInput x){
        var u=users.findById(id).orElseThrow(ApiException::missing);
        if(!Set.of("UNASSIGNED","CLUB","SOCIETY").contains(x.joinType()))throw ApiException.bad("成员类别无效");
        if(!Set.of("NONE","PENDING","APPROVED","REJECTED","WITHDRAWN").contains(x.memberStatus()))throw ApiException.bad("成员状态无效");
        validateDivision(x.joinType(),x.clubDivision());
        if(x.memberStatus().equals("APPROVED"))validateType(x.joinType());
        var email=x.email().trim().toLowerCase(Locale.ROOT);
        if(!u.email.equals(email)&&users.existsByEmail(email))throw ApiException.bad("邮箱已被使用");
        var existing=membership(id);
        if(x.memberStatus().equals("NONE")&&existing.isPresent())throw ApiException.bad("已有申请记录，请使用已退出保留历史记录");
        u.name=x.name().trim();u.email=email;u.studentNo=x.studentNo().trim();u.department=x.department().trim();u.contact=x.contact().trim();u.grade=x.grade().trim();u.joinType=x.joinType();
        if(!x.memberStatus().equals("NONE")){
            var club=primaryClub().orElseThrow(()->ApiException.bad("请先启用征途俱乐部资料"));
            var m=existing.orElseGet(Membership::new);m.userId=id;m.clubId=club.id;
            if(m.id==null)m.reason="管理员登记成员";
            m.contact=u.contact;m.grade=u.grade;m.college=u.department;m.joinType=x.joinType();m.clubDivision=Objects.toString(x.clubDivision(),"");m.status=x.memberStatus();m.reviewNote=Objects.toString(x.note(),"");m.reviewedAt=m.status.equals("PENDING")?null:Instant.now();memberships.save(m);
        }
        return users.save(u);
    }
}
