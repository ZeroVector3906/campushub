package cn.campushub;

import cn.campushub.Domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Component
public class SeedData implements ApplicationRunner {
    final CampusService s; final PasswordEncoder encoder;
    @Value("${campushub.seed-demo}") boolean demo;
    @Value("${campushub.admin-username}") String adminUsername;
    @Value("${campushub.admin-password}") String adminPassword;
    public SeedData(CampusService s,PasswordEncoder encoder) { this.s=s;this.encoder=encoder; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!s.users().existsByUsername(adminUsername)) {
            String password=adminPassword.isBlank() && demo?"CampusHub@2026":adminPassword;
            if(password.length()<12) throw new IllegalStateException("首次启动请设置至少 12 位 ADMIN_PASSWORD，或仅在本地设置 SEED_DEMO=true");
            AuthController.checkPassword(password);
            var admin=user(adminUsername,"校园管理员","admin@campushub.local","ADMIN001","学生社团管理中心",encoder.encode(password));admin.role="ADMIN";s.users().save(admin);
        }
        if(!demo || s.clubs().count()>0) return;
        String hash=encoder.encode("CampusHub@2026");
        String[] names={"林同学","陈沐阳","王诗涵","张子墨","李雨桐","刘一诺","赵星辰","周予安","吴清禾","徐知夏","孙景行","马思远","朱若溪","胡嘉宁","郭晨曦","何沐风"};
        var students=new ArrayList<User>();
        for(int i=0;i<names.length;i++) {
            String username=i==0?"student":"student"+(i+1);
            var u=user(username,names[i],username+"@campus.edu.cn","2026"+String.format("%06d",i+1),new String[]{"计算机与人工智能学院","艺术与设计学院","经济管理学院","人文学院"}[i%4],hash);students.add(s.users().save(u));
        }
        String[][] data={
            {"光影摄影社","文化艺术","用镜头收藏校园的每一束光。","从第一次按下快门，到策划属于自己的摄影展。在这里，我们一起城市漫游、学习构图与后期，用影像记录大学时光。每周三晚开放摄影分享，欢迎零基础同学加入。","camera","#547D6A","艺术楼 B203","photo@campus.edu.cn"},
            {"山野户外社","体育运动","走进山野，也遇见更勇敢的自己。","周末徒步、露营与定向越野。我们坚持安全同行和无痕山野，出发前开展路线说明与装备检查，让每一次探索都有准备。","mountain","#8C8960","体育馆 102","outdoor@campus.edu.cn"},
            {"回声音乐社","文化艺术","让热爱有回声，让校园有旋律。","不论你会弹吉他、热爱歌唱，还是喜欢安静听一场现场，我们都欢迎你。社团设有声乐、器乐和制作小组，定期举办草坪音乐会与开放麦。","music","#BA8067","大学生活动中心 301","music@campus.edu.cn"},
            {"代码与创意协会","学术科技","把脑海中的灵感，写成真实的作品。","面向所有对科技有兴趣的同学，开展编程入门、开源共创、创意硬件与项目交流。每月一个小项目，从零开始也能找到搭档。","code","#6B7E9F","信息楼 A405","code@campus.edu.cn"},
            {"青禾志愿者协会","公益实践","让微小的善意，在校园生根。","开展校园环保、社区陪伴与公益支教。每一次志愿服务都有培训、组织和记录，让热心找到合适的方向。","heart","#A07C80","学生活动中心 105","volunteer@campus.edu.cn"},
            {"风行羽毛球社","体育运动","挥拍之间，找到你的运动搭子。","每周固定训练与自由约球，提供新手教学、双打配合和校内友谊赛。运动水平各不相同，热爱运动的心始终一样。","racket","#6D9292","体育馆 203","badminton@campus.edu.cn"},
            {"知行读书会","文化艺术","在一本书里，看见更辽阔的世界。","每月共读一本书，周末开展轻松的阅读圆桌。文学、社会、科学都可以成为话题，在倾听中建立自己的思考。","book","#967B63","图书馆 4F 研讨室","reading@campus.edu.cn"},
            {"创新创业联盟","学术科技","让好点子，向前再走一步。","从需求访谈、产品原型到项目路演，为校园创意提供交流空间。邀请校友分享实践经验，鼓励跨专业团队合作。","sparkles","#8A7BA4","创新中心 201","innovation@campus.edu.cn"}
        };
        var clubList=new ArrayList<Club>();
        for(String[] d:data) {var c=new Club();c.name=d[0];c.category=d[1];c.summary=d[2];c.description=d[3];c.icon=d[4];c.color=d[5];c.location=d[6];c.contact=d[7];clubList.add(s.clubs().save(c));}
        for(int i=0;i<students.size();i++) for(int j=0;j<clubList.size();j++) {
            if((i+j)%3==0) {var m=new Membership();m.userId=students.get(i).id;m.clubId=clubList.get(j).id;m.reason="希望和大家一起探索兴趣，在校园留下有意义的经历。";m.status="APPROVED";m.reviewedAt=Instant.now().minusSeconds(86400);s.memberships().save(m);}
        }
        var pending=new Membership();pending.userId=students.get(0).id;pending.clubId=clubList.get(1).id;pending.reason="喜欢徒步和自然，希望参与周末的户外活动。";s.memberships().save(pending);
        var now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        var music=activity(clubList.get(2),"秋日草坪音乐会","文化艺术","把忙碌暂时放一边，让晚风和音乐接管这个傍晚。校园乐队、原创歌曲与开放麦，期待在草坪上见到你。活动免费，请自备野餐垫，保持场地整洁。","图书馆南侧大草坪",now.plusDays(5).withHour(18).withMinute(30).withSecond(0),120,true);
        activity(clubList.get(0),"校园光影 · 秋日摄影漫步","文化艺术","带上手机或相机，从梧桐大道出发，寻找属于校园的秋日色彩。由学长学姐带队，现场交流构图与自然光拍摄技巧。","梧桐大道集合点",now.plusDays(2).withHour(15).withMinute(0),30,false);
        activity(clubList.get(1),"周末出走计划：青山轻徒步","体育运动","一次适合新手的轻量徒步。全程约 6 公里，请穿运动鞋、自备饮用水。出发前开展安全说明，天气不佳将另行通知调整。","学校东门",now.plusDays(4).withHour(8).withMinute(30),40,false);
        activity(clubList.get(3),"从 0 到 1：创意编程工作坊","学术科技","通过一个小项目认识网页开发。请携带笔记本电脑，活动现场有助教协助配置环境，无需编程经验。","信息楼 A405",now.plusDays(7).withHour(14).withMinute(0),45,false);
        activity(clubList.get(4),"青禾行动 · 校园环保日","公益实践","从身边的小事开始，参与垃圾分类宣传与校园清洁。完成活动并签到后可在成员中心查看参与记录。","学生中心广场",now.plusDays(9).withHour(9).withMinute(0),60,false);
        var live=activity(clubList.get(6),"午后共读：把生活写成诗","文化艺术","正在进行的阅读交流。带一本喜欢的书，与大家分享打动你的片段。演示签到码为 READ2026。","图书馆 4F 研讨室",now.plusMinutes(10),25,false);
        var past=activity(clubList.get(5),"新生杯羽毛球友谊赛","体育运动","以球会友，开启新学期。已结束的活动保留报名与签到记录。","体育馆羽毛球场",now.minusDays(12).withHour(14),48,false);
        for(int i=0;i<12;i++) {
            var r=new Registration();r.userId=students.get(i).id;r.activityId=music.id;s.registrations().save(r);
            if(i<8) {var history=new Registration();history.userId=students.get(i).id;history.activityId=past.id;history.checkedInAt=past.startsAt.plusSeconds(120);history.createdAt=past.startsAt.minusSeconds(86400);s.registrations().save(history);}
        }
        var liveReg=new Registration();liveReg.userId=students.get(0).id;liveReg.activityId=live.id;s.registrations().save(liveReg);
        String[][] equip={{"索尼 A6400 微单套装","摄影器材","CH-PH-001","机身、16–50mm 镜头、电池与收纳包；借用时请现场核对配件。","艺术楼 B203","6"},
            {"户外双人帐篷","户外装备","CH-OD-002","双层防雨帐篷，含地钉、支架与防潮垫，使用后请清洁晾干。","体育馆器材室","12"},
            {"便携式蓝牙音箱","活动设备","CH-AV-003","适合小型校园活动，附充电线；请注意控制音量。","学生中心 108","8"},
            {"无线手持麦克风","活动设备","CH-AV-004","一拖二无线话筒，需配合音箱使用。","学生中心 108","10"},
            {"羽毛球拍训练套装","运动器材","CH-SP-005","双拍与球筒，适合日常训练与友谊赛。","体育馆 203","20"},
            {"便携投影仪","活动设备","CH-AV-006","含 HDMI 线、遥控器与电源适配器。","信息楼 A405","4"}};
        var equipList=new ArrayList<Equipment>();for(var d:equip) {var e=new Equipment();e.name=d[0];e.category=d[1];e.assetCode=d[2];e.description=d[3];e.location=d[4];e.totalQuantity=Integer.parseInt(d[5]);e.availableQuantity=e.totalQuantity;equipList.add(s.equipment().save(e));}
        var loan=new Loan();loan.userId=students.get(0).id;loan.equipmentId=equipList.get(0).id;loan.quantity=1;loan.purpose="参加校园光影摄影活动";loan.dueDate=LocalDate.now().plusDays(7);s.loans().save(loan);
        s.repair(students.get(0),new Requests.RepairInput(equipList.get(5).id,"投影仪连接后画面闪烁","更换 HDMI 线后仍会偶尔闪烁，请协助检查接口。","信息楼 A405","NORMAL"));
    }
    User user(String username,String name,String email,String studentNo,String department,String hash) {
        var u=new User();u.username=username;u.name=name;u.email=email;u.studentNo=studentNo;u.department=department;u.passwordHash=hash;return u;
    }
    Activity activity(Club club,String title,String category,String description,String location,ZonedDateTime start,int capacity,boolean featured) {
        var a=new Activity();a.clubId=club.id;a.title=title;a.category=category;a.description=description;a.location=location;a.startsAt=start.toInstant();a.endsAt=start.plusHours(2).toInstant();a.registrationDeadline=start.toInstant();a.capacity=capacity;a.featured=featured;a.checkinCode=title.startsWith("午后")?"READ2026":"CH2026";return s.activities().save(a);
    }
}
