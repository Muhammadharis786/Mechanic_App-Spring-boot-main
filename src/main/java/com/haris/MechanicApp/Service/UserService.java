package com.haris.MechanicApp.Service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.RoadInfo;
import com.haris.MechanicApp.Model.User.UserDto;
import com.haris.MechanicApp.Model.Verification.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;

import com.haris.MechanicApp.Repository.AppointmentRepository;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import com.haris.MechanicApp.Repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserService  {
    // Apni bucket ka naam yahan likhein
    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Autowired
    private RedisTemplate<String , String > redisTemplate;

    @Autowired
    private Storage storage;
    @Autowired


    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WhatsappOtpService whtsappotp;

    @Autowired
    private VerificationTokenRepository tokenRepo;


    @Autowired
    private MechanicRepository mechRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    // -----------------------------------
    // Get current date-time in formatted string
    // -----------------------------------
    public String modernDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy h:mm a", Locale.ENGLISH);
        String formattedDate = now.format(formatter).toLowerCase();

        formattedDate = formattedDate.substring(0, 2) +
                formattedDate.substring(2, 3).toUpperCase() +
                formattedDate.substring(3);

        return formattedDate;
    }

    // -----------------------------------
    // Forgot Password
    // -----------------------------------
    public ResponseEntity<?> forgotpasswords(String number) {
        Optional<User> checkUser = userRepo.findByPhonenumber(number);
        String token = String.format("%06d", new Random().nextInt(999999));
        if(checkUser.isPresent()){
                User user =  checkUser.get();
                if(user.isEnabled()){
                    updateVerificationToken(user, token);
                    whtsappotp.sendwhatsappotp(user.getPhonenumber(), token);

                    return ResponseEntity.ok("Check Whatsapp " + user.getPhonenumber());
                }

        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please Register Your Account");
    }
    // -----------------------------------
    // Register new user
    // -----------------------------------
    public ResponseEntity<?> register(DtoUser user) {

Optional<User> checkUser  = userRepo.findByPhonenumber(user.getPhonenumber());

        String token = String.format("%06d", new Random().nextInt(999999));

        if (checkUser.isPresent() ) {
            User  user2 = checkUser.get();

            if(user2.isEnabled()){
                return ResponseEntity.status(HttpStatus.IM_USED)
                        .body("phone Number  Already Used ");
            }
 updateVerificationToken(user2, token);
//
                whtsappotp.sendwhatsappotp(user2.getPhonenumber() , token);

                return ResponseEntity.ok("Check Your whatsApp for OTP code.." + user2.getPhonenumber());


        }

        else if(checkUser.isEmpty()){
            System.out.println("Create new register");
            User newUser = new User();
            newUser.setPhonenumber(user.getPhonenumber());

            newUser.setPassword(encoder.encode(user.getPassword()));
            newUser.setEnabled(false);
            newUser.setRegistrationDate(modernDate());

            newUser.setUsername(user.getPhonenumber());
            userRepo.save(newUser);

            createVerificationToken(newUser, token);
//
            whtsappotp.sendwhatsappotp(newUser.getPhonenumber() , token);

            return ResponseEntity.ok("Check your Whatsapp for OTP code  " + newUser.getPhonenumber());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send OTP. Please try again later.");

 }

    private void updateVerificationToken(User user, String token) {
        Optional<VerificationToken> checkToken = tokenRepo.findByUser(user);
        VerificationToken verificationToken;
        if (checkToken.isPresent()) {
            verificationToken = checkToken.get();
            verificationToken.setToken(token);
        } else {

            verificationToken = new VerificationToken();
            verificationToken.setUser(user);
            verificationToken.setToken(token);
        }
        verificationToken.setCreatedDate(LocalDateTime.now());
        verificationToken.setExpiryDate(1); // Set new expiry date
        tokenRepo.save(verificationToken);
    }

    // -----------------------------------
    // Save verification token with user
    // -----------------------------------
    private void createVerificationToken(User newUser, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(newUser);
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(1); // expiry in minutes (assumed)

        verificationToken.setCreatedDate(LocalDateTime.now());
        tokenRepo.save(verificationToken);

    }

    // -----------------------------------
    // Verify user registration via token
    // -----------------------------------
    public ResponseEntity<?> verifyRegistration(String token ,String number ) {
            Optional<User> checkuser = userRepo.findByPhonenumber(number);

            if(checkuser.isPresent()){
                User getuser= checkuser.get();

                if(getuser.isEnabled()){
                    return ResponseEntity.status(HttpStatus.IM_USED).body(number + " Already Verified Please Login");
                }
                Optional<VerificationToken> checkToken = tokenRepo.findByTokenAndUser_Userid(token ,getuser.getUserid());
                if(checkToken.isPresent()){
                    VerificationToken verifiedToken = checkToken.get();


                    // Enable user account
                    User user = verifiedToken.getUser();
                    user.setEnabled(true);
                    Set<Role> userRoles = new  HashSet<>() ;
                    userRoles.add(Role.USER);
                    user.setRoles(userRoles);
                    System.out.println("User True hogya hay ab");
                    userRepo.saveAndFlush(user);

                    // Delete token after verification
                    tokenRepo.delete(verifiedToken);

                    return ResponseEntity.ok("User Verified Successfully!");
                }
                return ResponseEntity.status(401).body("Invalid or Expired Token");
            }


        return ResponseEntity.status(401).body("First Enter Your Credentials");
    }

    public ResponseEntity<?> login(DtoUser user, AuthenticationManager authenticationManager) {
        String identifier = user.getPhonenumber() + ";" + user.getLoginAs().toUpperCase();
        System.out.println(identifier);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, user.getPassword())
            );

            if (auth.isAuthenticated()) {
                if("USER".equals(user.getLoginAs().toUpperCase())) {


                    return ResponseEntity.ok(" USER Login Successfully");


                }
                else if ("MECHANIC".equals(user.getLoginAs().toUpperCase())) {
                    Mechanic mech = mechRepo.findByPhonenumber(user.getPhonenumber()).get();

                        return ResponseEntity.ok(mech);
   }
                }
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Number or password ❌");
        } catch (AuthenticationException e) {
            String Message = "Invalid Number or password";
            if(e.getCause() instanceof UsernameNotFoundException)
                 Message = e.getCause().getMessage();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Message);
        }
    }


    public ResponseEntity<?> verifynewPasswordToken(Token token) {

        Optional<User> checkuser = userRepo.findByPhonenumber(token.getPhonenumber());

        if(checkuser.isPresent()){
            User user =  checkuser.get();
            Optional<VerificationToken> checkToken = tokenRepo.findByTokenAndUser_Userid(token.getToken() , user.getUserid() );
            if(checkToken.isPresent()){
                VerificationToken token1 = checkToken.get();
                if (token1.getExpiryDate().before(Calendar.getInstance().getTime())) {
                    return ResponseEntity.status(498).body("Expired Token");
                }
                    tokenRepo.delete(token1);
                return ResponseEntity.ok("Token Verified");
            }
            return ResponseEntity.status(401).body("Token Not Exist");

         }

        return ResponseEntity.status(401).body("Invalid Token");
 }

    public ResponseEntity<?> updatePassword(DtoUser user) {



                Optional<User>  checkuser =  userRepo.findByPhonenumber(user.getPhonenumber());
                if(checkuser.isPresent()){
                      User updateuser =  checkuser.get();

                      updateuser.setPassword(encoder.encode((user.getPassword())));
                      userRepo.save(updateuser);
                      return  ResponseEntity.ok("Password Updated Successfully");
                }

               return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Found");

    }

    public ResponseEntity<?> updateUser(UserDto userDto,
                                            MultipartFile userimage ,
                                           String phonenumber) {
             Optional<User>  checkuser = userRepo.findByPhonenumber(phonenumber);
        try
        {
            if(checkuser.isPresent()){

                User verifieduser =   checkuser.get();
               String mechanicImageUrl = uploadFileToGcs(userimage, "user_images");
                if(mechanicImageUrl == null){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File Upload Failed");
                }
                verifieduser.setEmail(userDto.getEmail());
                verifieduser.setPassword(  encoder.encode(userDto.getPassword()));
                verifieduser.setUsername(userDto.getUsername());
                verifieduser.setUserimgurl(mechanicImageUrl);
                userRepo.save(verifieduser);

        return ResponseEntity.ok("Updated Succesfully");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User Not Exist");


} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public ResponseEntity<?> dashboard(String phonenumber) {
        System.out.println("im dashboard");
       Optional<User>  checkuser = userRepo.findByPhonenumber(phonenumber);

    if(checkuser.isPresent()){
        User user =  checkuser.get();


        Map<String , Object> map = new HashMap<>();
        System.out.println("User Cordinates: "+ user.getLastLatitude()+" : " +  user.getLastLongitude());

        GeoOperations<String  , String> geoOperations = redisTemplate.opsForGeo();
         double userlongitude ;
         double userlatitude  ;

        List<Point> usercordinates = geoOperations.position("user", String.valueOf(user.getUserid()));
        //this is for search mechanic and calcute distance bw user and mechanic and give nearbymechanic
        //that define specific range and save in results type object
        if  ( usercordinates != null && ! usercordinates.isEmpty() &&  usercordinates.get(0) != null) {
            userlongitude =  usercordinates.get(0).getX();
            userlatitude =  usercordinates.get(0).getY();
            System.out.println("Fetched User position from REDIS: " + userlatitude + ", " + userlongitude);
        } else {
            // Fallback if not in Redis
            userlongitude = user.getLastLongitude().doubleValue();
            userlatitude = user.getLastLatitude().doubleValue();
        }
        GeoResults<GeoLocation<String>> results =
                geoOperations.search(
                        "mechanic",
                        GeoReference.fromCoordinate(userlongitude, userlatitude),
                        new Distance(65, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .includeCoordinates()

                );
        List<Long> mechanicIds = new ArrayList<>();
        StringBuilder destinationparam = new StringBuilder();

        for (GeoResult<GeoLocation<String>> result  : results){
          long mechanicid =  Long.parseLong (result.getContent().getName());

          Point point = result.getContent().getPoint();
            System.out.println("Mechanic Latitude: "+ point.getX());
            System.out.println("Mechanic Longitude: "+ point.getY());
            destinationparam.append(point.getY()).append(",").append(point.getX()).append("|");
          mechanicIds.add(mechanicid);
        }
        GoogleDistance googleapi =  new GoogleDistance();
        if(destinationparam.length()>0){
            destinationparam.setLength(destinationparam.length()-1);
        }
     List<RoadInfo> distancewithtime =    googleapi.getBatchRoadDistances(userlatitude , userlongitude , destinationparam.toString());

        List<Mechanic>   allnearbymechanics = mechRepo.findAllById(mechanicIds);
        if(allnearbymechanics.isEmpty()){
            map.put("user", user);
            map.put("mechanics", "Mechanic not available right now");
            return ResponseEntity.ok(map);
        }

        Map<Long, Double> distanceMap = new HashMap<>();
        for (int i = 0; i < mechanicIds.size(); i++) {
            distanceMap.put(mechanicIds.get(i), distancewithtime.get(i).getDistance());
        }

        List<MechanicDTO> mechanicDTOS = new ArrayList<>();
        for (Mechanic mechanic : allnearbymechanics) {
            MechanicDTO mechanicDTO = new MechanicDTO();
            mechanicDTO.setName(mechanic.getName());
            mechanicDTO.setMechanicType(mechanic.getMechanictype());
            mechanicDTO.setAveragerating(mechanic.getAverageRating());
            mechanicDTO.setExperience(mechanic.getExperienceyears());
            mechanicDTO.setIsactive(mechanic.isIsactive());
            mechanicDTO.setPhonenumber(mechanic.getPhonenumber());
            mechanicDTO.setMechanicimgurl(mechanic.getMechanicimgurl());
            mechanicDTO.setIsengaged(mechanic.isIsengaged());
            mechanicDTO.setLatitude(mechanic.getLatitude());
            mechanicDTO.setLongitude(mechanic.getLongitude());
            mechanicDTO.setMechaniclocname(mechanic.getLocationName());
            mechanicDTO.setDistance(BigDecimal.valueOf(distanceMap.get(mechanic.getId())));
            mechanicDTOS.add(mechanicDTO);


        }

        map.put("mechanics", mechanicDTOS);
        map.put("user", user);

        return  ResponseEntity.ok(map);
    }

    return  ResponseEntity.status(401).body("Invalid User");
}


    public ResponseEntity<?> allusers() {

          List<User>  allusers = userRepo.findAll();

          return  ResponseEntity.ok(allusers);
    }

    public ResponseEntity<?> dltuser(long userid) {

        Optional<User>  checkuser = userRepo.findById(userid);
        if(checkuser.isPresent()){
            User user = checkuser.get();
            userRepo.delete(user);
            return ResponseEntity.ok("User Deleted");
            

        }


        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }



}
