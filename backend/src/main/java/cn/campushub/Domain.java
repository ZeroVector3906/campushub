package cn.campushub;

import jakarta.persistence.*;
import java.time.*;

/** Entities are never serialized directly. ApiViews controls the public contract. */
public final class Domain {
    private Domain() {}
    @MappedSuperclass
    public abstract static class Base {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(nullable = false) public Instant createdAt = Instant.now();
    }
    @Entity(name = "AppUser") @Table(name = "app_users")
    public static class User extends Base {
        public String username;
        public String passwordHash;
        public String name;
        public String email;
        public String studentNo;
        public String department;
        public String bio = "";
        public String role = "STUDENT";
        public boolean enabled = true;
    }
    @Entity(name = "Club") @Table(name = "clubs")
    public static class Club extends Base {
        public String name;
        public String category;
        public String summary;
        @Column(columnDefinition = "text") public String description;
        public String icon;
        public String color;
        public String location;
        public String contact;
        public boolean recruiting = true;
        public boolean active = true;
    }
    @Entity(name = "Membership") @Table(name = "memberships")
    public static class Membership extends Base {
        public Long userId;
        public Long clubId;
        public String reason;
        public String status = "PENDING";
        public String reviewNote = "";
        public Instant reviewedAt;
    }
    @Entity(name = "Activity") @Table(name = "activities")
    public static class Activity extends Base {
        public Long clubId;
        public String title;
        public String category;
        @Column(columnDefinition = "text") public String description;
        public String location;
        public Instant startsAt;
        public Instant endsAt;
        public Instant registrationDeadline;
        public int capacity;
        public String checkinCode;
        public boolean featured;
        public String status = "PUBLISHED";
    }
    @Entity(name = "Registration") @Table(name = "registrations")
    public static class Registration extends Base {
        public Long userId;
        public Long activityId;
        public Instant checkedInAt;
    }
    @Entity(name = "Equipment") @Table(name = "equipment")
    public static class Equipment extends Base {
        public String name;
        public String category;
        public String assetCode;
        public String description;
        public String location;
        public int totalQuantity;
        public int availableQuantity;
        public boolean active = true;
    }
    @Entity(name = "Loan") @Table(name = "loans")
    public static class Loan extends Base {
        public Long userId;
        public Long equipmentId;
        public int quantity;
        public String purpose;
        public LocalDate dueDate;
        public String status = "PENDING";
        public String reviewNote = "";
        public Instant returnedAt;
    }
    @Entity(name = "RepairTicket") @Table(name = "repair_tickets")
    public static class RepairTicket extends Base {
        public Long userId;
        public Long equipmentId;
        public String title;
        public String description;
        public String location;
        public String priority = "NORMAL";
        public String status = "OPEN";
        public Instant updatedAt = Instant.now();
    }
    @Entity(name = "RepairUpdate") @Table(name = "repair_updates")
    public static class RepairUpdate extends Base {
        public Long ticketId;
        public String operatorName;
        public String status;
        public String note;
    }
}
