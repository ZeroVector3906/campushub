package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static cn.campushub.ApiViews.v;

@RestController @RequestMapping("/api/admin/reports")
public class ReportController {
    private final CampusService s;
    public ReportController(CampusService s) { this.s=s; }
    @GetMapping Map<String,Object> report() {
        var clubRows=s.clubs().findAll();var equipmentRows=s.equipment().findAll();
        var activeClubs=clubRows.stream().filter(c -> c.active).map(c -> c.id).collect(java.util.stream.Collectors.toSet());
        var activities=s.activities().findAll().stream().filter(a -> activeClubs.contains(a.clubId)).toList();var activityIds=activities.stream().map(a -> a.id).collect(java.util.stream.Collectors.toSet());var registrations=s.registrations().findAll().stream().filter(r -> activityIds.contains(r.activityId)).toList();var memberships=s.memberships().findAll().stream().filter(m -> activeClubs.contains(m.clubId)).toList();var loans=s.loans().findAll();var repairs=s.repairs().findAll();
        var month=YearMonth.now(ZoneId.of("Asia/Shanghai"));var trend=new ArrayList<Map<String,Object>>();
        for(int i=5;i>=0;i--) {
            var m=month.minusMonths(i);trend.add(v("month",m.toString(),"activities",activities.stream().filter(a -> YearMonth.from(a.startsAt.atZone(ZoneId.of("Asia/Shanghai"))).equals(m) && a.status.equals("PUBLISHED")).count(),
                "registrations",registrations.stream().filter(r -> YearMonth.from(r.createdAt.atZone(ZoneId.of("Asia/Shanghai"))).equals(m)).count()));
        }
        return v("userCount",s.users().count(),"clubCount",clubRows.stream().filter(c -> c.active).count(),
            "memberCount",memberships.stream().filter(m -> m.status.equals("APPROVED")).map(m -> m.userId).distinct().count(),
            "membershipCount",memberships.stream().filter(m -> m.status.equals("APPROVED")).count(),
            "activityCount",activities.stream().filter(a -> a.status.equals("PUBLISHED")).count(),"registrationCount",registrations.size(),
            "checkinCount",registrations.stream().filter(r -> r.checkedInAt!=null).count(),
            "pendingApplications",memberships.stream().filter(m -> m.status.equals("PENDING")).count(),
            "pendingLoans",loans.stream().filter(l -> Set.of("PENDING","RETURN_PENDING").contains(l.status)).count(),
            "openRepairs",repairs.stream().filter(r -> Set.of("OPEN","PROCESSING").contains(r.status)).count(),
            "equipmentTotal",equipmentRows.stream().filter(e -> e.active).mapToInt(e -> e.totalQuantity).sum(),
            "equipmentBorrowed",equipmentRows.stream().filter(e -> e.active).mapToInt(e -> e.totalQuantity-e.availableQuantity).sum(),
            "trend",trend,"clubs",clubRows.stream().filter(c -> c.active).map(c -> v("name",c.name,"category",c.category,"members",memberships.stream().filter(m->m.clubId.equals(c.id)&&m.status.equals("APPROVED")).count())).toList(),
            "equipment",equipmentRows.stream().filter(e -> e.active).map(e -> v("name",e.name,"total",e.totalQuantity,"borrowed",e.totalQuantity-e.availableQuantity,"loanCount",loans.stream().filter(l -> l.equipmentId.equals(e.id)).count())).toList());
    }
    @GetMapping("/export") ResponseEntity<byte[]> export(@RequestParam(defaultValue="activities") String type) {
        var rows=new ArrayList<List<?>>();
        switch(type) {
            case "activities" -> {
                rows.add(List.of("活动编号","活动名称","主办社团","开始时间","地点","名额","报名人数","签到人数","状态"));
                var clubNames=new HashMap<Long,String>();s.clubs().findAll().forEach(c->clubNames.put(c.id,c.name));
                var allRegistrations=s.registrations().findAll();
                var registrationCounts=allRegistrations.stream().collect(java.util.stream.Collectors.groupingBy(r->r.activityId,java.util.stream.Collectors.counting()));
                var checkinCounts=allRegistrations.stream().filter(r->r.checkedInAt!=null).collect(java.util.stream.Collectors.groupingBy(r->r.activityId,java.util.stream.Collectors.counting()));
                for(var a:s.activities().findAll()) rows.add(List.of(a.id,a.title,clubNames.get(a.clubId),a.startsAt.atZone(ZoneId.of("Asia/Shanghai")).toString(),a.location,a.capacity,registrationCounts.getOrDefault(a.id,0L),checkinCounts.getOrDefault(a.id,0L),label(a.status)));
            }
            case "members" -> {
                rows.add(List.of("组织","姓名","学号","院系","年级","联系方式","加入身份","部门","申请时间","状态"));
                var membershipRows=s.memberships().findAll();var people=new HashMap<Long,User>();var clubNames=new HashMap<Long,String>();
                s.users().findAllById(membershipRows.stream().map(m->m.userId).distinct().toList()).forEach(u->people.put(u.id,u));
                s.clubs().findAllById(membershipRows.stream().map(m->m.clubId).distinct().toList()).forEach(c->clubNames.put(c.id,c.name));
                for(var m:membershipRows) {var u=people.get(m.userId);rows.add(List.of(clubNames.get(m.clubId),u.name,u.studentNo,u.department,m.grade,m.contact,m.joinType.equals("CLUB")?"俱乐部":m.joinType.equals("SOCIETY")?"社团":"待分类",m.clubDivision,m.createdAt,label(m.status)));}
            }
            case "equipment" -> {
                rows.add(List.of("资产编号","器材名称","类别","存放地点","总库存","可用库存","在借数量","累计申请次数"));
                var loanCounts=s.loans().findAll().stream().collect(java.util.stream.Collectors.groupingBy(l->l.equipmentId,java.util.stream.Collectors.counting()));
                for(var e:s.equipment().findAll()) rows.add(List.of(e.assetCode,e.name,e.category,e.location,e.totalQuantity,e.availableQuantity,e.totalQuantity-e.availableQuantity,loanCounts.getOrDefault(e.id,0L)));
            }
            case "repairs" -> {
                rows.add(List.of("工单编号","标题","报修人","地点","优先级","状态","提交时间"));
                var repairRows=s.repairs().findAll();var names=new HashMap<Long,String>();s.users().findAllById(repairRows.stream().map(r->r.userId).distinct().toList()).forEach(u->names.put(u.id,u.name));
                for(var r:repairRows) rows.add(List.of(r.id,r.title,names.get(r.userId),r.location,label(r.priority),label(r.status),r.createdAt));
            }
            default -> throw ApiException.bad("不支持的报表类型");
        }
        var csv=new StringBuilder("\uFEFF");
        for(var row:rows) csv.append(String.join(",",row.stream().map(ReportController::cell).toList())).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=campushub-"+type+".csv")
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    static String cell(Object value) {
        String x=String.valueOf(value);String trimmed=x.stripLeading();
        if(!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0))>=0) x="'"+x;
        return "\""+x.replace("\"","\"\"")+"\"";
    }
    static String label(String status) {
        return switch(status) {case "PENDING"->"待审核";case "APPROVED"->"已通过";case "REJECTED"->"已驳回";case "WITHDRAWN"->"已退出";case "PUBLISHED"->"已发布";case "CANCELLED"->"已取消";case "OPEN"->"待受理";case "PROCESSING"->"维修中";case "RESOLVED"->"已修复";case "CLOSED"->"已关闭";case "HIGH"->"紧急";case "LOW"->"低";default->"普通";};
    }
}
