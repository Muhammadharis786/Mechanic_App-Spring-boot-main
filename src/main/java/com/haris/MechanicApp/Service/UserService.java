package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.GoogleDistance;
import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicDTO;
import com.haris.MechanicApp.Model.Verification.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import com.haris.MechanicApp.Repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserService  {

    @Autowired

    private UserRepository userRepo;

    @Autowired
    private VerificationTokenRepository tokenRepo;

    @Autowired
    private EmailService emailService;
    @Autowired
    private MechanicRepository mechRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // -----------------------------------
    // Load user by email for Spring Security
    // -----------------------------------
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        Optional<User> checkUser = userRepo.findByEmail(email);
//
//        if (checkUser.isPresent()) {
//
//            User user = checkUser.get();
//
//            if (user.isEnabled()) {
//                return new MyPrincipal(user);
//            }
//            throw new UsernameNotFoundException("User Not Verified");
//        }
//
//        throw new UsernameNotFoundException("User Not Found");
//    }

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
    public ResponseEntity<?> forgotpasswords(String email) {
        Optional<User> checkUser = userRepo.findByEmail(email);
        String token = String.format("%06d", new Random().nextInt(999999));
        if(checkUser.isPresent()){
                User user =  checkUser.get();
                if(user.isEnabled()){
                    updateVerificationToken(user, token);
                    emailService.sendVerificationEmail(user.getEmail(), token);
                    return ResponseEntity.ok("Check Email....");
                }

        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please Register Your Account");
    }
    // -----------------------------------
    // Register new user
    // -----------------------------------
    public ResponseEntity<?> register(DtoUser user) {

        Optional<User> checkUser = userRepo.findByEmail(user.getEmail());


        String token = String.format("%06d", new Random().nextInt(999999));

        if (checkUser.isPresent() ) {
            User  user2 = checkUser.get();

            if(user2.isEnabled()){
                return ResponseEntity.status(HttpStatus.IM_USED)
                        .body("Email Already Used ");
            }
            else   if ( !user2.isEnabled()){



                updateVerificationToken(user2, token);
                emailService.sendVerificationEmail(user2.getEmail(), token);
                return ResponseEntity.ok("We have sent a code of your email please check and verify your " +
                        "account" + user2.getEmail());
            }

        }

        else if(checkUser.isEmpty()){
            System.out.println("Create new register");
            User newUser = new User();
            newUser.setEmail(user.getEmail());

            newUser.setPassword(encoder.encode(user.getPassword()));
            newUser.setEnabled(false);
            newUser.setRegistrationDate(modernDate());
            int at = user.getEmail().indexOf('@');
            String username = user.getEmail().substring(0,at);
            username = username.replaceAll("[^a-zA-Z0-9]", "");
            newUser.setUsername(username);
            userRepo.save(newUser);

            createVerificationToken(newUser, token);
            emailService.sendVerificationEmail(newUser.getEmail(), token);
            return ResponseEntity.ok("We have sent a code of your email please check and verify your " +
                    "account " + newUser.getEmail());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to send email. Please try again later.");

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
        verificationToken.setExpiryDate(10); // Set new expiry date
        tokenRepo.save(verificationToken);
    }

    // -----------------------------------
    // Save verification token with user
    // -----------------------------------
    private void createVerificationToken(User newUser, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(newUser);
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(10); // expiry in minutes (assumed)
        tokenRepo.save(verificationToken);

    }

    // -----------------------------------
    // Verify user registration via token
    // -----------------------------------
    public ResponseEntity<?> verifyRegistration(String token ,String email ) {
            Optional<User> checkuser = userRepo.findByEmail(email);

            if(checkuser.isPresent()){
                User getuser= checkuser.get();

                if(getuser.isEnabled()){
                    return ResponseEntity.status(HttpStatus.IM_USED).body("Email Already Verified Please Login");
                }
                Optional<VerificationToken> checkToken = tokenRepo.findByTokenAndUser_Userid(token ,getuser.getUserid());
                if(checkToken.isPresent()){
                    VerificationToken verifiedToken = checkToken.get();
                    // Check if token expired
                    if (verifiedToken.getExpiryDate().before(Calendar.getInstance().getTime())) {
                        return ResponseEntity.status(498).body("Expired Token");
                    }

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
            }


        return ResponseEntity.status(401).body("Invalid Token");
    }

    public ResponseEntity<?> login(DtoUser user, AuthenticationManager authenticationManager) {

        Optional<User> user1 =  userRepo.findByEmail(user.getEmail());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            if (auth.isAuthenticated()) {
                if(user1.isPresent()){
                    User check =  user1.get();

                    if(check.isEnabled()){
                        return ResponseEntity.ok("Login Successful ✅");
                    }
                    return ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED).body("Email Not Verified ❌");
                }

            }



            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password ❌");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password ❌");
        }
    }


    public ResponseEntity<?> verifynewPasswordToken(Token token) {

        Optional<User> checkuser = userRepo.findByEmail(token.getEmail());

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



                Optional<User>  checkuser =  userRepo.findByEmail(user.getEmail());
                if(checkuser.isPresent()){
                      User updateuser =  checkuser.get();

                      updateuser.setPassword(encoder.encode((user.getPassword())));
                      userRepo.save(updateuser);
                      return  ResponseEntity.ok("Password Updated Successfully");
                }

               return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not Found");

    }

    public ResponseEntity<?> saveUserImage(MultipartFile file , String email) {
            User user = userRepo.findByEmail(email).get();
        try
        {
            String uploadDir = "upload/user/";
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.write(path, file.getBytes());

            user.setUserimgurl(uploadDir + fileName);
            userRepo.save(user);
            return ResponseEntity.ok("Upload successfuls");

} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<?> userdashboard(String email) {
       Optional<User>  checkuser = userRepo.findByEmail(email);
        long id = 9089;
    if(checkuser.isPresent()){
        User user =  checkuser.get();
        List<Mechanic>  allmechanics = mechRepo.findAll();
        Map<String , Object> map = new HashMap<>();
        System.out.println("User Cordinates: "+ user.getLastLatitude()+" : " +  user.getLastLongitude());
        List<MechanicDTO > mechanics = new ArrayList<>();
        int i = 1;
        for (Mechanic mechanic : allmechanics) {

            MechanicDTO mechanicDTO = new MechanicDTO();
            mechanicDTO.setName(mechanic.getName());
            mechanicDTO.setMechanicType(mechanic.getMechanictype());
            mechanicDTO.setAveragerating(mechanic.getAverageRating());
            mechanicDTO.setExperience(mechanic.getExperienceyears());
            mechanicDTO.setIsactive(mechanic.isIsactive());
            mechanicDTO.setPhonenumber(mechanic.getPhonenumber());
            mechanicDTO.setMechanicimgurl(mechanic.getMechanicimgurl());
            mechanicDTO.setIsengaged(mechanic.isIsengaged());


            GoogleDistance distance = new GoogleDistance();
            float distancinkm =         distance.CalulateDistance(
                    mechanic.getLatitude() , mechanic.getLongitude(),
                    user.getLastLatitude() , user.getLastLongitude()
            );

            mechanicDTO.setDistance(BigDecimal.valueOf(distancinkm).setScale(1 , RoundingMode.HALF_UP));
            mechanicDTO.setMechaniclocname(distance.getAddressFromLatLng(  mechanic.getLatitude()
                    ,mechanic.getLongitude()));

            mechanics.add(mechanicDTO);


        }
        map.put("mechanics", mechanics);
        map.put("user", user);
        return ResponseEntity.ok( map);

    }

    return  ResponseEntity.status(401).body("Invalid User");
}


}
