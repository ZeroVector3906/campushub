package cn.campushub;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Real TCP requests, cookie sessions and CSRF; PostgreSQL transactions commit normally. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:54329/campushub_test}","campushub.seed-demo=false","campushub.admin-password=TestAdminPassword2026!"})
class HttpWorkflowTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired FindByIndexNameSessionRepository<? extends Session> sessions;
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf="";
        HttpResponse<String> raw(String path,String method,Object body)throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(!method.equals("GET")) request.header("X-CSRF-TOKEN",csrf);
            if(body!=null) request.header("Content-Type","application/json");
            return http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        JsonNode call(String path,String method,Object body,int expected)throws Exception {
            if(!method.equals("GET")){var token=raw("/api/auth/csrf","GET",null);assertThat(token.statusCode()).isEqualTo(200);csrf=json.readTree(token.body()).path("token").asText();}
            var response=raw(path,method,body);assertThat(response.statusCode()).as(method+" "+path+" "+response.body()).isEqualTo(expected);
            return response.body().isBlank()?json.nullNode():json.readTree(response.body());
        }
        void login(String username,String password)throws Exception {call("/api/auth/login","POST",Map.of("username",username,"password",password),200);}
    }
    @Test void completeWorkflowWithConcurrentCapacityAndSessionRevocation()throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8),firstName="http_a_"+suffix,secondName="http_b_"+suffix;
        var admin=new Client();var first=new Client();var second=new Client();var secondSession=new Client();
        Long clubId=null,activityId=null,equipmentId=null;
        try {
            admin.login("admin","TestAdminPassword2026!");
            for(var name:List.of(firstName,secondName)) first.call("/api/auth/register","POST",Map.of("username",name,"password","WorkflowPassword2026!","name","验收同学","studentNo",name,"email",name+"@test.local","department","测试学院","joinType","CLUB","contact","qa@example.test","grade","2025级"),201);
            first.login(firstName,"WorkflowPassword2026!");second.login(secondName,"WorkflowPassword2026!");secondSession.login(firstName,"WorkflowPassword2026!");
            var primary=first.call("/api/me/dashboard","GET",null,200).path("memberships").get(0);
            admin.call("/api/admin/applications/"+primary.path("id").asLong()+"/review","PUT",Map.of("status","APPROVED","note","测试身份确认"),200);
            var club=admin.call("/api/admin/clubs","POST",Map.of("name","HTTP 验收社团 "+suffix,"category","学术科技","summary","真实接口验收","description","完整业务流程验收","icon","code","color","#547D6A","location","测试楼101","contact","qa@test.local","recruiting",true,"active",true),201);clubId=club.path("id").asLong();
            var application=first.call("/api/clubs/"+clubId+"/applications","POST",Map.of("reason","希望参加项目共创","contact","test@example.test","grade","2025级","college","信息工程学院"),201);
            admin.call("/api/admin/applications/"+application.path("id").asLong()+"/review","PUT",Map.of("status","APPROVED","note","欢迎加入"),200);
            assertThat(first.call("/api/me/dashboard","GET",null,200).path("memberships").get(0).path("status").asText()).isEqualTo("APPROVED");
            var activityStartsAt=Instant.now().plusSeconds(300);
            var activity=admin.call("/api/admin/activities","POST",Map.ofEntries(Map.entry("clubId",clubId),Map.entry("title","并发报名验收"),Map.entry("category","学术科技"),Map.entry("description","仅一个名额"),Map.entry("location","测试楼101"),Map.entry("startsAt",activityStartsAt.toString()),Map.entry("endsAt",Instant.now().plusSeconds(7200).toString()),Map.entry("registrationDeadline",activityStartsAt.toString()),Map.entry("capacity",1),Map.entry("checkinCode","HTTP2026"),Map.entry("featured",false),Map.entry("status","PUBLISHED")),201);activityId=activity.path("id").asLong();
            String registrationPath="/api/activities/"+activityId+"/registrations";
            first.csrf=first.call("/api/auth/csrf","GET",null,200).path("token").asText();second.csrf=second.call("/api/auth/csrf","GET",null,200).path("token").asText();
            try(var executor=Executors.newFixedThreadPool(2)) {
                var a=executor.submit(()->first.raw(registrationPath,"POST",Map.of()));var b=executor.submit(()->second.raw(registrationPath,"POST",Map.of()));
                var ar=a.get(20,TimeUnit.SECONDS);var br=b.get(20,TimeUnit.SECONDS);
                assertThat(List.of(ar.statusCode(),br.statusCode())).containsExactlyInAnyOrder(201,400);
                var winner=ar.statusCode()==201?first:second;var loser=ar.statusCode()==201?second:first;long registrationId=json.readTree(ar.statusCode()==201?ar.body():br.body()).path("id").asLong();
                loser.call("/api/registrations/"+registrationId+"/checkin","POST",Map.of("code","HTTP2026"),403);
                winner.call("/api/registrations/"+registrationId+"/checkin","POST",Map.of("code","WRONG"),400);
                winner.call("/api/registrations/"+registrationId+"/checkin","POST",Map.of("code","HTTP2026"),200);
            }
            var equip=admin.call("/api/admin/equipment","POST",Map.of("name","HTTP 测试相机","assetCode","HTTP-"+suffix,"category","摄影器材","description","接口测试器材","location","测试楼101","totalQuantity",2,"active",true),201);equipmentId=equip.path("id").asLong();
            var loan=first.call("/api/loans","POST",Map.of("equipmentId",equipmentId,"quantity",2,"purpose","验收测试","dueDate",LocalDate.now().plusDays(1).toString()),201);long loanId=loan.path("id").asLong();
            admin.call("/api/admin/loans/"+loanId+"/review","PUT",Map.of("status","APPROVED","note","已发放"),200);
            assertThat(jdbc.queryForObject("SELECT available_quantity FROM equipment WHERE id=?",Integer.class,equipmentId)).isZero();
            first.call("/api/loans/"+loanId+"/return","POST",null,200);admin.call("/api/admin/loans/"+loanId+"/review","PUT",Map.of("status","RETURNED","note","配件齐全"),200);
            admin.call("/api/admin/loans/"+loanId+"/review","PUT",Map.of("status","RETURNED"),400);
            assertThat(jdbc.queryForObject("SELECT available_quantity FROM equipment WHERE id=?",Integer.class,equipmentId)).isEqualTo(2);
            var repair=first.call("/api/repairs","POST",Map.of("equipmentId",equipmentId,"title","接口验收报修","description","无法开机","location","测试楼101","priority","HIGH"),201);long repairId=repair.path("id").asLong();
            for(var status:List.of("PROCESSING","RESOLVED","CLOSED"))admin.call("/api/admin/repairs/"+repairId+"/progress","POST",Map.of("status",status,"note","已完成处理步骤 "+status),200);
            var csv=admin.raw("/api/admin/reports/export?type=equipment","GET",null);assertThat(csv.statusCode()).isEqualTo(200);assertThat(csv.body()).contains("HTTP 测试相机").startsWith("\uFEFF");
            first.call("/api/me/profile","PUT",Map.of("name","接口验收完成","email",firstName+"@test.local","department","测试学院","bio","已跑通完整流程"),200);
            first.call("/api/me/password","PUT",Map.of("oldPassword","WorkflowPassword2026!","newPassword","ChangedPassword2026!"),200);
            first.call("/api/auth/me","GET",null,401);secondSession.call("/api/auth/me","GET",null,401);
            first.login(firstName,"ChangedPassword2026!");first.call("/api/auth/logout","POST",null,200);first.call("/api/auth/me","GET",null,401);
            second.call("/api/admin/users","GET",null,403);
            var root=admin.raw("/","GET",null);assertThat(root.statusCode()).isEqualTo(200);assertThat(root.body()).contains("西安欧亚学院征途单车俱乐部");assertThat(admin.raw("/images/cycling.svg","GET",null).statusCode()).isEqualTo(200);
        } finally {
            // Delete only fixtures created by this test; business/demo database is never used.
            for(var name:List.of(firstName,secondName)) {
                sessions.findByPrincipalName(name).keySet().forEach(sessions::deleteById);
                jdbc.update("DELETE FROM repair_updates WHERE ticket_id IN (SELECT id FROM repair_tickets WHERE user_id IN (SELECT id FROM app_users WHERE username=?))",name);
                for(var table:List.of("repair_tickets","loans","registrations","memberships")) jdbc.update("DELETE FROM "+table+" WHERE user_id IN (SELECT id FROM app_users WHERE username=?)",name);
                jdbc.update("DELETE FROM app_users WHERE username=?",name);
            }
            if(activityId!=null)jdbc.update("DELETE FROM activities WHERE id=?",activityId);
            if(equipmentId!=null)jdbc.update("DELETE FROM equipment WHERE id=?",equipmentId);
            if(clubId!=null)jdbc.update("DELETE FROM clubs WHERE id=?",clubId);
        }
    }
}
