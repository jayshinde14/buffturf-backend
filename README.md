# BuffTurf - Sports Turf Booking System

![BuffTurf Banner](https://via.placeholder.com/1200x400?text=BuffTurf+-+Sports+Turf+Booking) *(Optional: Add a banner image here)*

BuffTurf is a comprehensive web application designed to streamline the booking and management of sports turfs. It provides a seamless interface for users to discover, book, and pay for turf slots, while offering administrators robust tools to manage facilities, bookings, and users.

> **Note to Interviewers:** This project demonstrates a full-stack, decoupled architecture with integrated payments, JWT-based security, and dynamic QR code generation.

## 🔗 Live Demo
* **Frontend:** [Hosted on Vercel](#) *(Add your Vercel URL here)*
* **Backend API:** [Hosted on Render](#) *(Add your Render URL here)*
* **Database:** MySQL Database hosted on Clever Cloud

---

## 🚀 Features

### For Users
*   **User Authentication:** Secure registration and login using JWT (JSON Web Tokens).
*   **Turf Discovery:** Browse available sports turfs and view their details.
*   **Real-time Booking:** Check slot availability and book turfs instantly.
*   **Secure Payments:** Integrated with Razorpay for seamless and secure transaction processing.
*   **Booking History & QR Codes:** View past bookings and generate QR codes for booking verification at the venue.
*   **Email Notifications:** Receive booking confirmations and password reset OTPs via email.
*   **AI Assistant:** Integrated AI features to assist users in the booking process.

### For Administrators
*   **Admin Dashboard:** Centralized dashboard to oversee platform operations.
*   **QR Scanner:** Built-in scanner to verify user bookings at the venue.
*   **Turf Management:** Add, update, and remove turf listings.
*   **Booking & User Management:** View all user bookings and oversee user activities.

---

## 🛠️ Tech Stack & Architecture

This project follows a standard decoupled client-server architecture.

### Frontend (Hosted on Vercel)
*   **Framework:** React.js (v19)
*   **Routing:** React Router DOM
*   **HTTP Client:** Axios
*   **Utilities:** `qrcode.react`, `html5-qrcode`

### Backend (Hosted on Render)
*   **Framework:** Spring Boot (v3.5) with Java 21
*   **Security:** Spring Security & JWT
*   **Payment Gateway:** Razorpay API
*   **Email Service:** Spring Boot Mail (SMTP)
*   **QR Code Processing:** Google ZXing

### Database (Hosted on Clever Cloud)
*   **Type:** Relational Database (MySQL)
*   **ORM:** Spring Data JPA / Hibernate

---

## 💻 Local Development

*(It is highly professional to include local setup instructions so other developers or interviewers know how to run your code on their machines.)*

### Prerequisites
*   Node.js (v18+) and npm
*   Java Development Kit (JDK) 21
*   Maven
*   MySQL Server (if you want to use a local DB instead of Clever Cloud)
*   Razorpay Account (for testing payments)

### 1. Backend Setup

1.  Navigate to the backend directory:
    ```bash
    cd "Turf Backend Files/buffturf-backend/buffturf-backend"
    ```
2.  Configure Environment Variables:
    Create a `.env` file in the root directory and add the necessary configuration:
    ```env
    # Database Configuration (Clever Cloud or Local)
    DB_URL=jdbc:mysql://<your-clever-cloud-host>:3306/<db-name>
    DB_USER=<your-db-user>
    DB_PASS=<your-db-password>
    
    # Application Secrets
    JWT_SECRET=your_jwt_secret_key
    RAZORPAY_KEY_ID=your_key_id
    RAZORPAY_KEY_SECRET=your_key_secret
    
    # Mail Configuration
    MAIL_USERNAME=your_email@gmail.com
    MAIL_PASSWORD=your_app_password
    ```
3.  Build and Run:
    ```bash
    ./mvnw spring-boot:run
    ```

### 2. Frontend Setup

1.  Navigate to the frontend directory:
    ```bash
    cd buffturf-frontend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Start the development server:
    ```bash
    npm start
    ```
    The application will run at `http://localhost:3000`.

---

## 📸 Application Gallery

<!-- Example of how to add an image:
![Home Page](./screenshots/home.png)
-->

- **Home Page**
- **Booking Interface & Razorpay Checkout**
- **Admin Dashboard & QR Scanner**
- **User Profile & QR Ticket**

---

## 🤝 Contributing
Contributions, issues, and feature requests are welcome!

## 📄 License
This project is licensed under the [MIT License](LICENSE).
