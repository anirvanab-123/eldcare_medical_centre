CREATE DATABASE IF NOT EXISTS eldercare DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eldercare;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS nursing_record;
DROP TABLE IF EXISTS customer_service;
DROP TABLE IF EXISTS care_level_item;
DROP TABLE IF EXISTS care_item;
DROP TABLE IF EXISTS care_level;
DROP TABLE IF EXISTS checkout_request;
DROP TABLE IF EXISTS outing_request;
DROP TABLE IF EXISTS bed_usage;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS bed;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  role VARCHAR(20) NOT NULL COMMENT 'ADMIN/HEALTH_MANAGER',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_no VARCHAR(20) NOT NULL DEFAULT '606',
  floor_no INT NOT NULL,
  room_no VARCHAR(20) NOT NULL,
  room_type VARCHAR(20) NOT NULL DEFAULT 'DOUBLE' COMMENT 'SINGLE/DOUBLE/MULTI/APARTMENT',
  area DECIMAL(5,1) DEFAULT NULL COMMENT '房间面积(㎡)',
  capacity INT DEFAULT 2 COMMENT '房间容量',
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_building_room (building_no, room_no)
) ENGINE=InnoDB;

CREATE TABLE bed (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  bed_no VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT 'FREE/OCCUPIED/OUTING',
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_room_bed (room_id, bed_no),
  CONSTRAINT fk_bed_room FOREIGN KEY (room_id) REFERENCES room(id)
) ENGINE=InnoDB;

CREATE TABLE care_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  level_code VARCHAR(20) NOT NULL,
  name VARCHAR(50) NOT NULL UNIQUE,
  daily_price DECIMAL(10,2) NOT NULL,
  description VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  gender VARCHAR(10) NOT NULL,
  birth_date DATE NOT NULL,
  id_card VARCHAR(18) NOT NULL UNIQUE,
  blood_type VARCHAR(10),
  family_name VARCHAR(50) NOT NULL,
  family_phone VARCHAR(20) NOT NULL,
  building_no VARCHAR(20) NOT NULL DEFAULT '606',
  room_id BIGINT,
  bed_id BIGINT,
  check_in_date DATE NOT NULL,
  contract_end_date DATE NOT NULL,
  care_level_id BIGINT,
  health_manager_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'IN_HOME' COMMENT 'IN_HOME/OUTING/CHECKED_OUT',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_customer_room FOREIGN KEY (room_id) REFERENCES room(id),
  CONSTRAINT fk_customer_bed FOREIGN KEY (bed_id) REFERENCES bed(id),
  CONSTRAINT fk_customer_level FOREIGN KEY (care_level_id) REFERENCES care_level(id),
  CONSTRAINT fk_customer_manager FOREIGN KEY (health_manager_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE bed_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  bed_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  active TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_usage_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_usage_bed FOREIGN KEY (bed_id) REFERENCES bed(id),
  KEY idx_usage_customer_active (customer_id, active)
) ENGINE=InnoDB;

CREATE TABLE outing_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  outing_time DATETIME NOT NULL,
  expected_return_time DATETIME NOT NULL,
  actual_return_time DATETIME,
  approval_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/APPROVED/REJECTED',
  approver_id BIGINT,
  approval_time DATETIME,
  approval_remark VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_outing_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_outing_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_outing_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE checkout_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  checkout_type VARCHAR(20) NOT NULL COMMENT 'NORMAL/DEATH/KEEP_BED',
  reason VARCHAR(500) NOT NULL,
  checkout_date DATE NOT NULL,
  approval_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  approver_id BIGINT,
  approval_time DATETIME,
  approval_remark VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_checkout_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_checkout_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_checkout_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

CREATE TABLE care_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  execution_cycle VARCHAR(50) NOT NULL,
  execution_times INT NOT NULL DEFAULT 1,
  description VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE care_level_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  care_level_id BIGINT NOT NULL,
  care_item_id BIGINT NOT NULL,
  UNIQUE KEY uk_level_item (care_level_id, care_item_id),
  CONSTRAINT fk_li_level FOREIGN KEY (care_level_id) REFERENCES care_level(id),
  CONSTRAINT fk_li_item FOREIGN KEY (care_item_id) REFERENCES care_item(id)
) ENGINE=InnoDB;

