package com.datalinks.media.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.media.MediaConfiguration;
import com.microsoft.windowsazure.services.media.MediaContract;
import com.microsoft.windowsazure.services.media.MediaService;
import com.microsoft.windowsazure.services.media.WritableBlobContainerContract;
import com.microsoft.windowsazure.services.media.models.AccessPolicy;
import com.microsoft.windowsazure.services.media.models.AccessPolicyInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicyPermission;
import com.microsoft.windowsazure.services.media.models.Asset;
import com.microsoft.windowsazure.services.media.models.AssetFile;
import com.microsoft.windowsazure.services.media.models.AssetFileInfo;
import com.microsoft.windowsazure.services.media.models.AssetInfo;
import com.microsoft.windowsazure.services.media.models.Job;
import com.microsoft.windowsazure.services.media.models.JobInfo;
import com.microsoft.windowsazure.services.media.models.JobState;
import com.microsoft.windowsazure.services.media.models.ListResult;
import com.microsoft.windowsazure.services.media.models.Locator;
import com.microsoft.windowsazure.services.media.models.LocatorInfo;
import com.microsoft.windowsazure.services.media.models.LocatorType;
import com.microsoft.windowsazure.services.media.models.MediaProcessor;
import com.microsoft.windowsazure.services.media.models.MediaProcessorInfo;
import com.microsoft.windowsazure.services.media.models.Task;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chris
 */
public class AmsMediaHandler {
    
    final static Logger logger = Logger.getLogger("AmsMediaHandler");


    // Media Services account credentials configuration
    private static final String mediaServiceUri = "https://media.windows.net/API/";
    private static final String oAuthUri = "https://wamsprodglobal001acs.accesscontrol.windows.net/v2/OAuth2-13";
    private static final String clientId = "tstmediaservice1";
    private static final String scope = "urn:WindowsAzureMediaServices";
    private static MediaContract mediaService;

    // Encoder configuration
    private static final String preferedEncoder = "Media Encoder Standard";
    private static final String encodingPreset = "H264 Multiple Bitrate 720p";

    
    public static String uploadMediaToAMS(String clientSecret, String file){
        
        String url = null;
        AssetInfo uploadAsset = null;

        logger.log(Level.INFO, "Trying to upload file {0} with key: {1} ",new Object[]{file,clientSecret});

        try {
            // Set up the MediaContract object to call into the Media Services account
            Configuration configuration = MediaConfiguration.configureWithOAuthAuthentication(
            mediaServiceUri, oAuthUri, clientId, clientSecret, scope);
            mediaService = MediaService.create(configuration);

            // Upload a local file to an Asset
            uploadAsset = uploadFileAndCreateAsset(file);            
            logger.log(Level.INFO, "Uploaded Asset Id:  {0} ",uploadAsset.getId());

            // Transform the Asset
            AssetInfo encodedAsset = encode(uploadAsset);
            logger.log(Level.INFO, "Encoded Asset Id:  {0} ",encodedAsset.getId());

            
            // Create the Streaming Origin Locator
            url = getStreamingOriginLocator(encodedAsset);

            logger.log(Level.INFO, "Origin Locator URL:  {0} ",url);
            logger.log(Level.INFO, "Sample completed");

        } catch (FileNotFoundException | ServiceException | InterruptedException | NoSuchAlgorithmException se) {
            logger.log(Level.SEVERE, "Exception occured:  {0} ",se.getMessage());
        }
        return url;
    }

    private static AssetInfo uploadFileAndCreateAsset(String fileName)
        throws ServiceException, FileNotFoundException, NoSuchAlgorithmException {

        WritableBlobContainerContract uploader;
        AssetInfo resultAsset;
        AccessPolicyInfo uploadAccessPolicy;
        LocatorInfo uploadLocator = null;

        // Create an Asset
        resultAsset = mediaService.create(Asset.create().setName(fileName).setAlternateId("altId"));
        logger.log(Level.SEVERE, "Created Asset:  {0} ",fileName);

        // Create an AccessPolicy that provides Write access for 15 minutes
        uploadAccessPolicy = mediaService
            .create(AccessPolicy.create("uploadAccessPolicy", 15.0, EnumSet.of(AccessPolicyPermission.WRITE)));

        // Create a Locator using the AccessPolicy and Asset
        uploadLocator = mediaService
            .create(Locator.create(uploadAccessPolicy.getId(), resultAsset.getId(), LocatorType.SAS));

        // Create the Blob Writer using the Locator
        uploader = mediaService.createBlobWriter(uploadLocator);

        File file = new File("upload-dir/"+fileName); 

        // The local file that will be uploaded to your Media Services account
        InputStream input = new FileInputStream(file);

        logger.log(Level.SEVERE, "Uploading:  {0} ",fileName);

        // Upload the local file to the asset
        uploader.createBlockBlob(fileName, input);

        // Inform Media Services about the uploaded files
        mediaService.action(AssetFile.createFileInfos(resultAsset.getId()));
        logger.log(Level.SEVERE, "Uploaded Asset File:  {0} ",fileName);

        
        mediaService.delete(Locator.delete(uploadLocator.getId()));
        mediaService.delete(AccessPolicy.delete(uploadAccessPolicy.getId()));

        return resultAsset;
    }

