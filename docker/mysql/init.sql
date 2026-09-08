-- 1. Create the databases
CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS merchant_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS refund_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS audit_db;

-- 2. Create the user (allowing connections from localhost & docker/network)
CREATE USER IF NOT EXISTS 'payment_app'@'%' IDENTIFIED BY 'rootpassword';
CREATE USER IF NOT EXISTS 'payment_app'@'localhost' IDENTIFIED BY 'rootpassword';

-- 3. Grant privileges on all 6 databases
GRANT ALL PRIVILEGES ON customer_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'payment_app'@'localhost';

GRANT ALL PRIVILEGES ON merchant_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON merchant_db.* TO 'payment_app'@'localhost';

GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_app'@'localhost';

GRANT ALL PRIVILEGES ON refund_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON refund_db.* TO 'payment_app'@'localhost';

GRANT ALL PRIVILEGES ON notification_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'payment_app'@'localhost';

GRANT ALL PRIVILEGES ON audit_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON audit_db.* TO 'payment_app'@'localhost';

-- 4. Reload privileges
FLUSH PRIVILEGES;
