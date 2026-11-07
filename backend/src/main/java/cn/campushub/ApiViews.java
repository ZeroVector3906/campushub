package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class ApiViews {
    final UserRepository users; final ClubRepository clubs; final MembershipRepository memberships;
    final ActivityRepository activities; final RegistrationRepository registrations; final EquipmentRepository equipment;
    final RepairUpdateRepository updates;
    public ApiViews(UserRepository users,ClubRepository clubs,MembershipRepository memberships,ActivityRepository activities,
                    RegistrationRepository registrations,EquipmentRepository equipment,RepairUpdateRepository updates) {
        this.users=users;this.clubs=clubs;this.memberships=memberships;this.activities=activities;this.registrations=registrations;this.equipment=equipment;this.updates=updates;
    }
    static Map<String,Object> v(Object... pairs) {
        var out=new LinkedHashMap<String,Object>(); for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]); return out;
    }
    static Map<String,Object> user(User u) {
        return v("id",u.id,"username",u.username,"name",u.name,"email",u.email,"studentNo",u.studentNo,"department",u.department,
                "bio",u.bio,"role",u.role,"enabled",u.enabled,"createdAt",u.createdAt);
    }
    Map<String,Object> club(Club c) {
        return v("id",c.id,"name",c.name,"category",c.category,"summary",c.summary,"description",c.description,"icon",c.icon,"color",c.color,
                "location",c.location,"contact",c.contact,"recruiting",c.recruiting,"active",c.active,"memberCount",memberships.countByClubIdAndStatus(c.id,"APPROVED"));
    }
    Map<String,Object> activity(Activity a,boolean admin) {
        long count=registrations.countByActivityId(a.id);
        var out=v("id",a.id,"clubId",a.clubId,"clubName",clubs.findById(a.clubId).map(c -> c.name).orElse("社团已归档"),
                "title",a.title,"category",a.category,"description",a.description,"location",a.location,"startsAt",a.startsAt,"endsAt",a.endsAt,
                "registrationDeadline",a.registrationDeadline,"capacity",a.capacity,"registrationCount",count,"featured",a.featured,"status",a.status,
                "canRegister",a.status.equals("PUBLISHED") && a.registrationDeadline.isAfter(Instant.now()) && count<a.capacity);
        if(admin) out.put("checkinCode",a.checkinCode); return out;
    }
    Map<String,Object> membership(Membership m) {
        return v("id",m.id,"user",users.findById(m.userId).map(ApiViews::user).orElse(null),"club",clubs.findById(m.clubId).map(this::club).orElse(null),
                "reason",m.reason,"contact",m.contact,"grade",m.grade,"college",m.college,"status",m.status,"reviewNote",m.reviewNote,"createdAt",m.createdAt,"reviewedAt",m.reviewedAt);
    }
    Map<String,Object> registration(Registration r) {
        return v("id",r.id,"user",users.findById(r.userId).map(ApiViews::user).orElse(null),"activity",activities.findById(r.activityId).map(a -> activity(a,false)).orElse(null),
                "createdAt",r.createdAt,"checkedInAt",r.checkedInAt);
    }
    static Map<String,Object> equipment(Equipment e) {
        return v("id",e.id,"name",e.name,"category",e.category,"assetCode",e.assetCode,"description",e.description,"location",e.location,
                "totalQuantity",e.totalQuantity,"availableQuantity",e.availableQuantity,"active",e.active);
    }
    Map<String,Object> loan(Loan l) {
        return v("id",l.id,"user",users.findById(l.userId).map(ApiViews::user).orElse(null),"equipment",equipment.findById(l.equipmentId).map(ApiViews::equipment).orElse(null),
                "quantity",l.quantity,"purpose",l.purpose,"dueDate",l.dueDate,"status",l.status,"reviewNote",l.reviewNote,"createdAt",l.createdAt,"returnedAt",l.returnedAt,
                "overdue",Set.of("APPROVED","RETURN_PENDING").contains(l.status) && l.dueDate.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai"))));
    }
    Map<String,Object> repair(RepairTicket t) {
        return v("id",t.id,"user",users.findById(t.userId).map(ApiViews::user).orElse(null),"equipmentId",t.equipmentId,
                "equipmentName",t.equipmentId==null?null:equipment.findById(t.equipmentId).map(e -> e.name).orElse(null),
                "title",t.title,"description",t.description,"location",t.location,"priority",t.priority,"status",t.status,"createdAt",t.createdAt,"updatedAt",t.updatedAt,
                "updates",updates.findByTicketIdOrderByCreatedAtAsc(t.id).stream().map(u -> v("id",u.id,"status",u.status,"note",u.note,"operatorName",u.operatorName,"createdAt",u.createdAt)).toList());
    }
}
