package org.example.hebimageobjectfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.*;


@SpringBootApplication
@RestController
public class HebImageObjectFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(HebImageObjectFinderApplication.class, args);
    }

    public static String filePath = "/Users/frank/Desktop/HEB/ProjectFolder/TestImages4uploading/";

    @GetMapping("/images")
    public String returnImages(@RequestParam(required = false) String objects) {
        String imagesList = "";

        if (objects != null) {
            System.out.println("------> Returning JSON response with metadata for ONLY specified images");
            imagesList = ImageDB.getSpecifiedImages(objects);
        } else {
            System.out.println("------> Returning JSON response with metadata for ALL images");
            imagesList = ImageDB.getAllImages();
        }

        return imagesList;
    }

    @GetMapping("/images/{imageId}")
    public String getImageFromId(@PathVariable("imageId") String imageId) {
        System.out.println("------> Returning JSON response with specific metadata for imageId = " + imageId);

        return ImageDB.getImageFromId(imageId);
    }

    @PostMapping("/images")
    public String addImage(@RequestParam(required = false) String filename,
                               @RequestParam(required = false) String image_url,
                               @RequestParam(required = false) String imageLabel,
                               @RequestParam(required = false) boolean enableObjectDetection) throws IOException {

        String upload_id = "";
        ArrayList<String> objectNames = new ArrayList<>();
        JSONObject imageDataObject = new JSONObject();

        System.out.println("------> Passed in items for POST call:");
        System.out.println("---------> filename = " + filename);
        System.out.println("---------> image_url = " + image_url);
        System.out.println("---------> imageLabel = " + imageLabel);
        System.out.println("---------> enableObjectDetection = " + enableObjectDetection);

        // Check if filename OR image_url are provided
        if (filename != null && image_url == null) {  // ONLY passed filename
            // Check if filename exists
            String fullPathToImage = filePath + filename;
            File f = new File(fullPathToImage);
            if(f.exists() && !f.isDirectory()) {
                imageDataObject.put("filesize", (f.length() / 1024) + " kb");  // Get filesize
                imageDataObject.put("filename", filename);

                // Upload image to Imagga and get results if object detection is ON
                if (enableObjectDetection) {
                    upload_id = ImaggaObjectDetection.ImageUpload(fullPathToImage);
                    System.out.println("------> Upload ID = " + upload_id);

                    objectNames = ImaggaObjectDetection.getObjectNamesFromUploadID(upload_id);
                } else {
                    System.out.println("------> *** WARNING *** ---> OBJECT DETECTION IS DISABLED");
                }
            } else {
                throw new IllegalArgumentException("IMAGE FILE DOES NOT EXIST!!!");
            }
        } else if (filename == null && image_url != null) {  // Only passed image_url
            System.out.println("------> Using image from URL: " + image_url);
            imageDataObject.put("image_url", image_url);

            if (enableObjectDetection) {
                objectNames = ImaggaObjectDetection.getObjectNamesFromURL(image_url);
            } else {
                System.out.println("------> *** WARNING *** ---> OBJECT DETECTION IS DISABLED");
            }
        } else if (filename != null && image_url != null) {  // Both are passed in, error out!
            System.out.println("------> *** ERROR *** ---> BOTH IMAGE FILE AND URL PROVIDED!!! ***");
            throw new IllegalArgumentException("BOTH IMAGE FILE AND URL PROVIDED!!!");
        } else {  // Neither are passed in, error out!
            System.out.println("------> *** ERROR *** ---> NO IMAGE FILE OR URL PROVIDED!!! ***");
            throw new IllegalArgumentException("NO IMAGE FILE OR URL PROVIDED!!!");
        }


        // Create JSON body
        JSONObject jsonImageBodyObject = new JSONObject();

        // Image data
        jsonImageBodyObject.put("image data", imageDataObject);

        // Optional image label
        if (imageLabel == null) {
            String generatedLabel = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            imageLabel = "Image_" + generatedLabel;  // Set default value if missing
        }
        JSONObject optionalLabelObject = new JSONObject();
        optionalLabelObject.put("imageLabel", imageLabel);
        jsonImageBodyObject.put("image label", optionalLabelObject);

        // Optional object detection
        JSONObject optionalObjectDetectionObject = new JSONObject();
        optionalObjectDetectionObject.put("enableObjectDetection", enableObjectDetection);
        jsonImageBodyObject.put("object detection flag", optionalObjectDetectionObject);

        // Object names detected
        JSONArray objectNamesJSONArray = new JSONArray();
        for (String objectName : objectNames) {
            objectNamesJSONArray.put(objectName);
        }
        jsonImageBodyObject.put("objects detected", objectNamesJSONArray);

        // Add to MongoDB
        String dbIdentifier = ImageDB.addImage(jsonImageBodyObject);

        // DB identifier
        JSONObject dbIdentifierObject = new JSONObject();
        dbIdentifierObject.put("dbIdentifier", dbIdentifier);
        jsonImageBodyObject.put("DB identifier", dbIdentifierObject);

        return jsonImageBodyObject.toString();
    }
}
