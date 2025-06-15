package dating.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import dating.DTOs.db.ProfileDataDbDTO;
import dating.DTOs.db.UserDTO;
import dating.DTOs.db.UserDbDTO;
import dating.FeignClientService.BackendFeignClient;
import dating.FeignClientService.RegistrationData2;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

@RestController
@RequestMapping("/api/v2")
public class ProxyDatabaseRestController {

    private final BackendFeignClient feignClient;

    public ProxyDatabaseRestController(
            BackendFeignClient feignClient) {
        this.feignClient = feignClient;
    }

//    @PostMapping("/proxy-create-user")
//    public ResponseEntity<Object> proxyCreateUser(@RequestBody Object userDbDTO) {
//        System.out.println("debugging proxyCreateUser");
//        return feignClient.createNewUser(userDbDTO);
//    }

    @PostMapping("/proxy-create-user")
    public ResponseEntity<Object> proxyCreateUser(@RequestBody UserDbDTO userDbDTO) {
        System.out.println("Request received in proxyCreateUser"); // Debug log
        System.out.println("Received payload: " + userDbDTO); // Log the received JSON
        return feignClient.createNewUser(userDbDTO);
    }

    @GetMapping("/proxy-user/{id}")
    public ResponseEntity<UserDTO> proxyGetUser(@PathVariable Long id) {
        ResponseEntity<UserDTO> userById = feignClient.getUserById(id);
        return userById;
    }

//    @GetMapping("/proxy-user/{id}")
//    public UserDTO proxyGetUser(@PathVariable Long id) {
//        UserDTO userById = feignClient.getUserById(id);
//        return userById;
//    }


    @GetMapping("/proxy-user-with-data/{id}")
    public ResponseEntity<UserDbDTO> proxyGetUserWithData(@PathVariable Long id) {
        return feignClient.getUserWithDataById(id);
    }


//    @GetMapping("/proxy-user-with-data/{id}")
//    public UserDbDTO proxyGetUserWithData(@PathVariable Long id) {
//        UserDbDTO userWithDataById = feignClient.getUserWithDataById(id);
//        return userWithDataById;
//    }



//    @PatchMapping("/proxy-profile-data-update")
//    public ProfileDataDbDTO updateProfileDataById(@RequestBody ProfileDataDbDTO profileDataDbDTO) throws JsonProcessingException
//    {
//        return feignClient.updateProfileDataById(profileDataDbDTO);
//    }


}

