-- Preserve credentials and user-customized names; update only the old default display name.
UPDATE app_users SET name='征途俱乐部管理员' WHERE role='ADMIN' AND name='校园管理员';
