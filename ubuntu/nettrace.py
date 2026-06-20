import os
import json
import subprocess
import csv
from datetime import datetime, timedelta

# Configuration
LOG_FILE = "nettrace_log.csv"
DEVICE_NAME = "Ubuntu-Laptop"

def get_brave_path():
    """Finds the path to the Brave browser dynamically."""
    try:
        return subprocess.check_output(["which", "brave-browser"]).decode("utf-8").strip()
    except subprocess.CalledProcessError:
        return ""

def run_speed_test():
    """Runs fast-cli and returns Download, Upload, and Ping."""
    brave_path = get_brave_path()
    env = os.environ.copy()
    
    if brave_path:
        env["PUPPETEER_EXECUTABLE_PATH"] = brave_path

    try:
        # We use a 90-second timeout so it doesn't hang forever if the internet is down
        result = subprocess.run(
            ["/usr/local/bin/fast", "--upload", "--json"],
            env=env,
            capture_output=True,
            text=True,
            timeout=90
        )
        data = json.loads(result.stdout)
        return data.get("downloadSpeed", 0.0), data.get("uploadSpeed", 0.0), data.get("ping", 0), "Active_Connection"
    except Exception:
        return 0.0, 0.0, 0, "OFFLINE_OR_TIMEOUT"

def backfill_missing_hours(current_time):
    """Checks the CSV for gaps and fills them if the laptop was off."""
    if not os.path.exists(LOG_FILE):
        return # No file yet, nothing to backfill

    try:
        with open(LOG_FILE, "r") as f:
            lines = f.readlines()
            if len(lines) <= 1:
                return # Only header exists

            last_line = lines[-1].strip().split(",")
            last_date_str, last_time_str = last_line[0], last_line[1]
            last_dt = datetime.strptime(f"{last_date_str} {last_time_str}", "%Y-%m-%d %H:00")
            
            # Floor current time to the active hour
            current_dt = current_time.replace(minute=0, second=0, microsecond=0)
            
            # Calculate gap
            gap_hours = int((current_dt - last_dt).total_seconds() // 3600)
            
            if gap_hours > 1:
                with open(LOG_FILE, "a", newline="") as append_file:
                    writer = csv.writer(append_file)
                    # Loop through missing hours and insert offline records
                    for i in range(1, gap_hours):
                        missing_dt = last_dt + timedelta(hours=i)
                        writer.writerow([
                            missing_dt.strftime("%Y-%m-%d"),
                            missing_dt.strftime("%H:00"),
                            DEVICE_NAME,
                            0.0, 0.0, 0,
                            "OFFLINE_OR_TIMEOUT"
                        ])
    except Exception as e:
        print(f"Backfill error: {e}")

def main():
    # 1. Setup Data File & Headers if it doesn't exist
    if not os.path.exists(LOG_FILE):
        with open(LOG_FILE, "w", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["Date", "Time", "Device", "Download_Mbps", "Upload_Mbps", "Ping_ms", "Network_Name"])

    now = datetime.now()
    
    # 2. Check for gaps and backfill
    backfill_missing_hours(now)

    # 3. Run the actual speed test
    print("Running Fast.com speed test. This may take a minute...")
    download, upload, ping, status = run_speed_test()

    # 4. Append the new result
    with open(LOG_FILE, "a", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([
            now.strftime("%Y-%m-%d"),
            now.strftime("%H:00"), # Logging at the top of the hour for clean charts
            DEVICE_NAME,
            download, upload, ping, status
        ])
    print(f"Logged Data: {download} Mbps Down | {upload} Mbps Up | {ping} ms Ping")

if __name__ == "__main__":
    main()