package org.example.hebimageobjectfinder;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import java.util.Objects;


public class ImageDB {
    public static String uri = "mongodb://localhost:27017/Images?retryWrites=true&writeConcern=majority";

    public static String addImage(JSONObject data) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("------> Adding single image into MongoDB");

            // database and collection code goes here
            MongoDatabase db = mongoClient.getDatabase("Images");
            MongoCollection<Document> collection = db.getCollection("image");

            // Create document
            Document document = new Document(data.toMap());
            String dbIdentifier = Objects.requireNonNull(collection.insertOne(document).getInsertedId()).asObjectId().getValue().toString();
            System.out.println("------> Inserted image with id: " + dbIdentifier);

            return dbIdentifier;
        }
    }

    public static String getImageFromId(String imageId) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("------> Getting single image from MongoDB");

            // database and collection code goes here
            MongoDatabase db = mongoClient.getDatabase("Images");
            MongoCollection<Document> collection = db.getCollection("image");
            String foundDocument = "";

            Bson filter = Filters.eq("_id", new ObjectId(imageId));
            FindIterable<Document> documents = collection.find(filter);

            for (Document document : documents) {
                foundDocument = document.toJson();  // Should only ever return one document
            }

            return foundDocument;
        }
    }

    public static String getAllImages() {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("------> Getting ALL images from MongoDB");

            // database and collection code goes here
            MongoDatabase db = mongoClient.getDatabase("Images");
            MongoCollection<Document> collection = db.getCollection("image");
            StringBuilder allImages = new StringBuilder();

            //Retrieving the documents
            FindIterable<Document> iterDoc = collection.find();
            allImages.append("[");
            for (Document document : iterDoc) {
                allImages.append(document.toJson());
                allImages.append(",");
            }
            allImages.deleteCharAt(allImages.length() - 1);  // Remove the last comma before closing out
            allImages.append("]");

            return allImages.toString();
        }
    }

    public static String getSpecifiedImages(String objects) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("------> Getting multiple specified images from MongoDB for these objects: " + objects);

            if (objects.isEmpty()) {
                throw new IllegalArgumentException("NO OBJECTS SPECIFIED PROVIDED!!!");
            }

            // database and collection code goes here
            MongoDatabase db = mongoClient.getDatabase("Images");
            MongoCollection<Document> collection = db.getCollection("image");

            objects = objects.substring(1, objects.length() - 1);  // Remove leading double quotes
            System.out.println("------> Passed in objects to filter on: " + objects);
            String[] specifiedObjects = objects.split(",");
            System.out.println("------> Passed in objects size: " + specifiedObjects.length);
            for (int i = 0; i < specifiedObjects.length; i++) {
                System.out.println("---------> Passed in objects[" + i + "]: " + specifiedObjects[i]);
            }

            Bson filter = Filters.eq("objects detected", specifiedObjects[0]);  // Add the first object filter
            if (specifiedObjects.length > 1) {  // If there are more than one object filter, add them here
                for (int x = 1; x < specifiedObjects.length; x++) {
                    filter = Filters.or(filter, Filters.eq("objects detected", specifiedObjects[x]));
                }
            }
            FindIterable<Document> documents = collection.find(filter);  // Get all documents based on the filters

            // Build JSON string to return
            StringBuilder foundDocument = new StringBuilder();
            foundDocument.append("[");
            for (Document document : documents) {
                foundDocument.append(document.toJson());
                foundDocument.append(",");
            }

            if (foundDocument.length() > 1) {
                foundDocument.deleteCharAt(foundDocument.length() - 1);  // Remove the last comma before closing out
            }
            foundDocument.append("]");

            return foundDocument.toString();
        }
    }
}