CREATE TABLE customer_service (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  care_item_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  expiry_date DATE NOT NULL,
  total_quantity INT NOT NULL DEFAULT 1,
  remaining_quantity INT NOT NULL DEFAULT 1,
  source_type VARCHAR(20) NOT NULL DEFAULT 'ADD_ON' COMMENT 'LEVEL/ADD_ON',
  source_level_id BIGINT,
  paid_status TINYINT NOT NULL DEFAULT 1 COMMENT '1已缴费/0欠费',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_service_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_service_item FOREIGN KEY (care_item_id) REFERENCES care_item(id),
  KEY idx_service_customer (customer_id, deleted)
) ENGINE=InnoDB;

CREATE TABLE nursing_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  customer_service_id BIGINT NOT NULL,
  health_manager_id BIGINT NOT NULL,
  nursing_time DATETIME NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  remark VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_record_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_record_service FOREIGN KEY (customer_service_id) REFERENCES customer_service(id),
  CONSTRAINT fk_record_manager FOREIGN KEY (health_manager_id) REFERENCES sys_user(id)
) ENGINE=InnoDB;

-- ====== 初始化数据 ======

-- 用户
INSERT INTO sys_user(username,password,real_name,phone,role) VALUES
('admin','admin','系统管理员','13800000000','ADMIN'),
('admin1','admin1','系统管理员一','13800000001','ADMIN'),
('admin2','admin2','系统管理员二','13800000002','ADMIN'),
('manager01','123456','健康管家张宁','13900000001','HEALTH_MANAGER'),
('manager02','123456','健康管家李芳','13900000002','HEALTH_MANAGER'),
('manager03','123456','健康管家王强','13900000003','HEALTH_MANAGER');

-- 房间：4 楼，每层不同类型
INSERT INTO room(building_no,floor_no,room_no,room_type,area,capacity) VALUES
('606',1,'101','APARTMENT',85.0,2),
('606',1,'102','APARTMENT',95.0,3),
('606',1,'103','DOUBLE',35.0,2),
('606',1,'104','DOUBLE',35.0,2),
('606',2,'201','SINGLE',25.0,1),
('606',2,'202','SINGLE',25.0,1),
('606',2,'203','DOUBLE',35.0,2),
('606',2,'204','DOUBLE',35.0,2),
('606',3,'301','DOUBLE',35.0,2),
('606',3,'302','DOUBLE',35.0,2),
('606',3,'303','MULTI',55.0,4),
('606',3,'304','MULTI',55.0,4),
('606',4,'401','MULTI',55.0,4),
('606',4,'402','MULTI',55.0,4);

-- 床位：每个房间根据 capacity 创建
INSERT INTO bed(room_id,bed_no) VALUES
(1,'A'),(1,'B'),
(2,'A'),(2,'B'),(2,'C'),
(3,'A'),(3,'B'),
(4,'A'),(4,'B'),
(5,'A'),
(6,'A'),
(7,'A'),(7,'B'),
(8,'A'),(8,'B'),
(9,'A'),(9,'B'),
(10,'A'),(10,'B'),
(11,'A'),(11,'B'),(11,'C'),(11,'D'),
(12,'A'),(12,'B'),(12,'C'),(12,'D'),
(13,'A'),(13,'B'),(13,'C'),(13,'D'),
(14,'A'),(14,'B'),(14,'C'),(14,'D');

-- 护理级别
INSERT INTO care_level(level_code,name,daily_price,description) VALUES
('L1','一级护理',120.00,'基础护理，适用于自理能力良好的老人'),
('L2','二级护理',180.00,'标准护理，提供日常协助与监测'),
('L3','三级护理',250.00,'专业护理，适用于需要较多照护的老人');

