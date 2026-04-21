# Cabika - Shared Cab Booking for Students 🚕

**Cabika** is a real-world, production-ready Android application built to simplify daily commute for college students through shared cab booking. The platform connects students traveling in the same direction, allowing them to share rides, reduce costs, and improve convenience.

The app is successfully deployed and publicly available on the Google Play Store, demonstrating its scalability and practical usability.

---

## 📲 Download Now
[![Get it on Google Play](https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=com.hk.vcab&pcampaignid=web_share)
*Available on Google Play Store*

---

## 📌 Problem Statement

College students often face significant hurdles when traveling:
- **High Individual Costs:** Booking a full cab solo is expensive for a student budget.
- **Coordination Gap:** Finding others traveling the same route at the same time is difficult.
- **Safety Concerns:** Lack of a trusted community-based coordination system.

**Cabika** solves this by creating a centralized, verified ride-sharing platform within the university ecosystem.

---

## 💡 Solution Overview

Cabika acts as a community-driven transportation network designed specifically for campuses. It enables students to:
- **Create Shared Bookings:** Post your travel plans and find co-passengers.
- **Discover Rides:** Real-time feed of available rides posted by others.
- **Instant Joining:** Join existing rides with a single tap.
- **Direct Coordination:** Seamlessly connect with co-passengers and drivers.

---

## ✨ Key Features

### 🎓 For Students
- **🔐 Secure Authentication:** Google Sign-in & email-based system (Restricted to college domains like `@vitbhopal.ac.in`).
- **🚗 Smart Booking System:** Create rides by selecting pickup, drop, travel time, and car type (5/7 seater).
- **🎀 Girl Only Cab:** A dedicated safety feature allowing female passengers to book/join exclusive female-only rides.
- **👥 Discovery Feed:** Browse all active rides, filtered by route and availability.
- **📂 Ride Management:** Track "My Bookings" and "Ongoing Rides" with easy edit/delete options.
- **📍 Predefined Locations:** Consistent routing using common points (VIT Bhopal, Railway Stations, Airports, etc.).

### 🚗 For Drivers
- **Verified Driver Dashboard:** Access for authorized drivers to accept student requests.
- **Available Bookings:** A real-time list of students requiring a driver, sorted chronologically.
- **Contact Integration:** Instant Call/WhatsApp buttons to coordinate with passengers.

### 🛡️ Admin Controls
- **System Oversight:** Monitor all users, drivers, and active bookings to ensure platform integrity.

---

## 🏗️ Tech Stack

- **Frontend:** Android (XML + Kotlin)
- **Backend:** Firebase 
    - **Authentication:** Secure Google Sign-In.
    - **Firestore:** High-performance real-time NoSQL database.
    - **Cloud Messaging (FCM):** Push notifications for ride assignments and updates.
- **Architecture:** MVVM-based structure for scalability and maintainability.
- **UI Components:** Material Design 3, Lottie Animations for smooth UX.

---

## 📂 Project Structure (Source Code)

```text
app/src/main/java/com/hk/vcab/
├── activities/
│   ├── HomeActivity.kt             # Smart redirection & Onboarding
│   ├── Login/RegisterActivity.kt   # Secure Auth flow
│   ├── BookRideActivity.kt         # Ride creation & Logic
│   ├── MatchedRidesActivity.kt     # Real-time Matching algorithm
│   └── MyRidesActivity.kt          # User-specific ride tracking
├── models/
│   ├── Ride.kt                     # Data structure for transport details
│   └── Passenger.kt                # Student/Driver profile data
├── adapters/
│   └── RideAdapter.kt              # Dynamic list rendering
└── MyFirebaseMessagingService.kt   # Background notification service
```

---

## ⚙️ How It Works

1. **Sign Up/Login:** Use your university email for verification.
2. **Action:** Create a ride request OR browse existing rides.
3. **Match:** Join a ride that fits your schedule.
4. **Coordinate:** Contact travel partners/drivers via WhatsApp/Call.
5. **Travel:** Complete your journey and manage history in the dashboard.

---

## 🚀 Real-World Impact

- **Sustainability:** Encourages ride-sharing, reducing the carbon footprint of campus travel. 🌱
- **Economy:** Significantly reduces travel costs through cost-splitting.
- **Community:** Strengthens coordination and trust within the student body.

---

## 📌 Future Roadmap

- [ ] **Live Location Tracking:** Real-time visibility of the cab's position. 📍
- [ ] **In-App Chat:** Secure messaging without sharing phone numbers. 💬
- [ ] **Rating System:** Build trust with Peer & Driver reviews. ⭐
- [ ] **AI-Based Matching:** Intelligent route optimization. 🤖
- [ ] **Payment Integration:** In-app wallet and split-payment system. 💳

---

## 👨‍💻 Developed By

**Harsh Kumar**  
*B.Tech CSE (AI & ML)*  
Portfolio: [harsh0904k.github.io](https://harsh0904k.github.io/vcab-legal/about.html)

---
*Note: Cabika is more than just an app; it's a scalable solution for campus mobility challenges.*
