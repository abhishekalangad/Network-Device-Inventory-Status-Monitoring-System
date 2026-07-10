import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  Network Device Inventory & Status Monitoring System
 * ============================================================
 *  A console-based application for managing network devices
 *  stored in MongoDB with real-time connectivity checking.
 *
 *  Database : networkDB
 *  Collection: devices
 *  MongoDB   : mongodb://localhost:27017
 * ============================================================
 */
public class Main {

    // ─────────────────────────────────────────────────────────────
    //  STATIC NESTED CLASS: Device (Data Model)
    // ─────────────────────────────────────────────────────────────

    /**
     * Represents a single network device entity.
     */
    static class Device {

        private String deviceId;
        private String deviceName;
        private String ipAddress;
        private String macAddress;
        private String deviceType;
        private String location;
        private String description;
        private String status;
        private String lastChecked;

        /** Full constructor */
        public Device(String deviceId, String deviceName, String ipAddress,
                      String macAddress, String deviceType, String location,
                      String description, String status, String lastChecked) {
            this.deviceId    = deviceId;
            this.deviceName  = deviceName;
            this.ipAddress   = ipAddress;
            this.macAddress  = macAddress;
            this.deviceType  = deviceType;
            this.location    = location;
            this.description = description;
            this.status      = status;
            this.lastChecked = lastChecked;
        }

        // ── Getters ──────────────────────────────────────────────

        public String getDeviceId()    { return deviceId; }
        public String getDeviceName()  { return deviceName; }
        public String getIpAddress()   { return ipAddress; }
        public String getMacAddress()  { return macAddress; }
        public String getDeviceType()  { return deviceType; }
        public String getLocation()    { return location; }
        public String getDescription() { return description; }
        public String getStatus()      { return status; }
        public String getLastChecked() { return lastChecked; }

        // ── Setters ──────────────────────────────────────────────

        public void setDeviceName(String deviceName)   { this.deviceName  = deviceName; }
        public void setIpAddress(String ipAddress)     { this.ipAddress   = ipAddress; }
        public void setMacAddress(String macAddress)   { this.macAddress  = macAddress; }
        public void setDeviceType(String deviceType)   { this.deviceType  = deviceType; }
        public void setLocation(String location)       { this.location    = location; }
        public void setDescription(String description) { this.description = description; }
        public void setStatus(String status)           { this.status      = status; }
        public void setLastChecked(String lastChecked) { this.lastChecked = lastChecked; }

        /**
         * Converts this Device into a MongoDB Document.
         */
        public Document toDocument() {
            return new Document("deviceId",    deviceId)
                    .append("deviceName",  deviceName)
                    .append("ipAddress",   ipAddress)
                    .append("macAddress",  macAddress)
                    .append("deviceType",  deviceType)
                    .append("location",    location)
                    .append("description", description)
                    .append("status",      status)
                    .append("lastChecked", lastChecked);
        }

        /**
         * Builds a Device from a MongoDB Document.
         */
        public static Device fromDocument(Document doc) {
            return new Device(
                    doc.getString("deviceId"),
                    doc.getString("deviceName"),
                    doc.getString("ipAddress"),
                    doc.getString("macAddress"),
                    doc.getString("deviceType"),
                    doc.getString("location"),
                    doc.getString("description"),
                    doc.getString("status"),
                    doc.getString("lastChecked")
            );
        }