-- 护理项目
INSERT INTO care_item(item_code,name,price,execution_cycle,execution_times,description) VALUES
('HL001','晨间护理',30.00,'每日',1,'协助洗漱、整理床铺'),
('HL002','血压测量',15.00,'每日',2,'测量并记录血压'),
('HL003','康复训练',80.00,'每周',3,'按康复计划进行训练'),
('HL004','服药提醒',20.00,'每日',3,'按时提醒并协助服药'),
('HL005','体温测量',10.00,'每日',2,'监测体温变化'),
('HL006','翻身拍背',25.00,'每2小时',1,'预防褥疮'),
('HL007','夜间巡房',35.00,'每晚',1,'夜间巡查老人状况'),
('HL008','心理疏导',50.00,'每周',1,'心理关怀与沟通');

-- 级别-项目关联
INSERT INTO care_level_item(care_level_id,care_item_id) VALUES
(1,1),(1,2),(1,4),
(2,1),(2,2),(2,3),(2,4),(2,5),
(3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8);

-- 示例客户（8 位）
INSERT INTO customer(name,gender,birth_date,id_card,blood_type,family_name,family_phone,building_no,room_id,bed_id,check_in_date,contract_end_date,care_level_id,health_manager_id,status) VALUES
('张秀兰','女','1945-03-12','110101194503120011','A型','张伟','13611112222','606',1,1,DATE_SUB(CURDATE(),INTERVAL 90 DAY),DATE_ADD(CURDATE(),INTERVAL 275 DAY),1,4,'IN_HOME'),
('李建国','男','1942-08-25','110101194208250022','B型','李娜','13622223333','606',2,5,DATE_SUB(CURDATE(),INTERVAL 60 DAY),DATE_ADD(CURDATE(),INTERVAL 305 DAY),2,5,'IN_HOME'),
('王桂芳','女','1948-11-05','110101194811050033','O型','王强','13633334444','606',6,10,DATE_SUB(CURDATE(),INTERVAL 45 DAY),DATE_ADD(CURDATE(),INTERVAL 320 DAY),3,6,'IN_HOME'),
('陈德昌','男','1940-06-18','110101194006180044','AB型','陈静','13644445555','606',11,21,DATE_SUB(CURDATE(),INTERVAL 30 DAY),DATE_ADD(CURDATE(),INTERVAL 335 DAY),2,4,'IN_HOME'),
('刘淑芬','女','1946-02-28','110101194602280055','A型','刘洋','13655556666','606',7,13,DATE_SUB(CURDATE(),INTERVAL 120 DAY),DATE_ADD(CURDATE(),INTERVAL 245 DAY),1,5,'OUTING'),
('赵国栋','男','1943-09-15','110101194309150066','B型','赵磊','13666667777','606',8,16,DATE_SUB(CURDATE(),INTERVAL 200 DAY),DATE_ADD(CURDATE(),INTERVAL 165 DAY),3,6,'IN_HOME'),
('孙美玲','女','1947-12-22','110101194712220077','O型','孙浩','13677778888','606',3,7,DATE_SUB(CURDATE(),INTERVAL 15 DAY),DATE_ADD(CURDATE(),INTERVAL 350 DAY),2,4,'IN_HOME'),
('周文博','男','1941-04-10','110101194104100088','A型','周明','13688889999','606',13,27,DATE_SUB(CURDATE(),INTERVAL 25 DAY),DATE_ADD(CURDATE(),INTERVAL 340 DAY),1,5,'IN_HOME');

-- 修复床位状态：根据客户关联动态更新bed表status字段
UPDATE bed b
LEFT JOIN customer c ON c.bed_id = b.id AND c.deleted = 0 AND c.status <> 'CHECKED_OUT'
SET b.status = CASE 
    WHEN c.id IS NULL THEN 'FREE'
    WHEN c.status = 'OUTING' THEN 'OUTING'
    ELSE 'OCCUPIED'
END
WHERE b.deleted = 0;

