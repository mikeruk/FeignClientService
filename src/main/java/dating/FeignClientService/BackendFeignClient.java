package dating.FeignClientService;

import dating.DTOs.db.ProfileDataDbDTO;
import dating.DTOs.db.UserDTO;
import dating.DTOs.db.UserDbDTO;
import dating.config.CustomFeignClientConfiguration;
import dating.configExcludedFromComponentScan.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        contextId = "daoClient",
        name = "backend-service", // Resolved dynamically via Eureka
        configuration = {CustomFeignClientConfiguration.class,
                         MyCustomFeignCachingConfig.class,
                         LoggingFeignClientConfiguration.class,
                         ApiVersionFeignConfig.class }      // ← our new contract}
)

// The annotation /@ApiVersion("/api/v1") set on the class would apply to *all* methods in
// this client OR you could put it on each single method below - works also!
// If you use it on class level, then dont use it below on method level to avoid colisions.
@ApiVersion("/api/v1")
public interface BackendFeignClient {

    //@PostMapping("/api/v1/create-new-user")    //<-- Use this when @ApiVersion("/api/v1") is not applied at all!
    //@ApiVersion("/api/v1")  //<-- dont apply it here on method level, if you have applied it on class level.
    @PostMapping(value = "create-new-user")  // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<Object> createNewUser(@RequestBody UserDbDTO userDbDTO);

    //@GetMapping("/api/v1/user/{id}")  //<-- Use this when @ApiVersion("/api/v1") is not applied at all!
    //@ApiVersion("api/v1") //<-- dont apply it here on method level, if you have applied it on class level.
    @GetMapping(value = "/user/{id}") // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id);

//    @GetMapping("/api/v1/user/{id}")
//    UserDTO getUserById(@PathVariable("id") Long id);

    //@GetMapping("/api/v1/user-with-data/{id}")   //<-- Use this when @ApiVersion("/api/v1") is not applied at all!
    //@ApiVersion("api/v1") //<-- dont apply it here on method level, if you have applied it on class level.
    @GetMapping("user-with-data/{id}") // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<UserDbDTO> getUserWithDataById(@PathVariable("id") Long id);




//    @GetMapping("/api/v1/user-with-data/{id}")
//    UserDbDTO getUserWithDataById(@PathVariable("id") Long id);


//    @PatchMapping("/api/v1/profile-data-update")
//    ProfileDataDbDTO updateProfileDataById(@RequestBody ProfileDataDbDTO profileDataDbDTO);

}

