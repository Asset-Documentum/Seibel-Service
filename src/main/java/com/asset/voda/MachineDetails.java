package com.asset.voda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.spec.KeySpec;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A singleton class for retrieving machine-specific details and managing database audit records.
 * This class handles machine metadata, database connections, and secure password decryption.
 */

public class MachineDetails {

    private static final MachineDetails instance;

    static {
        try {
            instance = new MachineDetails();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MachineDetails.class);

    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "YourSecretKey"; // Change this to your secret key

    private Connection connection = null;
    private PreparedStatement preparedStatement;
    private InetAddress ipv4Address;
    private String windowsUsername;
    private NetworkInterface networkInterface;
    private byte[] macBytes;
    private StringBuilder macAddress;
    private String machineName;

    // Get the singleton instance
    public static MachineDetails getInstance() {
        return instance;
    }

    private MachineDetails() throws Exception {
        try {
            String DB_URL = PathsConfig.DB_URL;
            String USER = PathsConfig.USER;
            String PASSWORD = PathsConfig.PASSWORD;
            String decryptedPassword = decryptPassword(PASSWORD);

            // SQL INSERT query
            String insertQuery = "INSERT INTO AuditDmChanges (Module,WindowsUserName,AppUserName,UserMachineName,UserMachineIPv4,MAC,ActionName,UserAction,ActionAttribute,AffectedCustomer,ActionStatus,TimeStamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Load Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Establish a connection
            connection = DriverManager.getConnection(DB_URL, USER, decryptedPassword);

            preparedStatement = connection.prepareStatement(insertQuery);

            // Get the IPv4 address
            ipv4Address = InetAddress.getLocalHost();

            // Get the Windows username
            windowsUsername = System.getProperty("user.name");

            // Get the MAC address
            networkInterface = NetworkInterface.getByInetAddress(ipv4Address);
            macBytes = networkInterface.getHardwareAddress();
            macAddress = new StringBuilder();
            for (int i = 0; i < macBytes.length; i++) {
                macAddress.append(String.format("%02X%s", macBytes[i], (i < macBytes.length - 1) ? "-" : ""));
            }

            // Get the machine name
            machineName = ipv4Address.getHostName();

        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error during initialization or database connection setup: {}", e.getMessage(), e);
        }
    }

    // Method to decrypt the encrypted password which in config.properties
    public static String decryptPassword(String encryptedPassword) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedPassword);

        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SECRET_KEY.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }

    // Method to that audit the action details in Oracle database
    public void databaseConnection(String docType, String affectedCustomer, String status) {
        try {
            // Get the current timestamp
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = currentDateTime.format(formatter);

            preparedStatement.setString(1, "Seibel-Service");
            preparedStatement.setString(2, windowsUsername);
            preparedStatement.setString(3, "Seibel-user");
            preparedStatement.setString(4, machineName);
            preparedStatement.setString(5, ipv4Address.getHostAddress());
            preparedStatement.setString(6, macAddress.toString());
            preparedStatement.setString(7, "Uploading");
            preparedStatement.setString(8, "Uploading");
            preparedStatement.setString(9, docType);
            preparedStatement.setString(10, affectedCustomer);
            preparedStatement.setString(11, status);
            preparedStatement.setString(12, timestamp);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error inserting audit record: {}", e.getMessage(), e);
        }
    }

    // Method to close the connection with database after the end of audit action
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Connection closed successfully!");
            }
        } catch (SQLException e) {
            logger.error("Error closing connection: {}", e.getMessage(), e);
        }
    }
}
