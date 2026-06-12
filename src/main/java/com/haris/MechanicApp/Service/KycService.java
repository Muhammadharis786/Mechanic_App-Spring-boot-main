package com.haris.MechanicApp.Service;

import com.google.cloud.storage.*;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.Image;  // AWS Image

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

@Service
public class KycService {

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Autowired
    private Storage storage;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Autowired
    private MechanicRepository mechanicRepository;

    private RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretKey)
                        )
                )
                .build();
    }

    // ================= FACE COMPARE (AWS Rekognition) =================
    public ResponseEntity<?> verifyFace(
            MultipartFile nicFront,
            MultipartFile selfie,
            MultipartFile nicBack ,
            String mechanicnumber
    ) throws IOException {
        System.out.println("This is mechanic number: "+ mechanicnumber);
            Optional<Mechanic> checkmechanic  = mechanicRepository.findByPhonenumber(mechanicnumber);
            if(checkmechanic.isEmpty()){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic  not found");
            }
            Mechanic mechanic = checkmechanic.get();
        RekognitionClient client = rekognitionClient();

        // AWS Image — directly use karo (import upar se aa rahi hai)
        Image source = Image.builder()
                .bytes(SdkBytes.fromByteArray(nicFront.getBytes()))
                .build();

        Image target = Image.builder()
                .bytes(SdkBytes.fromByteArray(selfie.getBytes()))
                .build();

        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(source)
                .targetImage(target)
                .similarityThreshold(80F)
                .build();

        CompareFacesResponse response = client.compareFaces(request);
       String nicnumber =  extractCnic(nicFront) ;


        float similarity = 0f;
        if (!response.faceMatches().isEmpty()) {

            similarity = response.faceMatches().getFirst().similarity();
        }

        boolean verified = similarity >= 90.0f;

        if(  verified && !nicnumber.contains("Not")){
       boolean   checknic  =   mechanicRepository.existsByCnicNumber(nicnumber);
       if(checknic){
           return ResponseEntity.status(HttpStatus.CONFLICT).body("cnic Already used");
       }
            System.out.println("This is cnic number:"+ nicnumber);
            String cnicFrontUrl = uploadFileToGcs(nicFront, "cnic_images");
            String cnicBackUrl = uploadFileToGcs(nicBack, "cnic_images");
            if ( cnicFrontUrl == null || cnicBackUrl == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
            }

            mechanic.setIskyc(true);
            mechanic.setCnicNumber(nicnumber);
            mechanic.setCnicfronturl(cnicFrontUrl);
            mechanic.setCnicbackurl(cnicBackUrl);
            mechanicRepository.save(mechanic);
            System.out.println("mechanic succesfully uploaded");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("verified", verified);
        result.put("similarityScore", String.format("%.2f", similarity) + "%");
        result.put("message", verified ? "KYC Verified ✅" : "KYC Failed ❌");

        return ResponseEntity.ok(result);
    }

    // ================= CNIC EXTRACT (Google Vision) =================
    public String extractCnic(MultipartFile image) throws IOException {

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {

            ByteString imgBytes = ByteString.copyFrom(image.getBytes());

            // Google Vision Image — fully qualified name use karo (conflict avoid)
            com.google.cloud.vision.v1.Image img =
                    com.google.cloud.vision.v1.Image.newBuilder()
                            .setContent(imgBytes)
                            .build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response =
                    client.batchAnnotateImages(Collections.singletonList(request));

            String fullText = response.getResponses(0)
                    .getFullTextAnnotation()
                    .getText();

            System.out.println("NIC extracted text: " + fullText);

            // CNIC pattern: 12345-1234567-1
            Pattern pattern = Pattern.compile("\\d{5}-\\d{7}-\\d");
            Matcher matcher = pattern.matcher(fullText);

            return matcher.find() ? matcher.group() : "CNIC Not Found";
        }
    }

    // ================= GCS UPLOAD =================
    public String uploadFileToGcs(MultipartFile file, String folder) throws IOException {
        String fileName = folder + "/" + UUID.randomUUID() + "_" +
                file.getOriginalFilename().replace(" ", "_");

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return "https://storage.googleapis.com/" + bucketName + "/" + fileName;
    }
}