        /**
         * Prints the device details in a formatted block.
         */
        public void print() {
            System.out.println("---------------------------------------------------");
            System.out.printf("  Device ID    : %s%n", deviceId);
            System.out.printf("  Name         : %s%n", deviceName);
            System.out.printf("  IP Address   : %s%n", ipAddress);
            System.out.printf("  MAC Address  : %s%n", macAddress);
            System.out.printf("  Type         : %s%n", deviceType);
            System.out.printf("  Location     : %s%n", location);
            System.out.printf("  Description  : %s%n", description);
            System.out.printf("  Status       : %s%n", status);
            System.out.printf("  Last Checked : %s%n", lastChecked);
            System.out.println("---------------------------------------------------");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STATIC NESTED CLASS: Database (Connection Management)
    // ─────────────────────────────────────────────────────────────

    /**
     * Manages the MongoDB connection lifecycle.
     */
    static class Database {

        private static final String CONNECTION_STRING = "mongodb://localhost:27017";
        private static final String DATABASE_NAME     = "networkDB";
        private static final String COLLECTION_NAME   = "devices";

        private MongoClient     mongoClient;
        private MongoDatabase   mongoDatabase;
        private MongoCollection<Document> collection;

        /**
         * Opens a connection to MongoDB and initialises the collection handle.
         *
         * @throws MongoException if the connection cannot be established
         */
        public void connect() {
            mongoClient    = MongoClients.create(CONNECTION_STRING);
            mongoDatabase  = mongoClient.getDatabase(DATABASE_NAME);
            collection     = mongoDatabase.getCollection(COLLECTION_NAME);

            // Trigger a lightweight command to verify connectivity immediately
            mongoDatabase.runCommand(new Document("ping", 1));
            System.out.println("[INFO] Connected to MongoDB successfully.");
        }

        /** Returns the devices collection. */
        public MongoCollection<Document> getCollection() {
            return collection;
        }

        /** Closes the MongoClient and releases resources. */
        public void close() {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("[INFO] MongoDB connection closed.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STATIC NESTED CLASS: DeviceDAO (Data Access Object)
    // ─────────────────────────────────────────────────────────────

    /**
     * Encapsulates all CRUD operations against the devices collection.
     */
    static class DeviceDAO {

        private final MongoCollection<Document> collection;

        public DeviceDAO(MongoCollection<Document> collection) {
            this.collection = collection;
        }

        // ── INSERT ────────────────────────────────────────────────

        /**
         * Inserts a new device document.
         *
         * @param device the device to persist
         * @return true if acknowledged by MongoDB, false otherwise
         */
        public boolean insert(Device device) {
            InsertOneResult result = collection.insertOne(device.toDocument());
            return result.wasAcknowledged();
        }

        // ── READ ─────────────────────────────────────────────────

        /**
         * Returns every device in the collection.
         */
        public List<Device> findAll() {
            List<Device> devices = new ArrayList<>();
            FindIterable<Document> docs = collection.find();
            for (Document doc : docs) {
                devices.add(Device.fromDocument(doc));
            }
            return devices;
        }

        /**
         * Finds a device by its unique device ID.
         *
         * @param deviceId the ID to search for
         * @return the matching Device, or null if not found
         */
        public Device findById(String deviceId) {
            Document doc = collection.find(Filters.eq("deviceId", deviceId)).first();
            return (doc != null) ? Device.fromDocument(doc) : null;
        }

        /**
         * Checks whether a device with the given ID already exists.
         */
        public boolean existsById(String deviceId) {
            return collection.find(Filters.eq("deviceId", deviceId)).first() != null;
        }

        // ── UPDATE ────────────────────────────────────────────────

        /**
         * Updates editable fields for an existing device.
         *
         * @param deviceId   the target device
         * @param deviceName new name
         * @param ipAddress  new IP
         * @param macAddress new MAC
         * @param deviceType new type
         * @param location   new location
         * @param description new description
         * @return true if at least one document was modified
         */
        public boolean update(String deviceId, String deviceName, String ipAddress,
                              String macAddress, String deviceType,
                              String location, String description) {

            Bson filter = Filters.eq("deviceId", deviceId);
            Bson updates = Updates.combine(
                    Updates.set("deviceName",  deviceName),
                    Updates.set("ipAddress",   ipAddress),
                    Updates.set("macAddress",  macAddress),
                    Updates.set("deviceType",  deviceType),
                    Updates.set("location",    location),
                    Updates.set("description", description)
            );

            UpdateResult result = collection.updateOne(filter, updates);
            return result.getModifiedCount() > 0;
        }

        /**
         * Updates the status and lastChecked timestamp for a device.
         *
         * @param deviceId    the target device
         * @param status      "Online" or "Offline"
         * @param lastChecked formatted timestamp
         * @return true if modified
         */
        public boolean updateStatus(String deviceId, String status, String lastChecked) {
            Bson filter  = Filters.eq("deviceId", deviceId);
            Bson updates = Updates.combine(
                    Updates.set("status",      status),
                    Updates.set("lastChecked", lastChecked)
            );
            UpdateResult result = collection.updateOne(filter, updates);
            return result.getModifiedCount() > 0;
        }

        // ── DELETE ────────────────────────────────────────────────

        /**
         * Removes the device with the given ID.
         *
         * @param deviceId the ID to delete
         * @return true if a document was actually deleted
         */
        public boolean delete(String deviceId) {
            DeleteResult result = collection.deleteOne(Filters.eq("deviceId", deviceId));
            return result.getDeletedCount() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STATIC NESTED CLASS: NetworkUtils (Connectivity Checking)
    // ─────────────────────────────────────────────────────────────

    /**
     * Utility class for testing network reachability.
     */
    static class NetworkUtils {

        private static final int TIMEOUT_MS = 3000;

        /**
         * Tests whether the given IP address is reachable within
         * {@value #TIMEOUT_MS} milliseconds using ICMP / TCP echo.
         *
         * @param ipAddress the IPv4 address to probe
         * @return true if reachable (Online), false otherwise (Offline)
         */
        public static boolean isReachable(String ipAddress) {
            try {
                InetAddress address = InetAddress.getByName(ipAddress);
                return address.isReachable(TIMEOUT_MS);
            } catch (Exception e) {
                // Any network error → treat device as Offline
                return false;
            }
        }

        /**
         * Returns the current timestamp as a human-readable string.
         */
        public static String currentTimestamp() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.now().format(fmt);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  APPLICATION CONSTANTS & SHARED STATE
    // ─────────────────────────────────────────────────────────────

    /** IPv4 validation pattern: each octet is 0–255. */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
    );

    private static final Scanner  scanner  = new Scanner(System.in);
    private static       Database database;
    private static       DeviceDAO deviceDAO;

    // ─────────────────────────────────────────────────────────────
    //  ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        printBanner();

        // ── Initialise MongoDB ───────────────────────────────────
        database = new Database();
        try {
            database.connect();
        } catch (Exception e) {
            System.err.println("[ERROR] Could not connect to MongoDB: " + e.getMessage());
            System.err.println("        Please ensure MongoDB is running on localhost:27017.");
            System.exit(1);
        }

        deviceDAO = new DeviceDAO(database.getCollection());

        // ── Main Menu Loop ───────────────────────────────────────
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readMenuChoice();

            switch (choice) {
                case 1 -> addDevice();
                case 2 -> viewAllDevices();
                case 3 -> searchDevice();
                case 4 -> updateDevice();
                case 5 -> deleteDevice();
                case 6 -> checkDeviceStatus();
                case 7 -> checkAllDevices();
                case 8 -> {
                    running = false;
                    exitApplication();
                }
                default -> System.out.println("[WARN] Invalid choice. Please enter a number between 1 and 8.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  MENU DISPLAY HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Prints the application banner on start-up. */
    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║   NETWORK DEVICE INVENTORY & MONITORING SYSTEM   ║");
        System.out.println("  ║             Powered by MongoDB  •  Java 17       ║");
        System.out.println("  ╚══════════════════════════════════════════════════╝");
        System.out.println();
    }

    /** Prints the main menu. */
    private static void printMenu() {
        System.out.println();
        System.out.println("  =============================================");
        System.out.println("   NETWORK DEVICE INVENTORY SYSTEM");
        System.out.println("  =============================================");
        System.out.println("   1. Add Device");
        System.out.println("   2. View All Devices");
        System.out.println("   3. Search Device");
        System.out.println("   4. Update Device");
        System.out.println("   5. Delete Device");
        System.out.println("   6. Check Device Status");
        System.out.println("   7. Check All Devices");
        System.out.println("   8. Exit");
        System.out.println("  =============================================");
        System.out.print("  Enter Choice: ");
    }

    /**
     * Reads and returns an integer menu choice.
     * Returns -1 on non-numeric input.
     */
    private static int readMenuChoice() {
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 1 – ADD DEVICE
    // ─────────────────────────────────────────────────────────────

    /**
     * Prompts for device details, validates input,
     * checks for duplicate IDs, then inserts into MongoDB.
     */
    private static void addDevice() {
        System.out.println("\n  [ ADD NEW DEVICE ]");
        System.out.println("  ------------------------------------------");

        String deviceId = readNonBlank("  Device ID       : ");

        // ── Duplicate check ──────────────────────────────────────
        if (deviceDAO.existsById(deviceId)) {
            System.out.println("  [ERROR] A device with ID '" + deviceId + "' already exists.");
            return;
        }

        String deviceName  = readNonBlank("  Device Name     : ");
        String ipAddress   = readValidIp ("  IP Address      : ");
        String macAddress  = readOptional("  MAC Address     : (optional, press Enter to skip) ", "N/A");
        String deviceType  = readNonBlank("  Device Type     : ");
        String location    = readNonBlank("  Location        : ");
        String description = readNonBlank("  Description     : ");

        Device device = new Device(
                deviceId, deviceName, ipAddress, macAddress,
                deviceType, location, description,
                "Unknown",  // initial status
                "Never"     // initial lastChecked
        );

        try {
            boolean ok = deviceDAO.insert(device);
            if (ok) {
                System.out.println("\n  ✔  Device Added Successfully.");
            } else {
                System.out.println("  [ERROR] Insert was not acknowledged by MongoDB.");
            }
        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 2 – VIEW ALL DEVICES
    // ─────────────────────────────────────────────────────────────

    /** Retrieves and displays every device in the collection. */
    private static void viewAllDevices() {
        System.out.println("\n  [ ALL DEVICES ]");

        try {
            List<Device> devices = deviceDAO.findAll();
            if (devices.isEmpty()) {
                System.out.println("  No devices found.");
                return;
            }

            System.out.printf("%n  Total devices: %d%n", devices.size());
            for (Device d : devices) {
                d.print();
            }

        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 3 – SEARCH DEVICE
    // ─────────────────────────────────────────────────────────────

    /** Searches for a device by Device ID and prints its details. */
    private static void searchDevice() {
        System.out.println("\n  [ SEARCH DEVICE ]");
        System.out.println("  ------------------------------------------");

        String deviceId = readNonBlank("  Enter Device ID : ");

        try {
            Device device = deviceDAO.findById(deviceId);
            if (device == null) {
                System.out.println("  Device Not Found.");
            } else {
                System.out.println("\n  Device Found:");
                device.print();
            }
        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 4 – UPDATE DEVICE
    // ─────────────────────────────────────────────────────────────

    /**
     * Looks up a device by ID and allows editing of all fields
     * except the Device ID itself.
     */
    private static void updateDevice() {
        System.out.println("\n  [ UPDATE DEVICE ]");
        System.out.println("  ------------------------------------------");

        String deviceId = readNonBlank("  Enter Device ID : ");

        try {
            Device existing = deviceDAO.findById(deviceId);
            if (existing == null) {
                System.out.println("  Device Not Found.");
                return;
            }

            System.out.println("  Current details:");
            existing.print();
            System.out.println("  Enter new values (press Enter to keep current):");

            String deviceName  = readWithDefault("  Device Name  [" + existing.getDeviceName()  + "] : ", existing.getDeviceName());
            String ipAddress   = readUpdatedIp   ("  IP Address   [" + existing.getIpAddress()   + "] : ", existing.getIpAddress());
            String macAddress  = readWithDefault("  MAC Address  [" + existing.getMacAddress()  + "] (optional, Enter to keep) : ", existing.getMacAddress());
            String deviceType  = readWithDefault("  Device Type  [" + existing.getDeviceType()  + "] : ", existing.getDeviceType());
            String location    = readWithDefault("  Location     [" + existing.getLocation()    + "] : ", existing.getLocation());
            String description = readWithDefault("  Description  [" + existing.getDescription() + "] : ", existing.getDescription());

            boolean ok = deviceDAO.update(deviceId, deviceName, ipAddress,
                                          macAddress, deviceType, location, description);
            if (ok) {
                System.out.println("\n  ✔  Device Updated Successfully.");
            } else {
                System.out.println("  [WARN] No changes were applied (values may be identical).");
            }

        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 5 – DELETE DEVICE
    // ─────────────────────────────────────────────────────────────

    /** Deletes a device by Device ID after confirmation. */
    private static void deleteDevice() {
        System.out.println("\n  [ DELETE DEVICE ]");
        System.out.println("  ------------------------------------------");

        String deviceId = readNonBlank("  Enter Device ID : ");

        // Show device first so the user knows what they're deleting
        try {
            Device existing = deviceDAO.findById(deviceId);
            if (existing == null) {
                System.out.println("  Device Not Found.");
                return;
            }

            System.out.println("  Device to be deleted:");
            existing.print();

            System.out.print("  Confirm deletion? (yes/no) : ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("yes") && !confirm.equals("y")) {
                System.out.println("  Deletion cancelled.");
                return;
            }

            boolean deleted = deviceDAO.delete(deviceId);
            if (deleted) {
                System.out.println("\n  ✔  Device Deleted Successfully.");
            } else {
                System.out.println("  [ERROR] Could not delete the device.");
            }

        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 6 – CHECK DEVICE STATUS (single)
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks the reachability of one device by Device ID,
     * updates MongoDB, and displays the result.
     */
    private static void checkDeviceStatus() {
        System.out.println("\n  [ CHECK DEVICE STATUS ]");
        System.out.println("  ------------------------------------------");

        String deviceId = readNonBlank("  Enter Device ID : ");

        try {
            Device device = deviceDAO.findById(deviceId);
            if (device == null) {
                System.out.println("  Device Not Found.");
                return;
            }

            System.out.println("\n  Checking Device...");
            System.out.println("  Device Name : " + device.getDeviceName());
            System.out.println("  IP Address  : " + device.getIpAddress());

            boolean online    = NetworkUtils.isReachable(device.getIpAddress());
            String  status    = online ? "Online" : "Offline";
            String  timestamp = NetworkUtils.currentTimestamp();

            deviceDAO.updateStatus(deviceId, status, timestamp);

            if (online) {
                System.out.println("  Status      : ✔  ONLINE");
            } else {
                System.out.println("  Status      : ✖  OFFLINE");
            }
            System.out.println("  Checked At  : " + timestamp);

        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 7 – CHECK ALL DEVICES
    // ─────────────────────────────────────────────────────────────

    /**
     * Iterates over every stored device, probes each IP address,
     * updates statuses in MongoDB, and prints a summary table.
     */
    private static void checkAllDevices() {
        System.out.println("\n  [ CHECK ALL DEVICES ]");
        System.out.println("  ------------------------------------------");

        try {
            List<Device> devices = deviceDAO.findAll();
            if (devices.isEmpty()) {
                System.out.println("  No devices found.");
                return;
            }

            System.out.println("  Checking Devices...\n");

            String timestamp = NetworkUtils.currentTimestamp();
            int onlineCount  = 0;
            int offlineCount = 0;

            for (Device device : devices) {
                boolean online = NetworkUtils.isReachable(device.getIpAddress());
                String  status = online ? "Online" : "Offline";

                deviceDAO.updateStatus(device.getDeviceId(), status, timestamp);

                // Format the output line with dots padding
                String label  = device.getDeviceName();
                String marker = online ? "ONLINE" : "OFFLINE";
                System.out.printf("  %-30s %s%n",
                        padWithDots(label, 30), marker);

                if (online) onlineCount++; else offlineCount++;
            }

            System.out.println();
            System.out.println("  ------------------------------------------");
            System.out.printf("  Online : %d   |   Offline : %d%n", onlineCount, offlineCount);
            System.out.println("  ------------------------------------------");
            System.out.println("  Scan Completed.");

        } catch (MongoException e) {
            System.out.println("  [ERROR] Database error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FEATURE 8 – EXIT
    // ─────────────────────────────────────────────────────────────

    /** Closes resources and prints a farewell message. */
    private static void exitApplication() {
        System.out.println();
        System.out.println("  =============================================");
        System.out.println("   Thank you for using Network Device");
        System.out.println("   Inventory System.");
        System.out.println("  =============================================");
        database.close();
        scanner.close();
    }

    // ─────────────────────────────────────────────────────────────
    //  INPUT HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Keeps prompting until a non-blank value is entered.
     *
     * @param prompt the message to display
     * @return the trimmed, non-empty input string
     */
    private static String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  [WARN] Input cannot be blank. Please try again.");
        }
    }

    /**
     * Reads an optional field; if blank, returns {@code defaultValue} silently.
     * No warning is shown — blank is explicitly allowed.
     *
     * @param prompt       the message to display
     * @param defaultValue the value to use when the user presses Enter
     * @return the trimmed input, or defaultValue if blank
     */
    private static String readOptional(String prompt, String defaultValue) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    /**
     * Keeps prompting until a valid IPv4 address is entered.
     *
     * @param prompt the message to display
     * @return a valid IPv4 address string
     */
    private static String readValidIp(String prompt) {
        while (true) {
            System.out.print(prompt);
            String ip = scanner.nextLine().trim();
            if (ip.isEmpty()) {
                System.out.println("  [WARN] IP Address cannot be blank.");
            } else if (!isValidIpv4(ip)) {
                System.out.println("  [WARN] '" + ip + "' is not a valid IPv4 address.");
            } else {
                return ip;
            }
        }
    }

    /**
     * Reads an optional update value; if blank, returns the current value.
     *
     * @param prompt       the message to display
     * @param currentValue the existing value to keep if Enter is pressed
     * @return the new value or currentValue if blank
     */
    private static String readWithDefault(String prompt, String currentValue) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? currentValue : value;
    }

    /**
     * Reads an updated IPv4; if blank keeps current. Validates non-blank input.
     *
     * @param prompt       the prompt message
     * @param currentValue existing IP address
     * @return a valid IPv4 address
     */
    private static String readUpdatedIp(String prompt, String currentValue) {
        while (true) {
            System.out.print(prompt);
            String ip = scanner.nextLine().trim();
            if (ip.isEmpty()) {
                return currentValue; // keep existing
            }
            if (isValidIpv4(ip)) {
                return ip;
            }
            System.out.println("  [WARN] '" + ip + "' is not a valid IPv4 address. Try again.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  VALIDATION HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Validates that the given string is a properly formatted IPv4 address.
     *
     * @param ip the string to validate
     * @return true if valid IPv4, false otherwise
     */
    private static boolean isValidIpv4(String ip) {
        return ip != null && IPV4_PATTERN.matcher(ip).matches();
    }

    // ─────────────────────────────────────────────────────────────
    //  FORMATTING HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Pads a label with dots to reach the desired total width, e.g.
     * {@code padWithDots("Router", 20)} → {@code "Router.............."}
     *
     * @param label     the text to pad
     * @param totalWidth the target total width including the label
     * @return the padded string
     */
    private static String padWithDots(String label, int totalWidth) {
        if (label.length() >= totalWidth) {
            return label;
        }
        StringBuilder sb = new StringBuilder(label);
        while (sb.length() < totalWidth) {
            sb.append('.');
        }
        return sb.toString();
    }
}
