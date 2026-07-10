# 🌐 Network Device Inventory & Status Monitoring System

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apachemaven)
![MongoDB](https://img.shields.io/badge/MongoDB-6.x%2F7.x-green?style=for-the-badge&logo=mongodb)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

A **console-based** network device inventory and real-time status monitoring system built with **Java 17** and **MongoDB**.

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Data Model](#-data-model)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
- [How to Run](#-how-to-run)
- [Menu Options](#-menu-options)
- [Sample Output](#-sample-output)
- [How Status Check Works](#-how-status-check-works)
- [Known Limitations](#-known-limitations)

---

## 🔍 Overview

This application allows you to manage and monitor network devices entirely from your terminal.  
It stores all device data in **MongoDB** and uses Java's built-in `InetAddress` API to check whether a device is **Online** or **Offline** in real time.

> ✅ No GUI &nbsp;|&nbsp; ✅ No Spring Boot &nbsp;|&nbsp; ✅ Single Java File &nbsp;|&nbsp; ✅ Pure Maven

---

## ✨ Features

| Feature | Description |
|---|---|
| ➕ Add Device | Store a new network device with full details |
| 📋 View All Devices | List every device in the database |
| 🔍 Search Device | Find a device by its unique Device ID |
| ✏️ Update Device | Edit any field except the Device ID |
| 🗑️ Delete Device | Remove a device with confirmation |
| 📡 Check Device Status | Ping a single device and update its status |
| 🌐 Check All Devices | Scan every device and show Online/Offline summary |
| 🚪 Exit | Gracefully close MongoDB connection and quit |

---

## 🛠 Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Build Tool | Apache Maven 3.x |
| Database | MongoDB Community Server 6.x / 7.x |
| MongoDB Driver | `mongodb-driver-sync` 4.11.1 |
| Networking | Java `InetAddress` (built-in JDK) |

---

## 📁 Project Structure

```
NetworkDeviceInventory/
├── pom.xml                   ← Maven build + dependencies
└── src/
    └── main/
        └── java/
            └── Main.java     ← Entire application (single file)
```

### Inside `Main.java` — Nested Class Architecture

```
Main.java
├── Device          → Data model (fields, getters, toDocument, fromDocument, print)
├── Database        → MongoDB connection lifecycle (connect, close)
├── DeviceDAO       → All CRUD operations (insert, findAll, findById, update, delete)
├── NetworkUtils    → InetAddress.isReachable() + timestamp helper
└── Main            → Entry point, menu loop, feature methods, input/validation helpers
```

---

## 🗂 Data Model

Each device document stored in MongoDB contains:

| Field | Type | Required | Default |
|---|---|---|---|
| `deviceId` | String | ✅ Yes | User-provided |
| `deviceName` | String | ✅ Yes | User-provided |
| `ipAddress` | String | ✅ Yes | Validated IPv4 |
| `macAddress` | String | ⬜ Optional | `N/A` if skipped |
| `deviceType` | String | ✅ Yes | e.g., Laptop, Router |
| `location` | String | ✅ Yes | e.g., Lab Room 3 |
| `description` | String | ✅ Yes | Free-text notes |
| `status` | String | Auto | `Unknown` initially |
| `lastChecked` | String | Auto | `Never` initially |

**Database:** `networkDB` &nbsp;|&nbsp; **Collection:** `devices` &nbsp;|&nbsp; **Host:** `localhost:27017`

---

## ✅ Prerequisites

Before running the project, ensure you have:

1. **Java 17** installed  
   👉 https://adoptium.net/

2. **Apache Maven 3.x** installed  
   👉 https://maven.apache.org/download.cgi

3. **MongoDB Community Server** installed and running locally  
   👉 https://www.mongodb.com/try/download/community

---

## ⚙️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Vyshnav-ms/Network-Device-Inventory-Status-Monitoring-System.git
cd Network-Device-Inventory-Status-Monitoring-System
```

### 2. Start MongoDB

```powershell
# Windows (if installed as a service)
net start MongoDB

# Or start manually
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" --dbpath "C:\data\db"
```

Verify MongoDB is running:
```bash
mongosh --eval "db.runCommand({ping:1})"
# Expected: { ok: 1 }
```

> **No manual database or collection setup needed** — MongoDB creates `networkDB` and `devices` automatically on first insert.

---

## ▶️ How to Run

```powershell
# Step 1 — Compile
mvn compile

# Step 2 — Run
mvn exec:java

# Or both in one command
mvn compile exec:java
```

### To Exit the Application
Type `8` and press Enter at the menu prompt.

> ⚠️ **Important:** Type the **number** `8`, not the word "Exit".

---

## 📌 Menu Options

```
=============================================
 NETWORK DEVICE INVENTORY SYSTEM
=============================================
1. Add Device
2. View All Devices
3. Search Device
4. Update Device
5. Delete Device
6. Check Device Status
7. Check All Devices
8. Exit
=============================================
Enter Choice:
```

### Option Details

#### `1` Add Device
- Prompts for all device fields
- Validates IPv4 format (`0–255.0–255.0–255.0–255`)
- Rejects duplicate Device IDs
- MAC Address is **optional** — press Enter to skip (stores `N/A`)
- Sets `status = Unknown`, `lastChecked = Never`

#### `2` View All Devices
- Lists all stored devices with full formatted details

#### `3` Search Device
- Search by Device ID
- Shows all details or `Device Not Found.`

#### `4` Update Device
- Shows current values in `[brackets]`
- Press Enter to keep any field unchanged
- Cannot change Device ID
- Validates new IP address

#### `5` Delete Device
- Shows full device details before deletion
- Requires `yes` confirmation

#### `6` Check Device Status
- Pings a single device IP
- Updates `status` and `lastChecked` in MongoDB
- Displays `ONLINE` or `OFFLINE`

#### `7` Check All Devices
- Pings every device IP sequentially
- Updates all statuses in MongoDB
- Displays dot-padded summary table

#### `8` Exit
- Prints farewell message
- Closes MongoDB connection
- Exits cleanly

---

## 🖥 Sample Output

### Adding a Device
```
  [ ADD NEW DEVICE ]
  ------------------------------------------
  Device ID       : D101
  Device Name     : Maverick
  IP Address      : 10.187.2.118
  MAC Address     : (optional, press Enter to skip) 0A-00-27-00-00-04
  Device Type     : Laptop
  Location        : Ratanhall
  Description     : Abhishek

  ✔  Device Added Successfully.
```

### View All Devices
```
---------------------------------------------------
  Device ID    : D101
  Name         : Maverick
  IP Address   : 10.187.2.118
  MAC Address  : 0A-00-27-00-00-04
  Type         : Laptop
  Location     : Ratanhall
  Description  : Abhishek
  Status       : Offline
  Last Checked : 2026-07-10 11:47:52
---------------------------------------------------
```

### Check All Devices
```
  Checking Devices...

  Maverick......................  OFFLINE
  Hassn Laptop..................  ONLINE

  ------------------------------------------
  Online : 1   |   Offline : 1
  ------------------------------------------
  Scan Completed.
```

---

## 📡 How Status Check Works

```
InetAddress.getByName(ipAddress)
           ↓
   isReachable(3000ms)
           ↓
   Admin rights?
   YES → ICMP Ping
   NO  → TCP Port 7 fallback
           ↓
   Response received?
   YES → Online
   NO  → Offline
           ↓
   MongoDB updated: status + lastChecked
```

**Timeout:** 3000 milliseconds per device.

### Windows Note
On Windows, `InetAddress.isReachable()` requires **Administrator privileges** to send ICMP packets. Without them, reachable devices may appear Offline.

**Fix:** Right-click your terminal → **Run as Administrator** → then `mvn exec:java`

---

## ⚠️ Known Limitations

| Issue | Reason | Workaround |
|---|---|---|
| Device appears Offline despite ping working | Windows blocks ICMP without admin | Run terminal as Administrator |
| Sequential scan is slow for many devices | Single-threaded design | Future: use parallel threads |
| No MongoDB authentication | Local dev setup | Add auth for production |
| MAC address not format-validated | Format varies (dashes, colons, etc.) | Accepted as free text |

---

## 🧱 MongoDB Operations Used

```java
collection.insertOne(document)
collection.find()
collection.find(Filters.eq("deviceId", id)).first()
collection.updateOne(filter, Updates.combine(...))
collection.deleteOne(Filters.eq("deviceId", id))
```

---

## 📄 License

This project is licensed under the **MIT License**.

---

<div align="center">
Made with ☕ Java &nbsp;|&nbsp; 🍃 MongoDB &nbsp;|&nbsp; ❤️ for network geeks
</div>