-- 床位使用记录（为在住客户创建）
INSERT INTO bed_usage(customer_id,bed_id,start_date,end_date,active)
SELECT c.id,c.bed_id,c.check_in_date,NULL,1
FROM customer c WHERE c.status='IN_HOME';

-- 客户护理服务（根据级别自动生成，数量根据周期计算合理值）
INSERT INTO customer_service(customer_id,care_item_id,purchase_date,expiry_date,total_quantity,remaining_quantity,source_type,source_level_id,paid_status)
SELECT c.id,cli.care_item_id,c.check_in_date,c.contract_end_date,
  CASE WHEN i.execution_cycle='每日' THEN DATEDIFF(c.contract_end_date,c.check_in_date)*i.execution_times
       WHEN i.execution_cycle='每周' THEN FLOOR(DATEDIFF(c.contract_end_date,c.check_in_date)/7)*i.execution_times
       WHEN i.execution_cycle='每2小时' THEN DATEDIFF(c.contract_end_date,c.check_in_date)*12
       WHEN i.execution_cycle='每晚' THEN DATEDIFF(c.contract_end_date,c.check_in_date)
       ELSE DATEDIFF(c.contract_end_date,c.check_in_date) END,
  CASE WHEN i.execution_cycle='每日' THEN DATEDIFF(c.contract_end_date,c.check_in_date)*i.execution_times
       WHEN i.execution_cycle='每周' THEN FLOOR(DATEDIFF(c.contract_end_date,c.check_in_date)/7)*i.execution_times
       WHEN i.execution_cycle='每2小时' THEN DATEDIFF(c.contract_end_date,c.check_in_date)*12
       WHEN i.execution_cycle='每晚' THEN DATEDIFF(c.contract_end_date,c.check_in_date)
       ELSE DATEDIFF(c.contract_end_date,c.check_in_date) END,
  'LEVEL',c.care_level_id,1
FROM customer c
JOIN care_level_item cli ON cli.care_level_id=c.care_level_id
JOIN care_item i ON i.id=cli.care_item_id
WHERE c.care_level_id IS NOT NULL AND c.deleted=0;

-- 护理执行记录（示例数据，根据客户服务自动生成合理记录）
INSERT INTO nursing_record(customer_id,customer_service_id,health_manager_id,nursing_time,quantity,remark)
SELECT cs.customer_id, cs.id, c.health_manager_id,
  CASE i.execution_cycle
    WHEN '每日' THEN TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*10+1) DAY), CASE i.item_code WHEN 'HL007' THEN '20:00:00' WHEN 'HL001' THEN '08:00:00' WHEN 'HL002' THEN '09:00:00' WHEN 'HL003' THEN '10:00:00' WHEN 'HL004' THEN '12:00:00' WHEN 'HL005' THEN '08:00:00' WHEN 'HL006' THEN '14:00:00' ELSE '10:00:00' END)
    WHEN '每周' THEN TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*7+1) DAY), '10:00:00')
    WHEN '每2小时' THEN TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*5+1) DAY), '14:00:00')
    WHEN '每晚' THEN TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*5+1) DAY), '20:00:00')
    ELSE TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*5+1) DAY), '10:00:00')
  END,
  1,
  CONCAT(i.name,'完成')
FROM customer_service cs
JOIN customer c ON c.id=cs.customer_id AND c.deleted=0 AND c.status='IN_HOME'
JOIN care_item i ON i.id=cs.care_item_id
JOIN sys_user u ON u.id=c.health_manager_id AND u.role='HEALTH_MANAGER'
WHERE cs.deleted=0 AND cs.remaining_quantity>0 AND cs.source_type='LEVEL'
ORDER BY RAND()
LIMIT 12;

-- 更新床位状态
UPDATE bed b JOIN customer c ON c.bed_id=b.id AND c.deleted=0 AND c.status='IN_HOME' SET b.status='OCCUPIED';
UPDATE bed b JOIN customer c ON c.bed_id=b.id AND c.deleted=0 AND c.status='OUTING' SET b.status='OUTING';

SET FOREIGN_KEY_CHECKS = 1;
