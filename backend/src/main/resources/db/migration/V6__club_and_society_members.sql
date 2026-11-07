-- Expand only. Existing users and applications need administrator classification.
ALTER TABLE app_users ADD COLUMN join_type VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED'
 CHECK(join_type IN ('UNASSIGNED','CLUB','SOCIETY'));
ALTER TABLE app_users ADD COLUMN contact VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE app_users ADD COLUMN grade VARCHAR(40) NOT NULL DEFAULT '';
ALTER TABLE memberships ADD COLUMN join_type VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED'
 CHECK(join_type IN ('UNASSIGNED','CLUB','SOCIETY'));
ALTER TABLE memberships ADD COLUMN club_division VARCHAR(20) NOT NULL DEFAULT ''
 CHECK(club_division IN ('','课程部','活动部','宣传部'));
-- Carry forward only real applicant details already recorded for this organization.
UPDATE app_users u SET contact=m.contact, grade=m.grade
FROM memberships m JOIN clubs c ON c.id=m.club_id
WHERE m.user_id=u.id AND c.name='征途单车俱乐部';
