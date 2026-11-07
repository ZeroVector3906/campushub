package cn.campushub;

import cn.campushub.Domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:54329/campushub_test}","campushub.seed-demo=false","campushub.admin-password=TestAdminPassword2026!"})
@AutoConfigureMockMvc @Transactional
class CampusHubIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampusService s;
    @Autowired MemberPolicy members;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Test void ridingSuggestionsPreserveContactAndOnlyOwnerOrAdminCanRead() throws Exception {
        var body=Map.of("kind","SUGGESTION","title","集体骑行建议","description","希望组织周末集体骑行","contact","微信：test-rider");
        mvc.perform(post("/api/riding-requests").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        var result=postAs("/api/riding-requests",body,student.username).andExpect(status().isCreated()).andExpect(jsonPath("$.user.id").value(student.id)).andExpect(jsonPath("$.contact").value("微信：test-rider")).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/api/riding-requests").with(user(other.username))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == "+id+")]").isEmpty());
        mvc.perform(get("/api/admin/riding-requests").with(user(student.username))).andExpect(status().isForbidden());
        putAs("/api/admin/riding-requests/"+id+"/review",Map.of("status","APPROVED","note","请联系管理员确认路线"),admin.username).andExpect(status().isOk()).andExpect(jsonPath("$.reviewedAt").isNotEmpty());
        mvc.perform(get("/api/riding-requests").with(user(student.username))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == "+id+")].reviewNote").value(org.hamcrest.Matchers.hasItem("请联系管理员确认路线")));
    }
    @Test void roomRequestsValidatePlaceOtherUsageAndTimeRange() throws Exception {
        var body=new HashMap<String,Object>();body.put("kind","ROOM");body.put("title","骑行室申请");body.put("description","两人训练");body.put("contact","test@example.test");body.put("room","北三骑行室");body.put("usage","其他");body.put("startsAt",Instant.now().plusSeconds(3600).toString());body.put("endsAt",Instant.now().plusSeconds(7200).toString());
        postAs("/api/riding-requests",body,student.username).andExpect(status().isBadRequest());
        body.put("otherUsage","整理车辆配件");
        postAs("/api/riding-requests",body,student.username).andExpect(status().isCreated()).andExpect(jsonPath("$.room").value("北三骑行室")).andExpect(jsonPath("$.otherUsage").value("整理车辆配件"));
        body.put("endsAt",Instant.now().toString());postAs("/api/riding-requests",body,student.username).andExpect(status().isBadRequest());
        body.put("endsAt",Instant.now().plusSeconds(7200).toString());body.put("room","无效场所");postAs("/api/riding-requests",body,student.username).andExpect(status().isBadRequest());
        body.put("room","和光文体中心俱乐部");body.put("usage","训练");postAs("/api/riding-requests",body,student.username).andExpect(status().isCreated()).andExpect(jsonPath("$.otherUsage").value(""));
    }
    @Test void membershipRequiresAndStoresApplicantDetails() throws Exception {
        postAs("/api/clubs/"+club.id+"/applications",Map.of("reason","加入训练"),student.username).andExpect(status().isBadRequest());
        postAs("/api/clubs/"+club.id+"/applications",Map.of("reason","加入训练","contact","QQ：12345678","grade","2025级","college","信息工程学院"),student.username).andExpect(status().isCreated()).andExpect(jsonPath("$.contact").value("QQ：12345678")).andExpect(jsonPath("$.grade").value("2025级")).andExpect(jsonPath("$.college").value("信息工程学院"));
    }
    @Test void archivedClubsDoNotLeakIntoPublicActivitiesOrMembershipCounts() throws Exception {
        var before=json.readTree(mvc.perform(get("/api/public/overview")).andReturn().getResponse().getContentAsString());
        club.active=false;s.clubs().saveAndFlush(club);
        var m=new Membership();m.userId=student.id;m.clubId=club.id;m.status="APPROVED";m.reason="归档边界测试";s.memberships().saveAndFlush(m);
        var publicActivities=json.readTree(mvc.perform(get("/api/activities")).andReturn().getResponse().getContentAsString());
        assertThat(publicActivities.findValuesAsText("title")).doesNotContain(activity.title);
        var after=json.readTree(mvc.perform(get("/api/public/overview")).andReturn().getResponse().getContentAsString());
        assertThat(after.path("memberCount").asLong()).isEqualTo(before.path("memberCount").asLong());
        assertThat(after.path("activityCount").asInt()).isEqualTo(before.path("activityCount").asInt()-1);
    }
    @Test void cyclingMigrationArchivesDemonstrationsWithoutDeletingMembershipHistory() {
        club.name="光影摄影社";s.clubs().saveAndFlush(club);
        var m=new Membership();m.userId=student.id;m.clubId=club.id;m.status="APPROVED";m.reason="保留原始记录";s.memberships().saveAndFlush(m);
        item.assetCode="CH-PH-001";s.equipment().saveAndFlush(item);
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection->{
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.ClassPathResource("db/migration/V2__zhengtu_cycling_identity.sql"));return null;
        });
        assertThat(jdbc.queryForObject("SELECT active FROM clubs WHERE id=?",Boolean.class,club.id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM memberships WHERE id=?",Long.class,m.id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM activities WHERE id=?",String.class,activity.id)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT total_quantity FROM equipment WHERE id=?",Integer.class,item.id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT active FROM equipment WHERE id=?",Boolean.class,item.id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM clubs WHERE name='征途单车俱乐部' AND active=TRUE",Long.class)).isEqualTo(1);
    }
    User student,other,admin;Club club;Activity activity;Equipment item;
    @BeforeEach void setup() {
        student=newUser("test_student","STUDENT");other=newUser("test_other","STUDENT");admin=newUser("test_admin","SUPER_ADMIN");
        club=new Club();club.name="测试社团";club.category="学术科技";club.summary="测试简介";club.description="测试详细介绍";club.icon="code";club.color="#547D6A";club.location="A101";club.contact="test@campus.edu";s.clubs().save(club);
        activity=new Activity();activity.clubId=club.id;activity.title="测试活动";activity.category="学术科技";activity.description="活动介绍";activity.location="A101";activity.startsAt=Instant.now().plusSeconds(600);activity.endsAt=Instant.now().plusSeconds(7800);activity.registrationDeadline=activity.startsAt;activity.capacity=1;activity.checkinCode="TEST2026";s.activities().save(activity);
        item=new Equipment();item.name="测试相机";item.category="摄影器材";item.assetCode="TEST001";item.description="测试器材";item.location="A101";item.totalQuantity=2;item.availableQuantity=2;s.equipment().save(item);
        for(var u:List.of(student,other)){var m=new Membership();m.userId=u.id;m.clubId=members.primaryClub().orElseThrow().id;m.reason="测试准备的俱乐部成员";m.status="APPROVED";m.joinType="CLUB";s.memberships().save(m);}
    }
    User newUser(String username,String role) {var u=new User();u.username=username;u.name=username;u.passwordHash=encoder.encode("Password2026!");u.email=username+"@campus.edu";u.studentNo=username;u.department="测试学院";u.role=role;u.joinType="CLUB";return s.users().save(u);}
    ResultActions postAs(String path,Object body,String username)throws Exception{return mvc.perform(post(path).with(user(username)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));}
    ResultActions putAs(String path,Object body,String username)throws Exception{return mvc.perform(put(path).with(user(username)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));}
    @Test void publicDataDoesNotExposeSecrets()throws Exception {
        mvc.perform(get("/api/activities")).andExpect(status().isOk()).andExpect(jsonPath("$[0].checkinCode").doesNotExist());
        mvc.perform(get("/api/clubs")).andExpect(status().isOk()).andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItem(club.name)));
    }
    @Test void authorizationAndCsrfAreEnforced()throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").with(user(student.username))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user(admin.username))).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
    @Test void registrationEncryptsPasswordAndCannotChooseRole()throws Exception {
        var body=Map.of("username","fresh_user","password","Password2026!","name","新同学","email","fresh@campus.edu","studentNo","20260001","department","计算机学院","role","ADMIN","joinType","CLUB","contact","qa@example.test","grade","2025级");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"));
        var u=s.users().findByUsername("fresh_user").orElseThrow();assertThat(u.passwordHash).isNotEqualTo("Password2026!");assertThat(encoder.matches("Password2026!",u.passwordHash)).isTrue();
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("username","fresh_user","password","Password2026!")))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("新同学"));
    }
    @Test void invalidRegistrationIsRejected()throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"!\",\"password\":\"123\"}")).andExpect(status().isBadRequest());
    }
    @Test void clubApplicationCanBeReviewedAndWithdrawn()throws Exception {
        postAs("/api/clubs/"+club.id+"/applications",Map.of("reason","热爱编程，希望加入","contact","test@example.test","grade","2025级","college","信息工程学院"),student.username).andExpect(status().isCreated());
        postAs("/api/clubs/"+club.id+"/applications",Map.of("reason","重复申请","contact","test@example.test","grade","2025级","college","信息工程学院"),student.username).andExpect(status().isBadRequest());
        var m=s.memberships().findByUserIdAndClubId(student.id,club.id).orElseThrow();
        putAs("/api/admin/applications/"+m.id+"/review",Map.of("status","APPROVED","note","欢迎加入"),admin.username).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        postAs("/api/applications/"+m.id+"/withdraw",Map.of(),other.username).andExpect(status().isForbidden());
        postAs("/api/applications/"+m.id+"/withdraw",Map.of(),student.username).andExpect(status().isOk());assertThat(s.memberships().findById(m.id).orElseThrow().status).isEqualTo("WITHDRAWN");
    }
    @Test void activityCapacityAndDuplicateRegistrationAreEnforced()throws Exception {
        postAs("/api/activities/"+activity.id+"/registrations",Map.of(),student.username).andExpect(status().isCreated());
        postAs("/api/activities/"+activity.id+"/registrations",Map.of(),student.username).andExpect(status().isBadRequest());
        postAs("/api/activities/"+activity.id+"/registrations",Map.of(),other.username).andExpect(status().isBadRequest());
    }
    @Test void checkinRequiresOwnerCodeAndTimeWindow()throws Exception {
        var r=s.register(student,activity.id);
        postAs("/api/registrations/"+r.id+"/checkin",Map.of("code","TEST2026"),other.username).andExpect(status().isForbidden());
        postAs("/api/registrations/"+r.id+"/checkin",Map.of("code","WRONG"),student.username).andExpect(status().isBadRequest());
        postAs("/api/registrations/"+r.id+"/checkin",Map.of("code","TEST2026"),student.username).andExpect(status().isOk()).andExpect(jsonPath("$.checkedInAt").isNotEmpty());
        postAs("/api/registrations/"+r.id+"/checkin",Map.of("code","TEST2026"),student.username).andExpect(status().isBadRequest());
    }
    @Test void earlyCheckinAndLateRegistrationAreRejected()throws Exception {
        activity.startsAt=Instant.now().plusSeconds(7200);activity.endsAt=Instant.now().plusSeconds(14400);activity.registrationDeadline=activity.startsAt;s.activities().save(activity);var r=s.register(student,activity.id);
        postAs("/api/registrations/"+r.id+"/checkin",Map.of("code","TEST2026"),student.username).andExpect(status().isBadRequest());
        activity.registrationDeadline=Instant.now().minusSeconds(1);s.activities().save(activity);
        postAs("/api/activities/"+activity.id+"/registrations",Map.of(),other.username).andExpect(status().isBadRequest());
    }
    @Test void loanApprovalAndReturnRestoreStockExactlyOnce()throws Exception {
        var l=s.applyLoan(student,new Requests.LoanInput(item.id,2,"测试用途",LocalDate.now().plusDays(2)));
        putAs("/api/admin/loans/"+l.id+"/review",Map.of("status","APPROVED"),admin.username).andExpect(status().isOk());assertThat(s.equipment().findById(item.id).orElseThrow().availableQuantity).isZero();
        postAs("/api/loans/"+l.id+"/return",Map.of(),student.username).andExpect(status().isOk());
        putAs("/api/admin/loans/"+l.id+"/review",Map.of("status","RETURNED"),admin.username).andExpect(status().isOk());
        putAs("/api/admin/loans/"+l.id+"/review",Map.of("status","RETURNED"),admin.username).andExpect(status().isBadRequest());assertThat(s.equipment().findById(item.id).orElseThrow().availableQuantity).isEqualTo(2);
    }
    @Test void pendingLoansDoNotReserveAndApprovalCannotOversell()throws Exception {
        var first=s.applyLoan(student,new Requests.LoanInput(item.id,2,"测试用途",LocalDate.now().plusDays(1)));var second=s.applyLoan(other,new Requests.LoanInput(item.id,2,"测试用途",LocalDate.now().plusDays(1)));
        s.reviewLoan(first.id,new Requests.Review("APPROVED",""));
        putAs("/api/admin/loans/"+second.id+"/review",Map.of("status","APPROVED"),admin.username).andExpect(status().isBadRequest());
        assertThat(s.equipment().findById(item.id).orElseThrow().availableQuantity).isZero();
    }
    @Test void stockCannotShrinkBelowOutstandingLoans()throws Exception {
        var l=s.applyLoan(student,new Requests.LoanInput(item.id,2,"测试用途",LocalDate.now().plusDays(1)));s.reviewLoan(l.id,new Requests.Review("APPROVED",""));
        assertThatThrownBy(()->s.saveEquipment(item.id,new Requests.EquipmentInput(item.name,item.category,item.assetCode,item.description,item.location,1,true))).isInstanceOf(ApiException.class);
    }
    @Test void repairMaintainsOrderedProgressAndValidTransitions()throws Exception {
        var t=s.repair(student,new Requests.RepairInput(item.id,"无法开机","按开关后没有反应","A101","HIGH"));
        postAs("/api/admin/repairs/"+t.id+"/progress",Map.of("status","CLOSED","note","非法跳转"),admin.username).andExpect(status().isBadRequest());
        postAs("/api/admin/repairs/"+t.id+"/progress",Map.of("status","PROCESSING","note","已安排检修"),admin.username).andExpect(status().isOk());
        postAs("/api/admin/repairs/"+t.id+"/progress",Map.of("status","RESOLVED","note","已更换电池"),admin.username).andExpect(status().isOk());
        postAs("/api/admin/repairs/"+t.id+"/progress",Map.of("status","CLOSED","note","维修完成"),admin.username).andExpect(status().isOk()).andExpect(jsonPath("$.updates.length()").value(4));
        mvc.perform(get("/api/me/dashboard").with(user(other.username))).andExpect(status().isOk()).andExpect(jsonPath("$.repairs.length()").value(0));
    }
    @Test void disabledUserImmediatelyLosesAccess()throws Exception {
        putAs("/api/admin/users/"+student.id+"/access",Map.of("role","STUDENT","enabled",false),admin.username).andExpect(status().isOk());
        mvc.perform(get("/api/me/dashboard").with(user(student.username))).andExpect(status().isUnauthorized());
        putAs("/api/admin/users/"+admin.id+"/access",Map.of("role","STUDENT","enabled",true),admin.username).andExpect(status().isOk());
    }
    @Test void reportsExportChineseCsvAndProtectFormulaCells()throws Exception {
        var result=mvc.perform(get("/api/admin/reports/export?type=equipment").with(user(admin.username))).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=campushub-equipment.csv")).andReturn();
        assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).startsWith("\uFEFF").contains("资产编号","测试相机");
        assertThat(ReportController.cell("=SUM(A1:A2)")).isEqualTo("\"'=SUM(A1:A2)\"");assertThat(ReportController.cell("a\"b")).isEqualTo("\"a\"\"b\"");
        mvc.perform(get("/api/admin/reports").with(user(admin.username))).andExpect(status().isOk()).andExpect(jsonPath("$.trend.length()").value(6));
    }
    @Test void profileAndPasswordCanBeUpdated()throws Exception {
        putAs("/api/me/profile",Map.of("name","新名字","email","new@campus.edu","department","新院系","bio","热爱校园"),student.username).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("新名字"));
        putAs("/api/me/password",Map.of("oldPassword","incorrect","newPassword","NewPassword2026!"),student.username).andExpect(status().isBadRequest());
        putAs("/api/me/password",Map.of("oldPassword","Password2026!","newPassword","NewPassword2026!"),student.username).andExpect(status().isOk());
        assertThat(encoder.matches("NewPassword2026!",s.users().findById(student.id).orElseThrow().passwordHash)).isTrue();
    }
    @Test void cancelledActivitiesCannotBeJoined()throws Exception {
        activity.status="CANCELLED";s.activities().save(activity);
        postAs("/api/activities/"+activity.id+"/registrations",Map.of(),student.username).andExpect(status().isBadRequest());
    }
}
