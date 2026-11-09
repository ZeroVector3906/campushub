package cn.campushub;

import cn.campushub.Domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRoleAndEnabled(String role, boolean enabled);
}
interface ClubRepository extends JpaRepository<Club, Long> {}
interface MembershipRepository extends JpaRepository<Membership, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from Membership m where m.id = :id")
    Optional<Membership> lockById(@Param("id") Long id);
    List<Membership> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Membership> findByUserIdAndClubId(Long userId, Long clubId);
    long countByClubIdAndStatus(Long clubId, String status);
    long countByStatus(String status);
    List<Membership> findByClubId(Long clubId);
    @Query("select m.clubId, count(m) from Membership m where m.status = 'APPROVED' group by m.clubId")
    List<Object[]> approvedClubCounts();
}
interface ActivityRepository extends JpaRepository<Activity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Activity a where a.id = :id")
    Optional<Activity> lockById(@Param("id") Long id);
}
interface RegistrationRepository extends JpaRepository<Registration, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Registration r where r.id = :id")
    Optional<Registration> lockById(@Param("id") Long id);
    List<Registration> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Registration> findByUserIdAndActivityId(Long userId, Long activityId);
    long countByActivityId(Long activityId);
    long countByCheckedInAtIsNotNull();
    @Query("select r.activityId, count(r) from Registration r group by r.activityId")
    List<Object[]> activityCounts();
}
interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> lockById(@Param("id") Long id);
}
interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from Loan l where l.id = :id")
    Optional<Loan> lockById(@Param("id") Long id);
}
interface RepairRepository extends JpaRepository<RepairTicket, Long> {
    List<RepairTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RepairTicket r where r.id = :id")
    Optional<RepairTicket> lockById(@Param("id") Long id);
}
interface RepairUpdateRepository extends JpaRepository<RepairUpdate, Long> {
    List<RepairUpdate> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    List<RepairUpdate> findByTicketIdInOrderByCreatedAtAsc(Collection<Long> ticketIds);
}
