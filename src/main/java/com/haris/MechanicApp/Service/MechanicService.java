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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MechanicService    {
    @Value("${server.base-url}" )
    private String baseUrl;


    @Autowired
    private MechanicRepository mechanicRepository;
    @Autowired
    private UserRepository userRepository;



    private  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private Storage storage;
    // Apni bucket ka naam yahan likhein
    @Value("${gcp.bucket.name}")
    private String bucketName;


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







//                if (mechanicRepository.existsByUser(mechanicAndduser)  ) {
//                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Mechanic already exists");
//                }
                // Files ko GCS par upload karein
                String mechanicImageUrl = uploadFileToGcs(mecanicimg, "mechanic_images");
                String cnicFrontUrl = uploadFileToGcs(cnicfrontimg, "cnic_images");
                String cnicBackUrl = uploadFileToGcs(cnicbackimg, "cnic_images");

                if (mechanicImageUrl == null || cnicFrontUrl == null || cnicBackUrl == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
                }

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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());

        }


    }


    /**
     * Helper function jo file ko Google Cloud Storage par upload karti hai
     * aur uska public URL wapas bhejti hai.
     */

    //ye jo hay wo google cloud storage may save kraiga files aur wha say retrive kraiga files ko jessay images ko
    private String uploadFileToGcs(MultipartFile file, String directory) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = file.getOriginalFilename().replace(" ", "_");
        String uniqueFileName = directory + "/" + UUID.randomUUID().toString() + "_" + originalFileName;

        // Yahan BUCKET_NAME ke bajaye @Value se inject kiya hua 'bucketName' istemal karein
        BlobId blobId = BlobId.of(bucketName, uniqueFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        return "https://storage.googleapis.com/" + bucketName + "/" + uniqueFileName;
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
        System.out.println("Mecahnic Dashboard Call hogya ");
        Optional <Mechanic >  checkmechanic = mechanicRepository.findByPhonenumber(phonenumber);
        if(checkmechanic.isPresent()){

            Mechanic verfiedmechanic = checkmechanic.get();
            System.out.println("Mechanic ka data show hoga ab");
            return  ResponseEntity.status(HttpStatus.OK).body(verfiedmechanic);


        }
        System.out.println("Mechanic mila hi nh hay");
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
