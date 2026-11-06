package cn.campushub;

import jakarta.validation.constraints.*;
import java.time.*;

public final class Requests {
    private Requests() {}
    public record Login(@NotBlank String username, @NotBlank String password) {}
    public record Register(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,32}", message = "用户名须为 3–32 位字母、数字或下划线") String username,
        @NotBlank @Size(min=8,max=64) String password,
        @NotBlank @Size(max=50) String name,
        @NotBlank @Email @Size(max=120) String email,
        @NotBlank @Size(max=30) String studentNo,
        @NotBlank @Size(max=100) String department) {}
    public record Profile(@NotBlank @Size(max=50) String name, @NotBlank @Email @Size(max=120) String email,
        @NotBlank @Size(max=100) String department, @Size(max=500) String bio) {}
    public record Password(@NotBlank String oldPassword, @NotBlank @Size(min=8,max=64) String newPassword) {}
    public record Application(@NotBlank @Size(max=1000) String reason) {}
    public record Review(@NotBlank String status, @Size(max=500) String note) {}
    public record Checkin(@NotBlank @Size(max=32) String code) {}
    public record ClubInput(@NotBlank @Size(max=80) String name, @NotBlank @Size(max=30) String category,
        @NotBlank @Size(max=160) String summary, @NotBlank @Size(max=10000) String description,
        @NotBlank @Size(max=30) String icon, @NotBlank @Pattern(regexp="#[0-9a-fA-F]{6}") String color,
        @NotBlank @Size(max=100) String location, @NotBlank @Size(max=100) String contact, boolean recruiting, boolean active) {}
    public record ActivityInput(@NotNull Long clubId, @NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=30) String category, @NotBlank @Size(max=10000) String description,
        @NotBlank @Size(max=120) String location, @NotNull Instant startsAt, @NotNull Instant endsAt,
        @NotNull Instant registrationDeadline, @Min(1) @Max(10000) int capacity,
        @NotBlank @Size(min=4,max=32) String checkinCode, boolean featured, @NotBlank String status) {}
    public record EquipmentInput(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=40) String category,
        @NotBlank @Size(max=50) String assetCode, @NotBlank @Size(max=1000) String description,
        @NotBlank @Size(max=100) String location, @Min(0) @Max(100000) int totalQuantity, boolean active) {}
    public record LoanInput(@NotNull Long equipmentId, @Min(1) @Max(1000) int quantity,
        @NotBlank @Size(max=500) String purpose, @NotNull LocalDate dueDate) {}
    public record RepairInput(Long equipmentId, @NotBlank @Size(max=120) String title,
        @NotBlank @Size(max=2000) String description, @NotBlank @Size(max=120) String location,
        @NotBlank String priority) {}
    public record RepairProgress(@NotBlank String status, @NotBlank @Size(max=1000) String note) {}
    public record UserAccess(@NotBlank String role, boolean enabled) {}
}
