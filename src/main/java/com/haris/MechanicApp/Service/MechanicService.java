package com.haris.MechanicApp.Service;

import com.haris.MechanicApp.Model.Mechanic.Mechanic;
import com.haris.MechanicApp.Model.Mechanic.MechanicCredientialsDTO;
import com.haris.MechanicApp.Model.Mechanic.MechanicNumnerDto;
import com.haris.MechanicApp.Model.Mechanic.MechanicRegistrationDto;
import com.haris.MechanicApp.Model.Verification.Role;
import com.haris.MechanicApp.Model.Verification.User;
import com.haris.MechanicApp.Repository.MechanicRepository;
import com.haris.MechanicApp.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MechanicService    {

    @Autowired
    private MechanicRepository mechanicRepository;
    @Autowired
    private UserRepository userRepository;

    private  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

//    @Override
//    public UserDetails loadUserByUsername(String phonenumber) throws UsernameNotFoundException {
////        Optional<User> checkUser = userRepo.findByEmail(email);
//
//        Optional<Mechanic> checkmechanic = mechanicRepository.findByPhonenumber(phonenumber);
//
//        if (checkmechanic.isPresent()) {
//
//           Mechanic mechanic   = checkmechanic.get();
//
//            if (mechanic.isIsverified()) {
//                return new MyPrincipalMechanic(mechanic);
//            }
//            throw new UsernameNotFoundException("Mechanic Not Verified");
//        }
//
//        throw new UsernameNotFoundException("Mechanic Not Found");
//    }

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

        try
        {
            Optional<Mechanic> checknumbermechanic = mechanicRepository.findByPhonenumber(mechanicdata.getPhonenumber());
                Optional<User> user = userRepository.findById(mechanicdata.getUserid());




            if (user.isEmpty()){

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not Created ");
            }

            if(!mechanicdata.isOtpVerified()){
                System.out.println("Otp Verifed nh hwa");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("OTP not verified");
            }
            if(user.isPresent()){
                User  mechanicAndduser = user.get();
                if (mechanicRepository.existsByUser(mechanicAndduser)) {
                    return ResponseEntity.ok("Already a mechanic");
                }

                Mechanic newregisteredmechanic = new Mechanic();
                String uploadDir = "upload/mechanic/";
                Files.createDirectories(Paths.get(uploadDir));
                String mechaniciimagefile = UUID.randomUUID() + "_" + mecanicimg.getOriginalFilename();
                String cnicfrontfile = UUID.randomUUID() + "_" + cnicbackimg.getOriginalFilename();
                String cnicbackfile = UUID.randomUUID() + "_" + cnicfrontimg.getOriginalFilename();


                Path pathmechimg = Paths.get(uploadDir + mechaniciimagefile);
                Path pathcnicfront = Paths.get(uploadDir + cnicfrontfile);
                Path pathcnicback = Paths.get(uploadDir + cnicbackfile);

                Files.write(pathmechimg , mecanicimg.getBytes());
                Files.write(pathcnicfront , cnicfrontimg.getBytes());
                Files.write(pathcnicback, cnicbackimg.getBytes());


                mechanicAndduser.getRoles().add(Role.MECHANIC);
                userRepository.save(mechanicAndduser);

                newregisteredmechanic .setUser (mechanicAndduser);

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

                newregisteredmechanic.setMechanicimgurl(uploadDir + mechaniciimagefile);
                newregisteredmechanic.setCnicfronturl(uploadDir + cnicfrontfile);
                newregisteredmechanic.setCnicbackurl(uploadDir + cnicbackfile);


                System.out.println("hogya mechanic registered");
                mechanicRepository.save(newregisteredmechanic);
                return ResponseEntity.ok("Mechanic is registered");

            }


        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mechanic Can not register");




  }
        catch ( Exception e){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());

        }



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
}
