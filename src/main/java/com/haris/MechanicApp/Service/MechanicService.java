package com.haris.MechanicApp.Service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicCredientialsDTO;
import com.haris.MechanicApp.Model.Mechanic.MechanicNumnerDto;
import com.haris.MechanicApp.Model.Mechanic.MechanicRegistrationDto;
import com.haris.MechanicApp.Model.Verification.Role;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MechanicService    {
    @Value("${server.base-url}" )
    private String baseUrl;

    @Autowired
    private Storage storage;
    @Autowired
    private MechanicRepository mechanicRepository;
    @Autowired
    private UserRepository userRepository;

    private  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    // Apni bucket ka naam yahan likhein
    private final String BUCKET_NAME = "mechanic-app-images-bucket-1"; // <<-- APNI BUCKET KA NAAM LIKHEIN



    public void updateLastSeen(Long mechanicId) {
        Mechanic m = mechanicRepository.findById(mechanicId)
                .orElseThrow();

        m.setLastSeen(Timestamp.valueOf(LocalDateTime.now()));
        mechanicRepository.save(m);
    }

    public void updateEngagedStatus(Long mechanicId, boolean engaged) {
        Mechanic m = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new RuntimeException("Mechanic not found"));

        m.setIsengaged(engaged);

        // true = accept, false = cancel
        mechanicRepository.save(m);
    }

    @Configuration
    public class GcsConfig{
        @Bean
        public Storage storage(){
            return StorageOptions.getDefaultInstance().getService();
        }
    }

    public ResponseEntity<?> registerMechanic(
            MechanicRegistrationDto mechanicdata,
            MultipartFile mecanicimg,
            MultipartFile cnicbackimg,
            MultipartFile cnicfrontimg) {

        try {
            Optional<Mechanic> checknumbermechanic = mechanicRepository.findByPhonenumber(mechanicdata.getPhonenumber());
            Optional<User> user = userRepository.findById(mechanicdata.getUserid());

            if (user.isEmpty()) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not Created ");
            }

            if (!mechanicdata.isOtpVerified()) {
                System.out.println("Otp Verifed nh hwa");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("OTP not verified");
            }
            if (user.isPresent()) {
                User mechanicAndduser = user.get();
                if (mechanicRepository.existsByUser(mechanicAndduser)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Mechanic already exists");
                }
                // ===== Step 2: Files ko GCS par upload karein =====
                String mechanicImageUrl = uploadFileToGcs(mecanicimg);
                String cnicFrontUrl = uploadFileToGcs(cnicfrontimg);
                String cnicBackUrl = uploadFileToGcs(cnicbackimg);


                Mechanic newregisteredmechanic = new Mechanic();
              //menay jo hay wo user jo tha ab mechanic bhi bn gya hay tu menay ab ussay save krdya hay Role mechanic
                mechanicAndduser.getRoles().add(Role.MECHANIC);
                userRepository.save(mechanicAndduser);

                newregisteredmechanic.setUser(mechanicAndduser);
                  //this is important data or object of mechanic
                newregisteredmechanic.setName(mechanicdata.getName());
                newregisteredmechanic.setPassword(encoder.encode(mechanicdata.getPassword()));
                newregisteredmechanic.setPhonenumber(mechanicdata.getPhonenumber());
                newregisteredmechanic.setExperienceyears(mechanicdata.getExperienceyears());
                newregisteredmechanic.setShopaddress(mechanicdata.getShopaddress());
                newregisteredmechanic.setMechanictype(mechanicdata.getMechanictype());
                newregisteredmechanic.setWorkinghours(mechanicdata.getWorkinghours());


                newregisteredmechanic.setLatitude(mechanicdata.getLatitude());
                newregisteredmechanic.setLongitude(mechanicdata.getLongitude());

                //this is for image save in Mechanic Entity of mechanic , cnic front , cnic back

                newregisteredmechanic.setMechanicimgurl(mechanicImageUrl);
                newregisteredmechanic.setCnicfronturl(cnicFrontUrl);
                newregisteredmechanic.setCnicbackurl(cnicBackUrl);


                System.out.println("hogya mechanic registered");
                mechanicRepository.save(newregisteredmechanic);
                return ResponseEntity.ok("Mechanic is registered");

            }


            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Can not register");


        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());

        }


    }


    /**
     * Helper function jo file ko Google Cloud Storage par upload karti hai
     * aur uska public URL wapas bhejti hai.
     */

    //ye jo hay wo google cloud storage may save kraiga files aur wha say retrive kraiga files ko jessay images ko
    private String uploadFileToGcs(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        // File ka ek unique naam banayein (spaces ko underscore se badal kar)
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "_");

        // BlobId object banayein (GCS mein file ka path)
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        // File ko GCS par upload karein
        storage.create(blobInfo, file.getBytes());

        // File ka public URL return karein
        return "https://storage.googleapis.com/" + BUCKET_NAME + "/" + fileName;
    }

    public ResponseEntity<?> checkmechanicnumber(MechanicNumnerDto numberDto) {

        Optional<Mechanic> checknumber = mechanicRepository.findByPhonenumber(numberDto.getNumber());
        if(checknumber.isPresent()){
            return  ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already registered");
        }
        return   ResponseEntity.status(HttpStatus.OK).body("Number is Valid");
    }

    public ResponseEntity<?> loginmechanic(MechanicCredientialsDTO credientialsDTO, AuthenticationManager authenticationManager) {
        Optional<Mechanic> mechanic  = mechanicRepository.findByPhonenumber(credientialsDTO.getPhonenumber());
        try
        {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credientialsDTO.getPhonenumber(),
                            credientialsDTO.getPassword())
                    );

            if (auth.isAuthenticated()) {
                if(mechanic.isPresent()){
                    Mechanic mechanic1 =  mechanic.get();

                    if(mechanic1.isIsverified()){
                        return ResponseEntity.ok("Login Successful ✅");
                    }
                    else if (!mechanic1.isIsverified()){
                        return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Verified ❌");
                    }

                }

            }



            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid phone number or password ❌");
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }

    }

    public ResponseEntity<?> mechanicdashboard(String phonenumber) {
Optional <Mechanic >  checkmechanic = mechanicRepository.findByPhonenumber(phonenumber);
    if(checkmechanic.isPresent()){

            Mechanic verfiedmechanic = checkmechanic.get();
            return  ResponseEntity.status(HttpStatus.OK).body(verfiedmechanic);


    }
    return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");

    }

    public ResponseEntity<?> dltmechani(long mechid) {

        Optional<Mechanic> mechanic = mechanicRepository.findById(mechid);
        if(mechanic.isPresent()){
              Mechanic mechanic1 =  mechanic.get();
              mechanicRepository.delete(mechanic1);
        return ResponseEntity.status(HttpStatus.OK).body("Mechanic is Deleted");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Not Found");
    }
}
