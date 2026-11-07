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
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:54329/campushub_test}","campushub.seed-demo=false","campushub.admin-password=TestAdminPassword2026!"})
@AutoConfigureMockMvc @Transactional
class MemberPolicyTest {
    @Autowired MockMvc mvc;@Autowired CampusService s;@Autowired MemberPolicy policy;@Autowired ObjectMapper json;@Autowired PasswordEncoder encoder;@Autowired JdbcTemplate jdbc;@Autowired jakarta.persistence.EntityManager entityManager;
    User member,admin;Equipment item;
    @BeforeEach void setup(){
        member=createUser("member_policy_student","STUDENT");admin=createUser("member_policy_admin","SUPER_ADMIN");
        item=new Equipment();item.name="身份权限测试器材";item.assetCode="POLICY-001";item.category="骑行装备";item.description="仅用于测试";item.location="测试地点";item.totalQuantity=2;item.availableQuantity=2;s.equipment().save(item);
    }
    User createUser(String name,String role){var u=new User();u.username=name;u.email=name+"@example.test";u.studentNo=name;u.name="测试成员";u.department="测试学院";u.passwordHash=encoder.encode("PolicyPassword2026!");u.role=role;return s.users().save(u);}
    ResultActions postAs(String path,Object body,User u)throws Exception{return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).with(user(u.username).roles(u.role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));}
    ResultActions putAs(String path,Object body,User u)throws Exception{return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).with(user(u.username).roles(u.role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));}
    Map<String,Object> edit(String type,String status){return new HashMap<>(Map.of("name","已编辑成员","email",member.email,"studentNo","20251234","department","信息工程学院","contact","微信：member-test","grade","2025级","joinType",type,"clubDivision",type.equals("CLUB")?"宣传部":"","memberStatus",status,"note","管理员确认"));}
    void approve(String type)throws Exception{putAs("/api/admin/users/"+member.id+"/profile",edit(type,"APPROVED"),admin).andExpect(status().isOk());}
    Map<String,Object> room(String place,String usage){return new HashMap<>(Map.of("kind","ROOM","title","场地权限测试","description","验证身份限制","contact","qa@example.test","room",place,"usage",usage,"startsAt",Instant.now().plusSeconds(3600).toString(),"endsAt",Instant.now().plusSeconds(7200).toString()));}

    @Test void onlySuperAdminCanManageAdministratorsAndAccounts()throws Exception{
        var normal=createUser("normal_admin","ADMIN");
        putAs("/api/admin/users/"+member.id+"/access",Map.of("role","ADMIN","enabled",true),normal).andExpect(status().isForbidden());
        putAs("/api/admin/users/"+admin.id+"/profile",edit("CLUB","APPROVED"),normal).andExpect(status().isForbidden());
        putAs("/api/admin/users/"+normal.id+"/account",Map.of("username","updated_admin","bio","更新简介","newPassword","ChangedPassword2026!"),normal).andExpect(status().isForbidden());
        putAs("/api/admin/users/"+normal.id+"/account",Map.of("username","updated_admin","bio","更新简介","newPassword","ChangedPassword2026!"),admin).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("updated_admin"));
        assertThat(encoder.matches("ChangedPassword2026!",s.users().findById(normal.id).orElseThrow().passwordHash)).isTrue();
        putAs("/api/admin/users/"+member.id+"/access",Map.of("role","ADMIN","enabled",true),admin).andExpect(status().isOk());
    }
    @Test void lastEnabledSuperAdministratorCannotBeDisabledOrDemoted()throws Exception{
        jdbc.update("UPDATE app_users SET role='ADMIN' WHERE role='SUPER_ADMIN' AND id<>?",admin.id);entityManager.clear();
        putAs("/api/admin/users/"+admin.id+"/access",Map.of("role","ADMIN","enabled",true),admin).andExpect(status().isBadRequest());
        putAs("/api/admin/users/"+admin.id+"/access",Map.of("role","SUPER_ADMIN","enabled",false),admin).andExpect(status().isBadRequest());
    }
    @Test void registrationChoicesCreatePendingApplicationsWithoutGrantingRoomPrivileges()throws Exception{
        for(var type:List.of("CLUB","SOCIETY")){
            var body=new HashMap<>(Map.of("username","join_"+type.toLowerCase(),"password","PolicyPassword2026!","name","申请同学","email",type+"@example.test","studentNo","TEST"+type,"department","测试学院","joinType",type,"contact","qa@example.test","grade","2025级","clubDivision",type.equals("CLUB")?"课程部":""));
            body.put("role","ADMIN");body.put("memberStatus","APPROVED");
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT")).andExpect(jsonPath("$.joinType").value(type)).andExpect(jsonPath("$.memberStatus").value("PENDING")).andExpect(jsonPath("$.canBorrowEquipment").value(true)).andExpect(jsonPath("$.allowedRooms").isEmpty()).andReturn();
            var created=s.users().findById(json.readTree(response.getResponse().getContentAsString()).path("id").asLong()).orElseThrow();
            assertThat(policy.membership(created.id).orElseThrow().joinType).isEqualTo(type);
        }
    }
    @Test void societyMembersCanOnlyStoreOrCollectBicyclesInNorthRoom()throws Exception{
        approve("SOCIETY");
        for(var usage:List.of("存车","放车","存放车辆")){
            var result=postAs("/api/riding-requests",room(MemberPolicy.NORTH,usage),member).andExpect(status().isCreated()).andExpect(jsonPath("$.usage").value("存取车辆")).andReturn();
            long id=json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
            jdbc.update("UPDATE riding_requests SET usage=? WHERE id=?",usage,id);entityManager.clear();
            putAs("/api/admin/riding-requests/"+id+"/review",Map.of("status","APPROVED"),admin).andExpect(status().isOk()).andExpect(jsonPath("$.usage").value("存取车辆"));
        }
        mvc.perform(get("/api/me/dashboard").with(user(member.username))).andExpect(status().isOk()).andExpect(jsonPath("$.member.allowedUsages[0]").value("存取车辆"));
        postAs("/api/riding-requests",room(MemberPolicy.NORTH,"拿车"),member).andExpect(status().isCreated());
        for(var usage:List.of("训练","维修","休息"))postAs("/api/riding-requests",room(MemberPolicy.NORTH,usage),member).andExpect(status().isForbidden());
        var other=room(MemberPolicy.NORTH,"其他");other.put("otherUsage","室内活动");postAs("/api/riding-requests",other,member).andExpect(status().isForbidden());
        postAs("/api/riding-requests",room(MemberPolicy.HEGUANG,"存车"),member).andExpect(status().isForbidden());
        postAs("/api/loans",Map.of("equipmentId",item.id,"quantity",1,"purpose","尝试借用","dueDate",LocalDate.now().plusDays(1).toString()),member).andExpect(status().isCreated());
    }
    @Test void changingMembershipUpdatesAccessButPreservesPasswordRoleHistoryAndReturns()throws Exception{
        var originalHash=member.passwordHash;approve("CLUB");
        postAs("/api/riding-requests",room(MemberPolicy.HEGUANG,"维修"),member).andExpect(status().isCreated());
        var r=postAs("/api/loans",Map.of("equipmentId",item.id,"quantity",1,"purpose","测试借用","dueDate",LocalDate.now().plusDays(1).toString()),member).andExpect(status().isCreated()).andReturn();
        long loanId=json.readTree(r.getResponse().getContentAsString()).path("id").asLong();
        putAs("/api/admin/loans/"+loanId+"/review",Map.of("status","APPROVED"),admin).andExpect(status().isOk());
        approve("SOCIETY");
        postAs("/api/loans",Map.of("equipmentId",item.id,"quantity",1,"purpose","再次申请","dueDate",LocalDate.now().plusDays(1).toString()),member).andExpect(status().isCreated());
        postAs("/api/loans/"+loanId+"/return",Map.of(),member).andExpect(status().isOk());
        putAs("/api/admin/loans/"+loanId+"/review",Map.of("status","RETURNED"),admin).andExpect(status().isOk());
        assertThat(member.passwordHash).isEqualTo(originalHash);assertThat(member.role).isEqualTo("STUDENT");assertThat(member.enabled).isTrue();
        assertThat(s.loans().findById(loanId)).isPresent();assertThat(policy.membership(member.id).orElseThrow().clubDivision).isEmpty();
        mvc.perform(get("/api/me/dashboard").with(user(member.username))).andExpect(status().isOk()).andExpect(jsonPath("$.member.memberType").value("SOCIETY")).andExpect(jsonPath("$.member.canBorrowEquipment").value(true));
    }
    @Test void unclassifiedOrPendingUsersCannotGainRightsOrEditOtherMembers()throws Exception{
        putAs("/api/admin/users/"+member.id+"/profile",edit("CLUB","APPROVED"),member).andExpect(status().isForbidden());
        postAs("/api/riding-requests",room(MemberPolicy.NORTH,"存车"),member).andExpect(status().isForbidden());
        putAs("/api/admin/users/"+member.id+"/profile",edit("CLUB","PENDING"),admin).andExpect(status().isOk());
        postAs("/api/riding-requests",room(MemberPolicy.HEGUANG,"训练"),member).andExpect(status().isForbidden());
        var invalid=edit("UNASSIGNED","APPROVED");putAs("/api/admin/users/"+member.id+"/profile",invalid,admin).andExpect(status().isBadRequest());
        var invalidDivision=edit("SOCIETY","APPROVED");invalidDivision.put("clubDivision","宣传部");putAs("/api/admin/users/"+member.id+"/profile",invalidDivision,admin).andExpect(status().isBadRequest());
    }
    @Test void pendingRoomApprovalRechecksCurrentMemberRestrictions()throws Exception{
        approve("CLUB");var result=postAs("/api/riding-requests",room(MemberPolicy.HEGUANG,"休息"),member).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).path("id").asLong();approve("SOCIETY");
        putAs("/api/admin/riding-requests/"+id+"/review",Map.of("status","APPROVED"),admin).andExpect(status().isForbidden());
        putAs("/api/admin/riding-requests/"+id+"/review",Map.of("status","REJECTED","note","当前身份不符"),admin).andExpect(status().isOk());
    }
    @Test void migrationPreservesLegacyDataWithoutInventingMemberIdentities(){
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection->{
            String schema="migration_"+UUID.randomUUID().toString().replace("-","");
            try(var st=connection.createStatement()){
                st.execute("CREATE SCHEMA "+schema);st.execute("SET LOCAL search_path TO "+schema);
                st.execute("CREATE TABLE app_users(id BIGINT PRIMARY KEY,password_hash VARCHAR(100),role VARCHAR(20),enabled BOOLEAN)");
                st.execute("CREATE TABLE clubs(id BIGINT PRIMARY KEY,name VARCHAR(80))");
                st.execute("CREATE TABLE memberships(id BIGINT PRIMARY KEY,user_id BIGINT,club_id BIGINT,status VARCHAR(20),contact VARCHAR(120),grade VARCHAR(40))");
                st.execute("INSERT INTO app_users VALUES(1,'preserved-bcrypt-hash','STUDENT',true)");st.execute("INSERT INTO clubs VALUES(1,'征途单车俱乐部')");st.execute("INSERT INTO memberships VALUES(1,1,1,'APPROVED','原联系方式','2024级')");
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.ClassPathResource("db/migration/V6__club_and_society_members.sql"));
                try(var rs=st.executeQuery("SELECT u.password_hash,u.role,u.enabled,u.contact,u.grade,u.join_type,m.join_type,m.status FROM app_users u JOIN memberships m ON u.id=m.user_id")){
                    assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).isEqualTo("preserved-bcrypt-hash");assertThat(rs.getString(2)).isEqualTo("STUDENT");assertThat(rs.getBoolean(3)).isTrue();assertThat(rs.getString(4)).isEqualTo("原联系方式");assertThat(rs.getString(5)).isEqualTo("2024级");assertThat(rs.getString(6)).isEqualTo("UNASSIGNED");assertThat(rs.getString(7)).isEqualTo("UNASSIGNED");assertThat(rs.getString(8)).isEqualTo("APPROVED");
                }
            }finally{try(var st=connection.createStatement()){st.execute("SET LOCAL search_path TO public");}}return null;
        });
    }
}
