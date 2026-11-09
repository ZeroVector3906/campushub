package cn.campushub;

import cn.campushub.Domain.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BatchReadTest {
    final UserRepository users=mock(UserRepository.class);
    final ClubRepository clubs=mock(ClubRepository.class);
    final MembershipRepository memberships=mock(MembershipRepository.class);
    final ActivityRepository activities=mock(ActivityRepository.class);
    final RegistrationRepository registrations=mock(RegistrationRepository.class);
    final EquipmentRepository equipment=mock(EquipmentRepository.class);
    final LoanRepository loans=mock(LoanRepository.class);
    final RepairRepository repairs=mock(RepairRepository.class);
    final RepairUpdateRepository updates=mock(RepairUpdateRepository.class);
    User user(long id,String type){var u=new User();u.id=id;u.username="user"+id;u.name="成员"+id;u.role="STUDENT";u.joinType=type;u.enabled=true;u.passwordHash="must-never-be-exported";return u;}
    Club club(){var c=new Club();c.id=9L;c.name=MemberPolicy.CLUB_NAME;c.active=true;return c;}
    Membership member(User u){var m=new Membership();m.id=u.id;m.userId=u.id;m.clubId=9L;m.status="APPROVED";m.joinType=u.joinType;return m;}
    @Test void membershipListsUseBoundedQueriesAndKeepSafeDtoFields(){
        var people=new ArrayList<User>();var rows=new ArrayList<Membership>();
        for(long id=1;id<=5;id++){var u=user(id,"CLUB");people.add(u);rows.add(member(u));}
        when(users.findAllById(any())).thenReturn(people);when(clubs.findAllById(any())).thenReturn(List.of(club()));
        when(memberships.approvedClubCounts()).thenReturn(Collections.singletonList(new Object[]{9L,5L}));
        var views=new ApiViews(users,clubs,memberships,activities,registrations,equipment,updates);
        var result=views.membershipRows(rows);
        assertThat(result).hasSize(5);
        assertThat(((Map<?,?>)result.get(4).get("user")).get("name")).isEqualTo("成员5");
        assertThat(((Map<?,?>)result.get(0).get("club")).get("memberCount")).isEqualTo(5L);
        assertThat(((Map<?,?>)result.get(0).get("user")).containsKey("passwordHash")).isFalse();
        verify(users,times(1)).findAllById(any());verify(clubs,times(1)).findAllById(any());verify(memberships,times(1)).approvedClubCounts();
        verify(users,never()).findById(any());verify(clubs,never()).findById(any());verify(memberships,never()).countByClubIdAndStatus(any(),any());
    }
    @Test void memberListKeepsSocietyEquipmentRightsAndAdministratorRoomRights(){
        var a=user(1,"CLUB");var b=user(2,"SOCIETY");var admin=user(3,"UNASSIGNED");admin.role="SUPER_ADMIN";
        when(clubs.findAll()).thenReturn(List.of(club()));when(memberships.findByClubId(9L)).thenReturn(List.of(member(a),member(b)));
        var result=new MemberPolicy(users,clubs,memberships).views(List.of(a,b,admin));
        assertThat(result.get(0).get("allowedRooms")).isEqualTo(List.of(MemberPolicy.HEGUANG,MemberPolicy.NORTH));
        assertThat(result.get(1).get("allowedRooms")).isEqualTo(List.of(MemberPolicy.NORTH));
        assertThat(result.get(1).get("allowedUsages")).isEqualTo(List.of("存取车辆"));
        assertThat(result.get(1).get("canBorrowEquipment")).isEqualTo(true);
        assertThat(result.get(2).get("allowedRooms")).isEqualTo(List.of(MemberPolicy.HEGUANG,MemberPolicy.NORTH));
        verify(clubs,times(1)).findAll();verify(memberships,times(1)).findByClubId(9L);verify(memberships,never()).findByUserIdAndClubId(any(),any());
    }
    @Test void equipmentCsvCountsLoansOnceForTheWholeExport(){
        var rows=new ArrayList<Equipment>();
        for(long id=1;id<=3;id++){var e=new Equipment();e.id=id;e.assetCode="ZT"+id;e.name="器材"+id;e.category="骑行";e.location="北三";e.totalQuantity=3;e.availableQuantity=2;rows.add(e);}
        var loan=new Loan();loan.equipmentId=1L;
        when(equipment.findAll()).thenReturn(rows);when(loans.findAll()).thenReturn(List.of(loan));
        var service=new CampusService(users,clubs,memberships,activities,registrations,equipment,loans,repairs,updates);
        var result=new ReportController(service).export("equipment");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(result.getBody(),StandardCharsets.UTF_8)).startsWith("\uFEFF").contains("器材1","器材3");
        verify(loans,times(1)).findAll();verify(equipment,times(1)).findAll();
    }
}
