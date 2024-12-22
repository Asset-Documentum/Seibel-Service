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

    private final String USERNAME = PathsConfig.USERNAME;
    private final String PASS = PathsConfig.PASS;

    private final List<Map<String, Object>> uploadedMetadata = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> failedMetadata = Collections.synchronizedList(new ArrayList<>());

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Processes a document using MetadataDTO, uploads it to Documentum, and manages its status.
     *
     * @param document The document file to upload.
     * @param metadataDTO The metadata DTO object containing metadata information.
     * @param folderPath The path to the folder where the document resides.
     */
    public void handleDocument(File document, MetadataDTO metadataDTO, Path folderPath) {
        try {
            DocumentsStatus documentsStatus = DocumentsStatus.getInstance();
            MachineDetails machineDetails = MachineDetails.getInstance();

            Map<String, Object> metadata = createMetadata(metadataDTO);

            String folderName = folderPath.getFileName().toString();
            Path processedFolder = Paths.get(PathsConfig.PROCESSED, folderName);
            Path failedFolder = Paths.get(PathsConfig.FAILED, folderName);

            Files.createDirectories(processedFolder);
            Files.createDirectories(failedFolder);

            File processedReport = new File(processedFolder.toFile(), "Data.xlsx");
            File failedReport = new File(failedFolder.toFile(), "Data.xlsx");

            boolean uploadSuccess = uploadToDocumentum(document, metadata);

            if (uploadSuccess) {
                uploadedMetadata.add(metadata);
                documentsStatus.incrementSuccess();
                logger.info("Successfully uploaded: {}", document.getName());
                machineDetails.databaseConnection(metadataDTO.getR_object_type(), metadataDTO.getObject_name(), "Success");

                Path documentsPath = folderPath.resolve("Documents");
                File toBeProcessedDoc = new File(documentsPath.toString(), document.getName());
                if (toBeProcessedDoc.exists()) {
                    toBeProcessedDoc.delete();
                }
            } else {
                failedMetadata.add(metadata);
                documentsStatus.incrementFailure();

                int retries = 3;
                while (retries > 0 && !uploadSuccess) {
                    uploadSuccess = uploadToDocumentum(document, metadata);
                    retries--;
                    logger.warn("Retrying upload for: {}. Attempts left: {}", document.getName(), retries);
                }

                if (!uploadSuccess) {
                    Path documentsFolder = failedFolder.resolve("Documents");
                    moveFile(new File(folderPath.resolve("Documents").toString(), document.getName()), documentsFolder);
                    logger.error("Failed to upload: {}", document.getName());
                    machineDetails.databaseConnection(metadataDTO.getR_object_type(), metadataDTO.getObject_name(), "Fail");
                }
            }

            ExcelReader.generateExcelReport(uploadedMetadata, processedReport);
            ExcelReader.generateExcelReport(failedMetadata, failedReport);

            logger.info("Reports generated: Processed -> {}, Failed -> {}", processedReport.getAbsolutePath(), failedReport.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Error processing document: {}", document.getName(), e);
        }
    }

    private Map<String, Object> createMetadata(MetadataDTO metadataDTO) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("object_name", metadataDTO.getObject_name());
        metadata.put("r_object_type", metadataDTO.getR_object_type());
        metadata.put("cch_contract_no", metadataDTO.getCch_contract_no());
        metadata.put("cch_sfid", metadataDTO.getCch_sfid());
        metadata.put("cch_source", metadataDTO.getCch_source());
        metadata.put("cch_customer_name", metadataDTO.getCch_customer_name());
        metadata.put("cch_customer_id", metadataDTO.getCch_customer_id());
        metadata.put("cch_box_no", metadataDTO.getCch_box_no());
        metadata.put("cch_department_code", metadataDTO.getCch_department_code());
        metadata.put("deleteflag", metadataDTO.getDeleteflag());
        metadata.put("cch_status", metadataDTO.getCch_status());
        metadata.put("cch_comments", metadataDTO.getCch_comments());
        metadata.put("cch_sub_department_code", metadataDTO.getCch_sub_department_code());
        metadata.put("cch_sim_no", metadataDTO.getCch_sim_no());
        metadata.put("cch_mobile_no", metadataDTO.getCch_mobile_no());
        metadata.put("cch_customer_account", metadataDTO.getCch_customer_account());
        return metadata;
    }

    private boolean uploadToDocumentum(File document, Map<String, Object> metadata) {
        try {
            String url = "http://10.0.40.26:8080/dctm-rest/repositories/VFREPO/folders/0b0001c8800084fc/documents";

            String decryptedPassword = MachineDetails.decryptPassword(PASS);
            String auth = USERNAME + ":" + decryptedPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            String metadataJson = gson.toJson(Collections.singletonMap("properties", metadata));

            String mimeType = Files.probeContentType(document.toPath());
            ContentType fileType = mimeType != null ? ContentType.create(mimeType) : ContentType.APPLICATION_OCTET_STREAM;

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("metadata", metadataJson, ContentType.APPLICATION_JSON);
            builder.addBinaryBody("content", document, fileType, document.getName());

            HttpResponse response = Request.post(url)
                    .addHeader("Authorization", "Basic " + encodedAuth)
                    .body(builder.build())
                    .addHeader("Accept", "application/json")
                    .execute()
                    .returnResponse();

            return response.getCode() == 200 || response.getCode() == 201;
        } catch (Exception e) {
            logger.error("Error uploading document: {}", document.getName(), e);
            return false;
        }
    }

    private void moveFile(File file, Path targetFolder) throws Exception {
        Path sourcePath = file.toPath();
        Path targetPath = targetFolder.resolve(file.getName());

        Files.createDirectories(targetFolder);
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Moved file: {} to {}", sourcePath, targetPath);
    }
}
