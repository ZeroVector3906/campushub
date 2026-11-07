package cn.campushub;

import cn.campushub.Domain.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import static cn.campushub.ApiViews.v;

@RestController @RequestMapping("/api")
public class CampusController {
    @Value("${campushub.seed-demo}") boolean demoEnabled;
    final MemberPolicy members;
    final CampusService s; final ApiViews views; final AuthController auth; final PasswordEncoder encoder;
    final FindByIndexNameSessionRepository<? extends Session> sessions;
    public CampusController(CampusService s,ApiViews views,AuthController auth,PasswordEncoder encoder,FindByIndexNameSessionRepository<? extends Session> sessions,MemberPolicy members) { this.members=members;this.s=s;this.views=views;this.auth=auth;this.encoder=encoder;this.sessions=sessions; }
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","UP","application","CampusHub","club","西安欧亚学院征途单车俱乐部&社团"); }
    @GetMapping("/clubs") List<?> clubs() { return s.clubs().findAll().stream().filter(c -> c.active).map(views::club).toList(); }
    private Set<Long> activeClubIds() { return s.clubs().findAll().stream().filter(c -> c.active).map(c -> c.id).collect(java.util.stream.Collectors.toSet()); }
    @GetMapping("/activities") List<?> activities() {
        var clubIds=activeClubIds();
        return s.activities().findAll().stream().filter(a -> clubIds.contains(a.clubId) && a.status.equals("PUBLISHED"))
            .sorted(Comparator.comparing(a -> a.startsAt)).map(a -> views.activity(a,false)).toList();
    }
    @GetMapping("/equipment") List<?> equipment() { return s.equipment().findAll().stream().filter(e -> e.active).map(ApiViews::equipment).toList(); }
    @GetMapping("/public/overview") Map<String,Object> overview() {
        var clubIds=activeClubIds();
        var activities=s.activities().findAll().stream().filter(a -> clubIds.contains(a.clubId) && a.status.equals("PUBLISHED")).toList();
        return v("demoEnabled",demoEnabled,"clubCount",s.clubs().findAll().stream().filter(c -> c.active).count(),"userCount",s.users().count(),
            "memberCount",s.memberships().findAll().stream().filter(m -> clubIds.contains(m.clubId) && m.status.equals("APPROVED")).map(m -> m.userId).distinct().count(),
            "activityCount",activities.size(),"upcomingCount",activities.stream().filter(a -> a.endsAt.isAfter(Instant.now())).count(),
            "equipmentCount",s.equipment().findAll().stream().filter(e -> e.active).mapToInt(e -> e.totalQuantity).sum());
    }
    @GetMapping("/me/dashboard") Map<String,Object> mine(Authentication a) {
        var u=auth.current(a);return v("member",members.view(u),"memberships",s.memberships().findByUserIdOrderByCreatedAtDesc(u.id).stream().map(views::membership).toList(),
            "registrations",s.registrations().findByUserIdOrderByCreatedAtDesc(u.id).stream().map(views::registration).toList(),
            "loans",s.loans().findByUserIdOrderByCreatedAtDesc(u.id).stream().map(views::loan).toList(),
            "repairs",s.repairs().findByUserIdOrderByCreatedAtDesc(u.id).stream().map(views::repair).toList());
    }
    @PutMapping("/me/profile") @Transactional Map<String,Object> profile(Authentication a,@Valid @RequestBody Requests.Profile input) {
        var u=auth.current(a);String email=input.email().toLowerCase(Locale.ROOT).trim();
        if(!u.email.equals(email) && s.users().existsByEmail(email)) throw ApiException.bad("邮箱已被使用");
        u.name=input.name().trim();u.email=email;u.department=input.department().trim();u.bio=input.bio()==null?"":input.bio();return ApiViews.user(s.users().save(u));
    }
    @PutMapping("/me/password") @Transactional Map<String,String> password(Authentication a,@Valid @RequestBody Requests.Password input,HttpServletRequest request) {
        var u=auth.current(a);if(!encoder.matches(input.oldPassword(),u.passwordHash)) throw ApiException.bad("原密码不正确");
        AuthController.checkPassword(input.newPassword());u.passwordHash=encoder.encode(input.newPassword());s.users().save(u);
        // Revoke every persisted session, including the current session.
        sessions.findByPrincipalName(u.username).keySet().forEach(sessions::deleteById);
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return Map.of("message","密码已更新，请重新登录");
    }
    @PostMapping("/clubs/{id}/applications") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> apply(Authentication a,@PathVariable Long id,@Valid @RequestBody Requests.Application input) { return views.membership(s.apply(auth.current(a),id,input)); }
    @PostMapping("/applications/{id}/withdraw") Map<String,String> withdraw(Authentication a,@PathVariable Long id) { s.withdraw(auth.current(a),id);return Map.of("message","已撤回申请或退出社团"); }
    @PostMapping("/activities/{id}/registrations") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> register(Authentication a,@PathVariable Long id) { return views.registration(s.register(auth.current(a),id)); }
    @DeleteMapping("/registrations/{id}") Map<String,String> cancelRegistration(Authentication a,@PathVariable Long id) { s.cancelRegistration(auth.current(a),id);return Map.of("message","已取消报名"); }
    @PostMapping("/registrations/{id}/checkin") Map<String,Object> checkin(Authentication a,@PathVariable Long id,@Valid @RequestBody Requests.Checkin input) { return views.registration(s.checkin(auth.current(a),id,input.code())); }
    @PostMapping("/loans") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> loan(Authentication a,@Valid @RequestBody Requests.LoanInput input) { return views.loan(s.applyLoan(auth.current(a),input)); }
    @PostMapping("/loans/{id}/return") Map<String,Object> returning(Authentication a,@PathVariable Long id) { return views.loan(s.loanAction(auth.current(a),id,true)); }
    @PostMapping("/loans/{id}/cancel") Map<String,Object> cancelLoan(Authentication a,@PathVariable Long id) { return views.loan(s.loanAction(auth.current(a),id,false)); }
    @PostMapping("/repairs") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> repair(Authentication a,@Valid @RequestBody Requests.RepairInput input) { return views.repair(s.repair(auth.current(a),input)); }

