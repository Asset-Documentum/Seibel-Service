# Seibel-Service
This project provides a service for processing and uploading documents to Documentum, managing the upload status, and generating reports based on the success or failure of each document upload. The service retrieves metadata, processes documents, and generates reports that track uploaded and failed documents.

Features
Document Processing: Handles documents in folders named by the current date.
Metadata Creation: Dynamically generates metadata for each document, including customer information and document details.
Upload to Documentum: Uploads documents to a specified Documentum repository.
Retries on Failure: Retries uploading a document up to three times in case of failure.
Reports Generation: Creates Excel reports for processed and failed documents.
Logging: Logs detailed information about each document upload process, including success and failure details.

Configuration
The service requires a configuration file PathsConfig.properties to define paths for processing documents and storing reports.

Sample PathsConfig.properties:

url=jdbc:oracle:thin:@//IP:Port/service-name
user=your_user
password=your_encrpted_password

// authentication on DA
username=your_username
pass=your_encrpted_password

to-be_processed=/path/to/To-Be Processed
in_progress=/path/to/In-Progress
processed=/path/to/Processed
failed=/path/to/Failed


Usage
1. Set up PathsConfig
Before running the service, ensure that the PathsConfig.properties file is properly configured with the correct paths and credentials.

2. Running the Service
To run the document upload service, you will need to call the watchFolder method of the FolderWatcher class, passing the required parameters:

3. Document Upload Process
The service will:

  1- Get Metadata: For each document, metadata will be generated based on the provided details.
  2- Upload to Documentum: The document and its metadata will be uploaded to the Documentum repository.
  3- Retry Logic: If the upload fails, the service will retry the upload up to three times.
  4- Move Documents: After successful upload, documents will be deleted from the "to-be-processed" folder. If the upload fails, the document will be moved to a         "failed" folder.
  5- Generate Reports: The service will generate two Excel reports:
    Processed Report: For successfully uploaded documents.
    Failed Report: For documents that failed to upload after retries.
4. Viewing the Reports
After the service has completed, the following reports will be generated:

Processed Report: Located in the /processed/{folderName}/Data.xlsx directory.
Failed Report: Located in the /failed/{folderName}/Data.xlsx directory.
These reports will contain the metadata of the documents that were either successfully uploaded or failed.

