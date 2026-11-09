package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
@org.springframework.web.context.annotation.RequestScope
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
                "bio",u.bio,"joinType",u.joinType,"contact",u.contact,"grade",u.grade,"role",u.role,"enabled",u.enabled,"createdAt",u.createdAt);
    }
    public Map<String,Object> club(Club c) {
        return v("id",c.id,"name",c.name,"category",c.category,"summary",c.summary,"description",c.description,"icon",c.icon,"color",c.color,
                "location",c.location,"contact",c.contact,"recruiting",c.recruiting,"active",c.active,"memberCount",clubCount(c.id));
    }
    public Map<String,Object> activity(Activity a,boolean admin) {
        long count=registrationCount(a.id);
        var out=v("id",a.id,"clubId",a.clubId,"clubName",findClub(a.clubId).map(c -> c.name).orElse("社团已归档"),
                "title",a.title,"category",a.category,"description",a.description,"location",a.location,"startsAt",a.startsAt,"endsAt",a.endsAt,
                "registrationDeadline",a.registrationDeadline,"capacity",a.capacity,"registrationCount",count,"featured",a.featured,"status",a.status,
                "canRegister",a.status.equals("PUBLISHED") && a.registrationDeadline.isAfter(Instant.now()) && count<a.capacity);
        if(admin) out.put("checkinCode",a.checkinCode); return out;
    }
    public Map<String,Object> membership(Membership m) {
        return v("id",m.id,"user",findUser(m.userId).map(ApiViews::user).orElse(null),"club",findClub(m.clubId).map(this::club).orElse(null),
                "joinType",m.joinType,"clubDivision",m.clubDivision,"reason",m.reason,"contact",m.contact,"grade",m.grade,"college",m.college,"status",m.status,"reviewNote",m.reviewNote,"createdAt",m.createdAt,"reviewedAt",m.reviewedAt);
    }
    public Map<String,Object> registration(Registration r) {
        return v("id",r.id,"user",findUser(r.userId).map(ApiViews::user).orElse(null),"activity",findActivity(r.activityId).map(a -> activity(a,false)).orElse(null),
                "createdAt",r.createdAt,"checkedInAt",r.checkedInAt);
    }
    static Map<String,Object> equipment(Equipment e) {
        return v("id",e.id,"name",e.name,"category",e.category,"assetCode",e.assetCode,"description",e.description,"location",e.location,
                "totalQuantity",e.totalQuantity,"availableQuantity",e.availableQuantity,"active",e.active);
    }
    public Map<String,Object> loan(Loan l) {
        return v("id",l.id,"user",findUser(l.userId).map(ApiViews::user).orElse(null),"equipment",findEquipment(l.equipmentId).map(ApiViews::equipment).orElse(null),
                "quantity",l.quantity,"purpose",l.purpose,"dueDate",l.dueDate,"status",l.status,"reviewNote",l.reviewNote,"createdAt",l.createdAt,"returnedAt",l.returnedAt,
                "overdue",Set.of("APPROVED","RETURN_PENDING").contains(l.status) && l.dueDate.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai"))));
    }
    public Map<String,Object> repair(RepairTicket t) {
        return v("id",t.id,"user",findUser(t.userId).map(ApiViews::user).orElse(null),"equipmentId",t.equipmentId,
                "equipmentName",t.equipmentId==null?null:findEquipment(t.equipmentId).map(e -> e.name).orElse(null),
                "title",t.title,"description",t.description,"location",t.location,"priority",t.priority,"status",t.status,"createdAt",t.createdAt,"updatedAt",t.updatedAt,
                "updates",updatesFor(t.id).stream().map(u -> v("id",u.id,"status",u.status,"note",u.note,"operatorName",u.operatorName,"createdAt",u.createdAt)).toList());
    }

    // Request-local lookup indexes; never retain entity data between HTTP requests.
    private final Map<Long,User> userIndex=new HashMap<>();
    private final Map<Long,Club> clubIndex=new HashMap<>();
    private final Map<Long,Activity> activityIndex=new HashMap<>();
    private final Map<Long,Equipment> equipmentIndex=new HashMap<>();
    private final Map<Long,List<RepairUpdate>> updateIndex=new HashMap<>();
    private Map<Long,Long> clubCounts,registrationCounts;
    private Optional<User> findUser(Long id){return Optional.ofNullable(userIndex.computeIfAbsent(id,k->users.findById(k).orElse(null)));}
    private Optional<Club> findClub(Long id){return Optional.ofNullable(clubIndex.computeIfAbsent(id,k->clubs.findById(k).orElse(null)));}
    private Optional<Activity> findActivity(Long id){return Optional.ofNullable(activityIndex.computeIfAbsent(id,k->activities.findById(k).orElse(null)));}
    private Optional<Equipment> findEquipment(Long id){return Optional.ofNullable(equipmentIndex.computeIfAbsent(id,k->equipment.findById(k).orElse(null)));}
    private static Map<Long,Long> counts(List<Object[]> rows){var out=new HashMap<Long,Long>();for(var row:rows)out.put(((Number)row[0]).longValue(),((Number)row[1]).longValue());return out;}
    private long clubCount(Long id){if(clubCounts==null)clubCounts=counts(memberships.approvedClubCounts());return clubCounts.getOrDefault(id,0L);}
    private long registrationCount(Long id){if(registrationCounts==null)registrationCounts=counts(registrations.activityCounts());return registrationCounts.getOrDefault(id,0L);}
    private List<RepairUpdate> updatesFor(Long id){return updateIndex.computeIfAbsent(id,k->updates.findByTicketIdOrderByCreatedAtAsc(k));}
    public void prepareUsers(Collection<Long> ids){if(!ids.isEmpty())users.findAllById(new HashSet<>(ids)).forEach(u->userIndex.put(u.id,u));}
    private void prepareClubs(Collection<Long> ids){if(!ids.isEmpty())clubs.findAllById(new HashSet<>(ids)).forEach(c->clubIndex.put(c.id,c));}
    private void prepareEquipment(Collection<Long> ids){if(!ids.isEmpty())equipment.findAllById(new HashSet<>(ids)).forEach(e->equipmentIndex.put(e.id,e));}
    public Map<String,Object> person(Long id){return findUser(id).map(ApiViews::user).orElse(null);}
    public List<Map<String,Object>> clubRows(List<Club> rows){return rows.stream().map(this::club).toList();}
    public List<Map<String,Object>> activityRows(List<Activity> rows,boolean admin){prepareClubs(rows.stream().map(a->a.clubId).toList());return rows.stream().map(a->activity(a,admin)).toList();}
    public List<Map<String,Object>> membershipRows(List<Membership> rows){prepareUsers(rows.stream().map(m->m.userId).toList());prepareClubs(rows.stream().map(m->m.clubId).toList());return rows.stream().map(this::membership).toList();}
    public List<Map<String,Object>> registrationRows(List<Registration> rows){
        prepareUsers(rows.stream().map(r->r.userId).toList());
        if(!rows.isEmpty()){var parents=activities.findAllById(rows.stream().map(r->r.activityId).distinct().toList());parents.forEach(a->activityIndex.put(a.id,a));prepareClubs(parents.stream().map(a->a.clubId).toList());}
        return rows.stream().map(this::registration).toList();
    }
    public List<Map<String,Object>> loanRows(List<Loan> rows){prepareUsers(rows.stream().map(l->l.userId).toList());prepareEquipment(rows.stream().map(l->l.equipmentId).toList());return rows.stream().map(this::loan).toList();}
    public List<Map<String,Object>> repairRows(List<RepairTicket> rows){
        prepareUsers(rows.stream().map(t->t.userId).toList());prepareEquipment(rows.stream().map(t->t.equipmentId).filter(Objects::nonNull).toList());
        if(!rows.isEmpty()){var ids=rows.stream().map(t->t.id).toList();ids.forEach(id->updateIndex.put(id,new ArrayList<>()));updates.findByTicketIdInOrderByCreatedAtAsc(ids).forEach(u->updateIndex.get(u.ticketId).add(u));}
        return rows.stream().map(this::repair).toList();
    }
}
