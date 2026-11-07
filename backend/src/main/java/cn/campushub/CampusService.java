package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Transactional
public class CampusService {
    final UserRepository users; final ClubRepository clubs; final MembershipRepository memberships;
    final ActivityRepository activities; final RegistrationRepository registrations; final EquipmentRepository equipment;
    final LoanRepository loans; final RepairRepository repairs; final RepairUpdateRepository updates;
    public CampusService(UserRepository users,ClubRepository clubs,MembershipRepository memberships,ActivityRepository activities,
                         RegistrationRepository registrations,EquipmentRepository equipment,LoanRepository loans,RepairRepository repairs,RepairUpdateRepository updates) {
        this.users=users;this.clubs=clubs;this.memberships=memberships;this.activities=activities;this.registrations=registrations;
        this.equipment=equipment;this.loans=loans;this.repairs=repairs;this.updates=updates;
    }
    public UserRepository users() { return users; }
    public ClubRepository clubs() { return clubs; }
    public MembershipRepository memberships() { return memberships; }
    public ActivityRepository activities() { return activities; }
    public RegistrationRepository registrations() { return registrations; }
    public EquipmentRepository equipment() { return equipment; }
    public LoanRepository loans() { return loans; }
    public RepairRepository repairs() { return repairs; }
    public RepairUpdateRepository updates() { return updates; }
    Membership apply(User user,Long clubId,Requests.Application input) {
        var club=clubs.findById(clubId).orElseThrow(ApiException::missing);
        if(!club.active || !club.recruiting) throw ApiException.bad("该社团暂未开放招新");
        var m=memberships.findByUserIdAndClubId(user.id,clubId).orElseGet(Membership::new);
        if(Set.of("PENDING","APPROVED").contains(m.status) && m.id!=null) throw ApiException.bad("你已申请或已加入该社团");
        m.userId=user.id;m.clubId=clubId;m.reason=input.reason();m.contact=input.contact().trim();m.grade=input.grade().trim();m.college=input.college().trim();m.status="PENDING";m.reviewNote="";m.createdAt=Instant.now();m.reviewedAt=null;
        return memberships.save(m);
    }
    void withdraw(User user,Long id) {
        var m=memberships.lockById(id).orElseThrow(ApiException::missing); own(user,m.userId);
        if(!Set.of("PENDING","APPROVED").contains(m.status)) throw ApiException.bad("当前申请无法撤回");
        m.status="WITHDRAWN";m.reviewedAt=Instant.now();memberships.save(m);
    }
    Membership reviewMembership(Long id,Requests.Review input) {
        requireStatus(input.status(),"APPROVED","REJECTED");
        var m=memberships.lockById(id).orElseThrow(ApiException::missing);
        if(!m.status.equals("PENDING")) throw ApiException.bad("该申请已处理");
        m.status=input.status();m.reviewNote=note(input.note());m.reviewedAt=Instant.now();return memberships.save(m);
    }
    Registration register(User user,Long activityId) {
        var a=activities.lockById(activityId).orElseThrow(ApiException::missing);
        if(!a.status.equals("PUBLISHED") || !a.registrationDeadline.isAfter(Instant.now())) throw ApiException.bad("报名已截止或活动已取消");
        if(!clubs.findById(a.clubId).orElseThrow().active) throw ApiException.bad("主办社团已归档");
        if(registrations.findByUserIdAndActivityId(user.id,activityId).isPresent()) throw ApiException.bad("你已经报名此活动");
        if(registrations.countByActivityId(activityId)>=a.capacity) throw ApiException.bad("报名人数已满");
        var r=new Registration();r.userId=user.id;r.activityId=activityId;return registrations.save(r);
    }
    void cancelRegistration(User user,Long id) {
        var r=registrations.lockById(id).orElseThrow(ApiException::missing);own(user,r.userId);
        var a=activities.lockById(r.activityId).orElseThrow();
        if(r.checkedInAt!=null || (a.startsAt.isBefore(Instant.now()) && !a.status.equals("CANCELLED"))) throw ApiException.bad("活动已开始或已签到，无法取消报名");
        registrations.delete(r);
    }
    Registration checkin(User user,Long id,String code) {
        var r=registrations.lockById(id).orElseThrow(ApiException::missing);own(user,r.userId);
        var a=activities.lockById(r.activityId).orElseThrow();
        if(!a.status.equals("PUBLISHED")) throw ApiException.bad("活动已取消");
        var now=Instant.now();
        if(now.isBefore(a.startsAt.minusSeconds(1800)) || now.isAfter(a.endsAt)) throw ApiException.bad("签到时间为活动开始前 30 分钟至活动结束");
        if(!a.checkinCode.equals(code.trim())) throw ApiException.bad("签到码不正确，请向现场负责人获取");
        if(r.checkedInAt!=null) throw ApiException.bad("你已完成签到");
        r.checkedInAt=now;return registrations.save(r);
    }
    Loan applyLoan(User user,Requests.LoanInput input) {
        var e=equipment.findById(input.equipmentId()).orElseThrow(ApiException::missing);
        if(!e.active || e.availableQuantity<input.quantity()) throw ApiException.bad("器材库存不足或已停用");
        var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if(input.dueDate().isBefore(today) || input.dueDate().isAfter(today.plusDays(90))) throw ApiException.bad("归还日期须在今天至 90 天内");
        var l=new Loan();l.userId=user.id;l.equipmentId=e.id;l.quantity=input.quantity();l.purpose=input.purpose();l.dueDate=input.dueDate();return loans.save(l);
    }
    Loan reviewLoan(Long id,Requests.Review input) {
        requireStatus(input.status(),"APPROVED","REJECTED","RETURNED");
        var l=loans.lockById(id).orElseThrow(ApiException::missing);
        if(input.status().equals("RETURNED")) {
            if(!l.status.equals("RETURN_PENDING")) throw ApiException.bad("请等待借用人提交归还申请");
            var e=equipment.lockById(l.equipmentId).orElseThrow();e.availableQuantity+=l.quantity;equipment.save(e);l.returnedAt=Instant.now();
        } else {
            if(!l.status.equals("PENDING")) throw ApiException.bad("借用申请已处理");
            if(input.status().equals("APPROVED")) {
                var e=equipment.lockById(l.equipmentId).orElseThrow();
                if(!e.active || e.availableQuantity<l.quantity) throw ApiException.bad("可用库存不足，无法批准");
                if(l.dueDate.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai")))) throw ApiException.bad("归还日期已过，请驳回后重新申请");
                e.availableQuantity-=l.quantity;equipment.save(e);
            }
        }
        l.status=input.status();l.reviewNote=note(input.note());return loans.save(l);
    }
    Loan loanAction(User user,Long id,boolean returning) {
        var l=loans.lockById(id).orElseThrow(ApiException::missing);own(user,l.userId);
        if(returning && l.status.equals("APPROVED")) l.status="RETURN_PENDING";
        else if(!returning && l.status.equals("PENDING")) l.status="CANCELLED";
        else throw ApiException.bad("当前状态无法执行此操作");
        return loans.save(l);
    }
    RepairTicket repair(User user,Requests.RepairInput input) {
        requireStatus(input.priority(),"LOW","NORMAL","HIGH");
        if(input.equipmentId()!=null && !equipment.existsById(input.equipmentId())) throw ApiException.bad("关联器材不存在");
        var t=new RepairTicket();t.userId=user.id;t.equipmentId=input.equipmentId();t.title=input.title();t.description=input.description();t.location=input.location();t.priority=input.priority();
        repairs.save(t); addUpdate(t,user.name,"工单已提交，等待受理");return t;
    }
    RepairTicket progress(User admin,Long id,Requests.RepairProgress input) {
        var t=repairs.lockById(id).orElseThrow(ApiException::missing);
        boolean valid=switch(t.status) {
            case "OPEN" -> input.status().equals("PROCESSING");
            case "PROCESSING" -> input.status().equals("PROCESSING") || input.status().equals("RESOLVED");
            case "RESOLVED" -> input.status().equals("PROCESSING") || input.status().equals("CLOSED");
            default -> false;
        };
        if(!valid) throw ApiException.bad("不允许此工单状态变更");
        t.status=input.status();t.updatedAt=Instant.now();addUpdate(t,admin.name,input.note());return repairs.save(t);
    }
    void addUpdate(RepairTicket t,String operator,String note) {
        var u=new RepairUpdate();u.ticketId=t.id;u.operatorName=operator;u.status=t.status;u.note=note;updates.save(u);
    }
    Club saveClub(Long id,Requests.ClubInput x) {
        var c=id==null?new Club():clubs.findById(id).orElseThrow(ApiException::missing);
        c.name=x.name();c.category=x.category();c.summary=x.summary();c.description=x.description();c.icon=x.icon();c.color=x.color();c.location=x.location();c.contact=x.contact();c.recruiting=x.recruiting();c.active=x.active();return clubs.save(c);
    }
    Activity saveActivity(Long id,Requests.ActivityInput x) {
        requireStatus(x.status(),"PUBLISHED","CANCELLED");
        if(!clubs.findById(x.clubId()).orElseThrow(ApiException::missing).active) throw ApiException.bad("请选择正常运营的社团");
        if(!x.endsAt().isAfter(x.startsAt()) || x.registrationDeadline().isAfter(x.startsAt())) throw ApiException.bad("请检查活动开始、结束与报名截止时间");
        var a=id==null?new Activity():activities.lockById(id).orElseThrow(ApiException::missing);
        if(id==null && !x.startsAt().isAfter(Instant.now())) throw ApiException.bad("新活动须安排在未来时间");
        if(id!=null && x.capacity()<registrations.countByActivityId(id)) throw ApiException.bad("名额不能小于已报名人数");
        a.clubId=x.clubId();a.title=x.title();a.category=x.category();a.description=x.description();a.location=x.location();a.startsAt=x.startsAt();a.endsAt=x.endsAt();a.registrationDeadline=x.registrationDeadline();
        a.capacity=x.capacity();a.checkinCode=x.checkinCode();a.featured=x.featured();a.status=x.status();return activities.save(a);
    }
    Equipment saveEquipment(Long id,Requests.EquipmentInput x) {
        var e=id==null?new Equipment():equipment.lockById(id).orElseThrow(ApiException::missing);
        int borrowed=e.totalQuantity-e.availableQuantity;
        if(x.totalQuantity()<borrowed) throw ApiException.bad("总库存不能少于尚未归还的数量："+borrowed);
        e.name=x.name();e.category=x.category();e.assetCode=x.assetCode();e.description=x.description();e.location=x.location();e.totalQuantity=x.totalQuantity();e.availableQuantity=x.totalQuantity()-borrowed;e.active=x.active();return equipment.save(e);
    }
    static void own(User u,Long owner) { if(!u.id.equals(owner)) throw ApiException.forbidden(); }
    static void requireStatus(String value,String... allowed) { if(!Arrays.asList(allowed).contains(value)) throw ApiException.bad("状态值无效"); }
    static String note(String value) { return value==null?"":value; }
}
