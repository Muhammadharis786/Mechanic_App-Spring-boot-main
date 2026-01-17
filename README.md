# 🚗 Mechanic Finder & Appointment System

A smart digital platform that connects customers with nearby mechanics for **emergency roadside assistance** and **scheduled vehicle repair appointments**.  
The system ensures fast response, real-time tracking, and secure access for all users.

---

## 📌 Features

### 🔧 Mechanic Finder (Emergency Mode)
- Instant mechanic request during vehicle breakdowns
- Automatic mechanic allocation based on:
  - Nearest location
  - Highest rating
  - Availability
- Real-time location tracking of the mechanic

### 📅 Appointment System (Scheduled Mode)
- Book mechanics from home
- Select service category and vehicle problem
- Schedule appointments in advance

---

## 🧭 User Workflow
1. User selects a service category (Car, Bike, Puncture, etc.)
2. Related vehicle problems are displayed
3. User selects the problem and sends a request
4. The system broadcasts the request to nearby mechanics
5. The first mechanic to accept is assigned
6. Live tracking is enabled after acceptance

---

## 📊 Dashboards

### 👤 Customer Dashboard
- Create emergency requests
- Book appointments
- Track mechanic location
- View service history

### 🔧 Mechanic Dashboard
- Receive nearby requests
- Accept or reject jobs
- Manage availability
- Navigate to customer location

### 🛠️ Admin Dashboard
- Manage users and mechanics
- Monitor system activity
- Control platform operations

---

## 🔄 Role Switching
- Users initially register as customers
- Customers can apply to become mechanics
- A switch mode allows seamless transition between Customer and Mechanic dashboards

---

## 🔐 Security
- JWT-based authentication
- Role-based authorization
- Secure access to all system features

---

## 🧪 Technology Stack

| Layer | Technology |
|------|------------|
| Backend | Spring Boot |
| Mobile App | Flutter |
| Database | PostgreSQL |
| Caching | Redis |
| Real-Time Communication | WebSocket |
| Cloud Platform | Google Cloud |
| Authentication | Firebase |

---

## 🎯 Objective
To provide a fast, reliable, and secure platform that improves access to vehicle repair services anytime and anywhere.

---

## 📂 Project Structure (High Level)

Backend/
├── controllers
├── services
├── repositories
├── security
└── websocket

MobileApp/
├── screens
├── services
└── widgets


---

## 📜 License
This project is for educational and demonstration purposes.