    // Create a Job that contains a Task to transform the Asset
    private static AssetInfo encode(AssetInfo assetToEncode) throws ServiceException, InterruptedException {

        // Retrieve the list of Media Processors that match the name
        ListResult<MediaProcessorInfo> mediaProcessors = mediaService
                        .list(MediaProcessor.list().set("$filter", String.format("Name eq '%s'", preferedEncoder)));

        // Use the latest version of the Media Processor
        MediaProcessorInfo mediaProcessor = null;
        for (MediaProcessorInfo info : mediaProcessors) {
            if (null == mediaProcessor || info.getVersion().compareTo(mediaProcessor.getVersion()) > 0) {
                mediaProcessor = info;
            }
        }

        logger.log(Level.SEVERE, "Using Media Processor:  {0} ",mediaProcessor.getName()+ " " + mediaProcessor.getVersion());

        // Create a task with the specified Media Processor
        String outputAssetName = String.format("%s as %s", assetToEncode.getName(), encodingPreset);
        String taskXml = "<taskBody><inputAsset>JobInputAsset(0)</inputAsset>"
                + "<outputAsset assetCreationOptions=\"0\"" // AssetCreationOptions.None
                + " assetName=\"" + outputAssetName + "\">JobOutputAsset(0)</outputAsset></taskBody>";

        Task.CreateBatchOperation task = Task.create(mediaProcessor.getId(), taskXml)
                .setConfiguration(encodingPreset).setName("Encoding");

        // Create the Job; this automatically schedules and runs it.
        Job.Creator jobCreator = Job.create()
                .setName(String.format("Encoding %s to %s", assetToEncode.getName(), encodingPreset))
                .addInputMediaAsset(assetToEncode.getId()).setPriority(2).addTaskCreator(task);
        JobInfo job = mediaService.create(jobCreator);

        String jobId = job.getId(); 
        logger.log(Level.SEVERE, "Created Job with Id: {0} ",jobId);

        // Check to see if the Job has completed
        checkJobStatus(jobId);
        // Done with the Job

        // Retrieve the output Asset
        ListResult<AssetInfo> outputAssets = mediaService.list(Asset.list(job.getOutputAssetsLink()));
        return outputAssets.get(0);
    }


    public static String getStreamingOriginLocator(AssetInfo asset) throws ServiceException {
        // Get the .ISM AssetFile
        ListResult<AssetFileInfo> assetFiles = mediaService.list(AssetFile.list(asset.getAssetFilesLink()));
        AssetFileInfo streamingAssetFile = null;
        for (AssetFileInfo file : assetFiles) {
            if (file.getName().toLowerCase().endsWith(".ism")) {
                streamingAssetFile = file;
                break;
            }
        }

        AccessPolicyInfo originAccessPolicy;
        LocatorInfo originLocator = null;

        // Create a 30-day readonly AccessPolicy
        double durationInMinutes = 60 * 24 * 30;
        originAccessPolicy = mediaService.create(
                AccessPolicy.create("Streaming policy", durationInMinutes, EnumSet.of(AccessPolicyPermission.READ)));

        // Create a Locator using the AccessPolicy and Asset
        originLocator = mediaService
                .create(Locator.create(originAccessPolicy.getId(), asset.getId(), LocatorType.OnDemandOrigin));

        // Create a Smooth Streaming base URL
        return originLocator.getPath() + streamingAssetFile.getName() + "/manifest";
    }

    private static void checkJobStatus(String jobId) throws InterruptedException, ServiceException {
        boolean done = false;
        JobState jobState = null;
        while (!done) {
            // Sleep for 5 seconds
            Thread.sleep(5000);

            // Query the updated Job state
            jobState = mediaService.get(Job.get(jobId)).getState();

            logger.log(Level.SEVERE, "Job state: {0} ",jobState);

            if (jobState == JobState.Finished || jobState == JobState.Canceled || jobState == JobState.Error) {
                done = true;
            }
        }
    }

}
