-- Archive only the original generic demonstration records. Preserve all history.
UPDATE clubs SET active=FALSE,recruiting=FALSE WHERE name IN ('光影摄影社','山野户外社','回声音乐社','代码与创意协会','青禾志愿者协会','风行羽毛球社','知行读书会','创新创业联盟');
UPDATE activities SET status='CANCELLED' WHERE club_id IN (SELECT id FROM clubs WHERE name IN ('光影摄影社','山野户外社','回声音乐社','代码与创意协会','青禾志愿者协会','风行羽毛球社','知行读书会','创新创业联盟'));
UPDATE equipment SET active=FALSE WHERE asset_code IN ('CH-PH-001','CH-OD-002','CH-AV-003','CH-AV-004','CH-SP-005','CH-AV-006');
INSERT INTO clubs (name,category,summary,description,icon,color,location,contact,recruiting,active)
VALUES ('征途单车俱乐部','骑行运动','西安欧亚学院 · 把热爱，骑成征途。','西安欧亚学院征途单车俱乐部，欢迎热爱单车与骑行的同学了解和加入。本网站提供入社申请与审核、骑行活动发布报名及签到、成员活动记录、器材登记借还、在线报修和数据报表服务。活动时间、路线与集合地点以管理员发布的活动详情为准；具体入社要求和联系方式由俱乐部确认后公布。','bike','#43664D','西安欧亚学院 · 集合地点见活动通知','联系渠道由俱乐部管理员确认后公布',TRUE,TRUE)
ON CONFLICT (name) DO NOTHING;
-- Equipment and event details must be entered by the club administrator after verification.
-- Do not invent real inventory, meeting places, event schedules or member numbers.
