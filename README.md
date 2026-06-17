# 🚀 Home WiFi Performance Monitor

A cross-platform, automated background service to track, log, and analyze home ISP (Internet Service Provider) performance. 

This project is designed to help users diagnose intermittent network degradation (e.g., 60 Mbps fiber plans dropping to 3-4 Mbps) by gathering consistent, undeniable, and human-readable historical data.

---

## 📖 Project Overview

This system runs silently in the background on both Linux (Ubuntu) and Android devices. It performs scheduled hourly network speed tests using Netflix's **Fast.com** infrastructure. The results are appended to a simple, easily accessible CSV log file, enabling daily and weekly analysis of connection stability, speed, and latency.

By testing from multiple devices (a stationary laptop and a mobile phone), the system can help isolate whether network drops are caused by the ISP, the router, or specific Wi-Fi bands (2.4GHz vs. 5GHz).

---

## ✨ Core Features

* **Automated Background Execution:** Runs without user intervention every hour.
* **Human-Readable Logging:** Data is stored in universally accessible `.csv` files for easy import into Excel, Google Sheets, or basic text editors.
* **Realistic Speed Testing:** Utilizes Fast.com (via `fast-cli` and APIs) to bypass ISP prioritization of standard speed-test servers, reflecting true streaming and download capabilities.
* **Smart Gap Detection (Backfilling):** Prevents corrupted or misleading data timelines. If a device is powered off during a scheduled test, the system retroactively logs `OFFLINE_OR_TIMEOUT` for the missed hours upon waking up.
* **Mobile Data Protection:** Enforces strict `UNMETERED` network constraints on mobile devices to ensure tests only run over Wi-Fi, completely protecting cellular data plans from accidental exhaustion.

---

## 🏗️ System Architecture

The project is split into two distinct applications tailored to their respective operating systems.

### 1. Ubuntu / Linux Client (The Anchor)
* **Testing Engine:** Node.js-based `fast-cli`. Extracts Download, Upload, and Ping metrics via JSON output.
* **Task Scheduler:** Standard Linux `cron` daemon scheduled for the top of every hour (e.g., `0 * * * *`).
* **Execution Logic:** * A custom Bash script is triggered by `cron`.
    * The script reads the last timestamp in the CSV log.
    * It calculates missing intervals and backfills `OFFLINE_OR_TIMEOUT` entries if the laptop was suspended.
    * It executes `fast-cli`, parses the JSON output, and appends the current metrics to the CSV.

### 2. Android Client (The Mobile Node)
* **Testing Engine:** Native HTTP requests to the Fast.com API or a lightweight Android speed-test library.
* **Task Scheduler:** Android Jetpack `WorkManager`.
* **Execution Logic:**
    * `WorkManager` queues the background task with a strict `NetworkType.UNMETERED` constraint.
    * To prevent network congestion, execution naturally staggers based on Android's Doze mode and battery optimization, ensuring it does not run at the exact same second as the Ubuntu `cron` job.
    * Upon execution, it performs the same Gap Detection algorithm as the Linux client to backfill missed hours (e.g., when the user was away from home on 5G).

---

## 📊 Data Logging Format

Data is maintained in a strictly formatted CSV file to ensure maximum compatibility. 

**Log File Schema:**
`Date, Time, Device, Download_Mbps, Upload_Mbps, Ping_ms, Network_Name`

**Example Output:**
```csv
Date,Time,Device,Download_Mbps,Upload_Mbps,Ping_ms,Network_Name
2026-06-17,09:00,Ubuntu-Laptop,58.2,12.1,14,Home_WiFi_5G
2026-06-17,10:00,Ubuntu-Laptop,4.1,1.2,145,Home_WiFi_2.4G
2026-06-17,11:00,Ubuntu-Laptop,0.0,0.0,0,OFFLINE_OR_TIMEOUT
2026-06-17,12:00,Ubuntu-Laptop,60.1,13.0,12,Home_WiFi_5G
