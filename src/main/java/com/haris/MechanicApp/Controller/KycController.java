package com.haris.MechanicApp.Controller;

import com.haris.MechanicApp.Model.KYC.KycResult;
import com.haris.MechanicApp.Service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    @Autowired
    private KycService kycService;

    @PostMapping("/verify/{mechanicnumber}")
    public ResponseEntity<?> verifyKyc(
            @PathVariable("mechanicnumber") String mechanicnumber,
            @RequestParam("nicFront") MultipartFile nicFront,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("nicBack") MultipartFile nicBack


    ) {
        try {

            return  kycService.verifyFace(nicFront, selfie ,nicBack ,mechanicnumber);
        } catch (RekognitionException e) {
            return ResponseEntity.badRequest()
                    .body(new KycResult(false, 0f, "AWS Error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new KycResult(false, 0f, "Error: " + e.getMessage()));
        }
    }
}