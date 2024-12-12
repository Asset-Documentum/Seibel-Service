package com.asset.voda;

import java.io.File;
import java.nio.file.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Seibel class is responsible for handling the upload of documents to Documentum.
 * It processes metadata, manages successful and failed uploads, and generates reports.
 */
public class Seibel {

    private static final Logger logger = LoggerFactory.getLogger(Seibel.class);

    // Authentication credentials for Documentum repository
    private final String USERNAME = PathsConfig.USERNAME;
    private final String PASS = PathsConfig.PASS;

    // Lists to store metadata for successful and failed uploads
    private final List<Map<String, Object>> uploadedMetadata = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> failedMetadata = Collections.synchronizedList(new ArrayList<>());

    // Gson instance for converting objects to JSON and formatting it nicely
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Processes a document, uploads it to Documentum, and manages its metadata.
     * If upload is successful, it is moved to the 'processed' folder;
     * if it fails, it is moved to the 'failed' folder after retrying up to 3 times.
     * Generates Excel reports for both successful and failed uploads.
     *
     * @param document The document file to upload.
     * @param objectType The type of the document (e.g., "document").
     * @param documentName The name of the document.
     * @param contractNo The contract number associated with the document.
     * @param sfid The SFID of the document.
     * @param source The source of the document.
     * @param customerName The customer name associated with the document.
     * @param customerId The customer ID.
     * @param customerAccount The customer account number.
     * @param boxNo The box number associated with the document.
     * @param mobileNo The mobile number linked to the document.
     * @param departmentCode The department code for the document.
     * @param deleteFlag Flag indicating if the document should be deleted.
     * @param status The status of the document.
     * @param subDepartmentCode The sub-department code.
     * @param simNo The SIM number.
     * @param comments Any additional comments related to the document.
     * @param folderPath The path to the folder where the document resides.
     */
    public void handleDocument(File document, String objectType, String documentName, String contractNo, String sfid,
                               String source, String customerName, String customerId, String customerAccount,
                               String boxNo, String mobileNo, String departmentCode, String deleteFlag, String status,
                               String subDepartmentCode, String simNo, String comments, Path folderPath) {
        try {
            // Create instances to track document status and machine details
            DocumentsStatus documentsStatus = DocumentsStatus.getInstance();
            MachineDetails machineDetails = MachineDetails.getInstance();

            // Create metadata map for the document
            Map<String, Object> metadata = createMetadata(objectType, documentName, contractNo, sfid, source, customerName,
                    customerId, customerAccount, boxNo, mobileNo, departmentCode,
                    deleteFlag, status, subDepartmentCode, simNo, comments);

            // Extract the folder name (e.g., "17-10-2024")
            String folderName = folderPath.getFileName().toString();

            // Create new directories in PROCESSED or FAILED based on the folder name
            Path processedFolder = Paths.get(PathsConfig.PROCESSED, folderName);
            Path failedFolder = Paths.get(PathsConfig.FAILED, folderName);

            Files.createDirectories(processedFolder);
            Files.createDirectories(failedFolder);

            File processedReport = new File(processedFolder.toFile(), "Data.xlsx");
            File failedReport = new File(failedFolder.toFile(), "Data.xlsx");

            // Try uploading the document to Documentum
            boolean uploadSuccess = uploadToDocumentum(document, metadata);

            // If upload is successful, process it accordingly
            if (uploadSuccess) {
                uploadedMetadata.add(metadata);
                documentsStatus.incrementSuccess();
                logger.info("Successfully uploaded: {}", document.getName());
                machineDetails.databaseConnection(objectType, documentName + " - " + mobileNo, "Success");

                // Combine folder path and the "Documents" directory to get the full path.
                Path documentsPath = folderPath.resolve("Documents");

                // Create a File object for the specific document you want to delete.
                File toBeProcessedDoc = new File(documentsPath.toString(), document.getName());

                // Check if the file exists and delete it.
                if (toBeProcessedDoc.exists()) {
                    toBeProcessedDoc.delete();
                }
            } else {
                failedMetadata.add(metadata);
                documentsStatus.incrementFailure();

                try {
                    int retries = 3;
                    while (retries > 0 && !uploadSuccess) {
                        uploadSuccess = uploadToDocumentum(document, metadata);
                        if (!uploadSuccess) {
                            retries--;
                            logger.warn("Retrying upload for: {}. Attempts left: {}", document.getName(), retries);
                        }
                    }

                    // Move the document to the failed folder after failed retries
                    Path documentsFolder = failedFolder.resolve("Documents");
                    moveFile(new File(folderPath.resolve("Documents").toString(), document.getName()), documentsFolder);

                    logger.error("Failed to upload: {}", document.getName());
                    machineDetails.databaseConnection(objectType, documentName + " - " + mobileNo, "Fail");

                } catch (Exception e) {
                    logger.error("Error handling failed upload for document: {}", document.getName(), e);
                }
            }

            // Generate Excel reports for processed and failed documents
            ExcelReader.generateExcelReport(uploadedMetadata, processedReport);
            ExcelReader.generateExcelReport(failedMetadata, failedReport);

            logger.info("Reports generated: Processed -> {}, Failed -> {}", processedReport.getAbsolutePath(), failedReport.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Error processing document: {}", document.getName(), e);
        }
    }

    /**
     * Creates a metadata map for the document to be uploaded to Documentum.
     * @return A map containing metadata for the document.
     */
    private Map<String, Object> createMetadata(String objectType, String documentName, String contractNo, String sfid,
                                               String source, String customerName, String customerId, String customerAccount,
                                               String boxNo, String mobileNo, String departmentCode, String deleteFlag,
                                               String status, String subDepartmentCode, String simNo, String comments) {

        Map<String, Object> metadata = new HashMap<>();

        // Regular string values
        metadata.put("object_name", documentName);
        metadata.put("r_object_type", objectType);
        metadata.put("cch_contract_no", contractNo);
        metadata.put("cch_sfid", sfid);
        metadata.put("cch_source", source);
        metadata.put("cch_customer_name", customerName);
        metadata.put("cch_customer_id", customerId);
        metadata.put("cch_box_no", boxNo);
        metadata.put("cch_department_code", departmentCode);
        metadata.put("deleteflag", deleteFlag);
        metadata.put("cch_status", status);
        metadata.put("cch_comments", comments);
        metadata.put("cch_sub_department_code", subDepartmentCode);

        // Convert list values into ArrayList
        if (simNo != null && !simNo.isEmpty()) {
            ArrayList<String> simNos = new ArrayList<>();
            simNos.add(simNo);
            metadata.put("cch_sim_no", simNos);
        } else {
            metadata.put("cch_sim_no", new ArrayList<String>());  // Empty list if no simNo
        }

        if (mobileNo != null && !mobileNo.isEmpty()) {
            ArrayList<String> mobileNos = new ArrayList<>();
            mobileNos.add(mobileNo);
            metadata.put("cch_mobile_no", mobileNos);
        } else {
            metadata.put("cch_mobile_no", new ArrayList<String>());  // Empty list if no mobileNo
        }

        // Treat cch_customer_account as a String, but ensure it retains decimal precision
        if (customerAccount != null && !customerAccount.isEmpty()) {
            metadata.put("cch_customer_account", customerAccount);  // Store as String, e.g. "1.34086595"
        } else {
            metadata.put("cch_customer_account", "0.00000000");  // Default to zero if empty
        }

        return metadata;
    }

    /**
     * Uploads the document to Documentum repository using a REST API.
     *
     * @param document The document file to upload.
     * @param metadata The metadata associated with the document.
     * @return A boolean indicating whether the upload was successful or not.
     */
    private boolean uploadToDocumentum(File document, Map<String, Object> metadata) {
        try {
            // URL of the Documentum repository
            String url = "http://10.0.40.26:8080/dctm-rest/repositories/VFREPO/folders/0b0001c880008201/documents";

            // Encrypt password and create authorization header
            String decryptedPassword = MachineDetails.decryptPassword(PASS);
            String auth = USERNAME + ":" + decryptedPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Convert metadata to JSON
            String metadataJson = gson.toJson(Collections.singletonMap("properties", metadata));

            // Determine the content type of the file
            String mimeType = Files.probeContentType(document.toPath());
            ContentType fileType = mimeType != null ? ContentType.create(mimeType) : ContentType.APPLICATION_OCTET_STREAM;

            // Build the HTTP request body with metadata and file content
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("metadata", metadataJson, ContentType.APPLICATION_JSON);
            builder.addBinaryBody("content", document, fileType, document.getName());

            // Send the request to Documentum
            HttpResponse response = Request.post(url)
                    .addHeader("Authorization", "Basic " + encodedAuth)
                    .body(builder.build())
                    .addHeader("Accept", "application/json")
                    .execute()
                    .returnResponse();

            // Return true if the response code is 200 (success) or 201 (created)
            return response.getCode() == 200 || response.getCode() == 201;
        } catch (Exception e) {
            logger.error("Error uploading document: {}", document.getName(), e);
            return false;
        }
    }

    /**
     * Moves a file from its current location to a target folder.
     * If the target folder does not exist, it is created.
     * The file is moved with the option to replace any existing file at the target location.
     *
     * @param file The file to be moved.
     * @param targetFolder The folder where the file should be moved to.
     * @throws Exception If an error occurs during the file move operation.
     */
    private void moveFile(File file, Path targetFolder) throws Exception {
        Path sourcePath = file.toPath();
        Path targetPath = targetFolder.resolve(file.getName());

        Files.createDirectories(targetFolder);
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Moved file: {} to {}", sourcePath, targetPath);
    }
}