    @GetMapping("/admin/clubs") List<?> adminClubs() { return s.clubs().findAll().stream().map(views::club).toList(); }
    @GetMapping("/admin/activities") List<?> adminActivities() { return s.activities().findAll().stream().sorted(Comparator.comparing((Activity a)->a.createdAt).reversed()).map(a->views.activity(a,true)).toList(); }
    @GetMapping("/admin/equipment") List<?> adminEquipment() { return s.equipment().findAll().stream().map(ApiViews::equipment).toList(); }
    @GetMapping("/admin/applications") List<?> adminApplications() { return s.memberships().findAll().stream().sorted(Comparator.comparing((Membership m)->m.createdAt).reversed()).map(views::membership).toList(); }
    @GetMapping("/admin/registrations") List<?> adminRegistrations() { return s.registrations().findAll().stream().map(views::registration).toList(); }
    @GetMapping("/admin/loans") List<?> adminLoans() { return s.loans().findAll().stream().sorted(Comparator.comparing((Loan l)->l.createdAt).reversed()).map(views::loan).toList(); }
    @GetMapping("/admin/repairs") List<?> adminRepairs() { return s.repairs().findAll().stream().sorted(Comparator.comparing((RepairTicket t)->t.createdAt).reversed()).map(views::repair).toList(); }
    @GetMapping("/admin/users") List<?> adminUsers() { return s.users().findAll().stream().map(members::view).toList(); }
    @PutMapping("/admin/users/{id}/access") @Transactional Map<String,Object> access(Authentication a,@PathVariable Long id,@Valid @RequestBody Requests.UserAccess input) {
        CampusService.requireStatus(input.role(),"STUDENT","ADMIN");var me=auth.current(a);
        if(me.id.equals(id)) throw ApiException.bad("不能修改自己的权限或停用自己");
        var u=s.users().findById(id).orElseThrow(ApiException::missing);u.role=input.role();u.enabled=input.enabled();return ApiViews.user(s.users().save(u));
    }
    @PostMapping("/admin/clubs") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createClub(@Valid @RequestBody Requests.ClubInput x) { return views.club(s.saveClub(null,x)); }
    @PutMapping("/admin/clubs/{id}") Map<String,Object> updateClub(@PathVariable Long id,@Valid @RequestBody Requests.ClubInput x) { return views.club(s.saveClub(id,x)); }
    @PostMapping("/admin/activities") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createActivity(@Valid @RequestBody Requests.ActivityInput x) { return views.activity(s.saveActivity(null,x),true); }
    @PutMapping("/admin/activities/{id}") Map<String,Object> updateActivity(@PathVariable Long id,@Valid @RequestBody Requests.ActivityInput x) { return views.activity(s.saveActivity(id,x),true); }
    @PostMapping("/admin/equipment") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createEquipment(@Valid @RequestBody Requests.EquipmentInput x) { return ApiViews.equipment(s.saveEquipment(null,x)); }
    @PutMapping("/admin/equipment/{id}") Map<String,Object> updateEquipment(@PathVariable Long id,@Valid @RequestBody Requests.EquipmentInput x) { return ApiViews.equipment(s.saveEquipment(id,x)); }
    @PutMapping("/admin/applications/{id}/review") Map<String,Object> reviewApplication(@PathVariable Long id,@Valid @RequestBody Requests.Review x) { return views.membership(s.reviewMembership(id,x)); }
    @PutMapping("/admin/loans/{id}/review") Map<String,Object> reviewLoan(@PathVariable Long id,@Valid @RequestBody Requests.Review x) { return views.loan(s.reviewLoan(id,x)); }
    @PostMapping("/admin/repairs/{id}/progress") Map<String,Object> progress(Authentication a,@PathVariable Long id,@Valid @RequestBody Requests.RepairProgress x) { return views.repair(s.progress(auth.current(a),id,x)); }
}
