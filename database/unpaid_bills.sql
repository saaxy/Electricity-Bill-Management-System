-- Helper script: list unpaid bills and consumer contact info
-- Usage:
--  mysql -u root -p -h localhost < unpaid_bills.sql
-- or run the SELECT inside your mysql prompt after USE electricitydb;

USE electricitydb;

-- Select bills that are not marked SUCCESS (either NULL, PENDING, or other failure states)
SELECT
  b.id AS bill_id,
  b.consumer_id,
  COALESCE(c.name, '(unknown)') AS consumer_name,
  COALESCE(c.email, '') AS consumer_email,
  COALESCE(c.phone, '') AS consumer_phone,
  b.amount,
  b.billing_date,
  b.due_date,
  COALESCE(b.payment_status, 'PENDING') AS payment_status,
  b.razorpay_order_id
FROM bill b
LEFT JOIN consumer c ON b.consumer_id = c.id
WHERE COALESCE(b.payment_status, 'PENDING') <> 'SUCCESS'
ORDER BY b.billing_date DESC, b.id DESC
LIMIT 200;

-- End of script
