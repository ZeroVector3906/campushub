ALTER TABLE app_users DROP CONSTRAINT app_users_role_check;
ALTER TABLE app_users ADD CONSTRAINT app_users_role_check CHECK(role IN ('STUDENT','ADMIN','SUPER_ADMIN'));
UPDATE app_users SET role='SUPER_ADMIN' WHERE username='admin' AND role='ADMIN'
 AND NOT EXISTS(SELECT 1 FROM app_users WHERE role='SUPER_ADMIN' AND enabled);
