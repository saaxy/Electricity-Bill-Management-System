

# ⚡ElectraFlow360 -  Electricity Bill Management System

**ElectraFlow360** is a comprehensive system to manage electricity usage, billing, and payments for consumers and administrators. It simplifies billing operations, tracks payments, and provides insightful reports for efficient energy management.

---

## Features

- **Meter Reading Management**: Record and manage consumer electricity meter readings efficiently.  
- **Automatic Bill Calculation**: Generate bills automatically based on unit consumption and tariff slabs.  
- **Payment Tracking**: Monitor bill payments, update status, and maintain transaction history.  
- **Admin and Customer Dashboards**: Role-based dashboards with relevant operations for admins and consumers.  
- **Report Generation**: Generate monthly or yearly reports for consumption, revenue, and outstanding payments.  

---

## Tech Stack

- **Backend**: Java or Python  
- **Database**: MySQL  
- **Frontend** (Optional): HTML / CSS / React  
- **Libraries / Tools**:  
  - JDBC (for Java) or SQLAlchemy (for Python)  
  - JavaMail API for email notifications (optional)  
  - iText or Apache PDFBox for PDF bill generation  

---

## Database Schema

### Consumers Table
| Column       | Type        | Description                  |
|--------------|------------|------------------------------|
| id           | INT        | Primary key                  |
| name         | VARCHAR    | Consumer full name           |
| address      | VARCHAR    | Consumer address             |
| email        | VARCHAR    | Consumer email               |
| meter_number | VARCHAR    | Unique meter identifier      |

### Bills Table
| Column         | Type        | Description                  |
|----------------|------------|------------------------------|
| id             | INT        | Primary key                  |
| consumer_id    | INT        | Foreign key to Consumers     |
| units          | DOUBLE     | Units consumed               |
| rate_per_unit  | DOUBLE     | Rate applied per unit        |
| total_amount   | DOUBLE     | Calculated total bill        |
| billing_date   | DATE       | Bill generation date         |
| status         | VARCHAR    | Payment status (Paid/Pending)|

