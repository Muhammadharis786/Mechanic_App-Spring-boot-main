package com.haris.MechanicApp.Service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.haris.MechanicApp.Model.Mechanic.*;
import com.haris.MechanicApp.Model.Verification.*;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.OtpTokenMechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MechanicService    {
    @Value("${server.base-url}" )
    private String baseUrl;


    @Autowired
    private MechanicRepository mechanicRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WhatsappOtpService whtsappotp;
    @Autowired
    private OtpTokenMechanicRepository otptokenrepo;

    @Autowired
    private RedisTemplate<String , String > redisTemplate;


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
            MultipartFile cnicfrontimg
    )
    {

        try {
            Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(mechanicdata.getPhonenumber());
            Optional<User> user = userRepository.findByPhonenumber(mechanicdata.getPhonenumber());
         if (user.isPresent()) {
                User mechanicAndduser = user.get();
                if(checkmechanic.isPresent()){

                    Mechanic newregisteredmechanic = checkmechanic.get();

                    String mechanicImageUrl = uploadFileToGcs(mecanicimg, "mechanic_images");
                    String cnicFrontUrl = uploadFileToGcs(cnicfrontimg, "cnic_images");
                    String cnicBackUrl = uploadFileToGcs(cnicbackimg, "cnic_images");

                    if (mechanicImageUrl == null || cnicFrontUrl == null || cnicBackUrl == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
                    }


                    //menay jo hay wo user jo tha ab mechanic bhi bn gya hay tu menay ab ussay save krdya hay Role mechanic
                    mechanicAndduser.getRoles().add(Role.MECHANIC);
                    userRepository.save(mechanicAndduser);
                    newregisteredmechanic.setUser(mechanicAndduser);
                    newregisteredmechanic.setName(mechanicdata.getName());
                    newregisteredmechanic.setExperienceyears(mechanicdata.getExperienceyears());
                    newregisteredmechanic.setShopaddress(mechanicdata.getShopaddress());
                    newregisteredmechanic.setMechanictype(mechanicdata.getMechanictype());
                    newregisteredmechanic.setWorkinghours(mechanicdata.getWorkinghours());
                    newregisteredmechanic.setShoplatitude(mechanicdata.getLatitude());
                    newregisteredmechanic.setShoplongitude(mechanicdata.getLongitude());

                    Double shoplat = mechanicdata.getLatitude().doubleValue(); //this is shop latitude
                    Double shoplon = mechanicdata.getLongitude().doubleValue();  //this is shop longitude
                    GeoOperations<String  , String> geoOperations = redisTemplate.opsForGeo();
                    String mechid =  newregisteredmechanic.getId().toString();



                    newregisteredmechanic.setShopaddress(mechanicdata.getShopaddress());
                    newregisteredmechanic.setMechanicimgurl(mechanicImageUrl);
                    newregisteredmechanic.setCnicfronturl(cnicFrontUrl);
                    newregisteredmechanic.setCnicbackurl(cnicBackUrl);

                    newregisteredmechanic.setIscompleteRegister(true);
                    System.out.println("hogya mechanic registered");

                    mechanicRepository.save(newregisteredmechanic);
                    // Jab mechanic register ya login kare
                    String  mechanictype= mechanicdata.getMechanictype().toUpperCase();
                    geoOperations.add("mech", new Point(shoplon, shoplat), mechid);
                    redisTemplate.opsForHash().put(
                            "mechanic:details:" + mechid,
                            "serviceType",
                            mechanictype

                            );

                    return ResponseEntity.ok("Mechanic is registered");

                }
                return ResponseEntity.status((HttpStatus.NOT_FOUND)).body("Mechanic Cannot be registered");


            }


            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("First Need to Create user Account! ");



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
    public String modernDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy h:mm a", Locale.ENGLISH);
        String formattedDate = now.format(formatter).toLowerCase();

        formattedDate = formattedDate.substring(0, 2) +
                formattedDate.substring(2, 3).toUpperCase() +
                formattedDate.substring(3);

        return formattedDate;
    }
    public ResponseEntity<?>    mechanicOtp(MechOtpDto otpDto) {
    Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(otpDto.getPhonenumber());
        Optional<User> checuser = userRepository.findByPhonenumber(otpDto.getPhonenumber());

         String token = String.format("%06d", new Random().nextInt(999999));

            if(checkmechanic.isPresent()){
            Mechanic mech =  checkmechanic.get();
            if (mech.isIsotpverified()  && mech.isIscompleteRegister()){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Number Already used");

            }

            createToken (token , mech);
                try {
                    whtsappotp.sendwhatsappotp(otpDto.getPhonenumber(), token);
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (
                            errorMsg.contains("30") ||      // 30 sec wala message
                                    errorMsg.contains("rate") ||
                                    errorMsg.contains("limit") ||
                                    errorMsg.contains("too many")
                    )) {
                        return ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .body("Please wait 30 seconds before requesting another OTP");
                    }
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to send OTP, please try again");
                }
            return ResponseEntity.ok("Check your WhatsApp Number: "+ otpDto.getPhonenumber());
        }

        User user;
        if(checuser.isPresent()) {
            user = checuser.get();

        }
        else {
            // User nahi hai — automatically bana do
            user = new User();
            user.setUsername(otpDto.getPhonenumber());         // MechOtpDto mein name add karo
            user.setPhonenumber(otpDto.getPhonenumber());
            user.setEnabled(true);
            user.setPassword(encoder.encode(otpDto.getPassword()));
            user.setRegistrationDate(modernDate());
            user.getRoles().add(Role.USER);
            userRepository.save(user);
        }
            Mechanic newmechanic =  new Mechanic();
            newmechanic.setPhonenumber(otpDto.getPhonenumber());
            newmechanic.setPassword(encoder.encode(otpDto.getPassword()));
            newmechanic.setUser(user);
            mechanicRepository.save(newmechanic);


            createToken (token , newmechanic);
        try {
            whtsappotp.sendwhatsappotp(otpDto.getPhonenumber(), token);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (
                    errorMsg.contains("30") ||      // 30 sec wala message
                            errorMsg.contains("rate") ||
                            errorMsg.contains("limit") ||
                            errorMsg.contains("too many")
            )) {
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Please wait 30 seconds before requesting another OTP");
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP, please try again");
        }

                return ResponseEntity.ok("Check your WhatsApp Number"+ otpDto.getPhonenumber());


}

    private void createToken(String token, Mechanic mech) {
        Optional<VerificationTokenMechanic> checktoken = otptokenrepo.findByMechanic(mech);
        if(checktoken.isPresent()){

            VerificationTokenMechanic Token = checktoken.get();
            otptokenrepo.delete(Token);
 }
        VerificationTokenMechanic verificationTokenMechanic = new VerificationTokenMechanic();

        verificationTokenMechanic.setToken(token);
        verificationTokenMechanic.setMechanic(mech);
        verificationTokenMechanic.setExpiryDate(1);

        otptokenrepo.save(verificationTokenMechanic);
        System.out.println("Token Created Successfully");


    }
    public  ResponseEntity<?> verifymechanic(Token token) {
        Optional<VerificationTokenMechanic> checktoken =  otptokenrepo.findByTokenAndMechanic_Phonenumber(token.getToken() , token.getPhonenumber() );

        if(checktoken.isPresent()){
   VerificationTokenMechanic Token = checktoken.get();
            // Instant se compare karo
            if (Token.getExpiryDate().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token Expired, please request again");
            }
    otptokenrepo.delete(Token);

    Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(token.getPhonenumber());
   if(checkmechanic.isPresent()){
        Mechanic mech =  checkmechanic.get();
            mech.setIsotpverified(true);
            mechanicRepository.save(mech);
            return  ResponseEntity.status(HttpStatus.OK).body("WhatsApp Number Verified");
   }
   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
}
return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid OTP please enter correct OTP!");

    }

    public ResponseEntity<?> onlinestatus (String mechanicphonenumber, IsOnlineDto isOnlineDto){
            Optional<Mechanic> ismechanic = mechanicRepository.findByPhonenumber(mechanicphonenumber);
            if(ismechanic.isEmpty()){
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");

            }
             Mechanic mech =  ismechanic.get();
            if(isOnlineDto.isIsonline()){
                mech.setIsactive(true);
                mechanicRepository.save(mech);

                redisTemplate.opsForValue().set("mechanic:online:" + mech.getId(), "true");

                redisTemplate.opsForHash().put(
                        "mechanic:details:" + mech.getId(),
                        "isOnline",
                        "true"
                );
                return ResponseEntity.status(HttpStatus.OK).body("Mechanic Online Successfully");
            }
            mech.setIsactive(false);
        mechanicRepository.save(mech);

        redisTemplate.delete("mechanic:online:" +mech.getId());

        redisTemplate.opsForHash().put(
                "mechanic:details:" + mech.getId(),
                "isOnline",
                "false"
        );
        return ResponseEntity.status(HttpStatus.OK).body("Mechanic Offline Successfully");
 }

    public ResponseEntity<?> forgotpasswords(String number) {
        System.out.println("This is number: "+ number);

        Optional<Mechanic> checmechanic = mechanicRepository.findByPhonenumber(number);
        if (checmechanic.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
        }

        Mechanic mechanic =  checmechanic.get();

        if(!mechanic.isIscompleteRegister()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Create Mechanic Account");

        }
        String token = String.format("%06d", new Random().nextInt(999999));
        createToken( token , mechanic);
        try {
            whtsappotp.sendwhatsappotp(mechanic.getPhonenumber(), token);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (
                    errorMsg.contains("30") ||      // 30 sec wala message
                            errorMsg.contains("rate") ||
                            errorMsg.contains("limit") ||
                            errorMsg.contains("too many")
            )) {
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Please wait 30 seconds before requesting another OTP");
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP, please try again");
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("We have sent otp to whatsapp "+ mechanic.getPhonenumber());
    }

    public ResponseEntity<?> verifynewPasswordToken(Token token) {
        Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(token.getPhonenumber());
        if(checkmechanic.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
        }
        Mechanic mechanic =  checkmechanic.get();
        Optional<VerificationTokenMechanic> checkToken = otptokenrepo.findByTokenAndMechanic_Phonenumber(token.getToken() , mechanic.getPhonenumber() );
            if (checkToken.isEmpty()){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token Not Found");
            }
            VerificationTokenMechanic token1 = checkToken.get();

        if (token1.getExpiryDate().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token Expired, please request again");
        }
        otptokenrepo.delete(token1);
        return ResponseEntity.ok("Token Verified");
 }

    public ResponseEntity<?> updatePassword(DtoUser mechanic) {
        Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(mechanic.getPhonenumber());
        if(checkmechanic.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mechanic Not Found");
        }
        Mechanic updatemechpassword =  checkmechanic.get();
        updatemechpassword.setPassword(encoder.encode((mechanic.getPassword())));
        mechanicRepository.save(updatemechpassword);
        return  ResponseEntity.ok("Password Updated Successfully");
 }
}
