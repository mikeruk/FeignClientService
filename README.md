
   NB!!! Before deciding to use FeignClient for your project, know that 
Feign does not send @PATCH requests,
Feign’s default HTTP client (which under the covers is Client.Default, a thin wrapper around 
java.net.HttpURLConnection) does not support the PATCH verb. HttpURLConnection in most JDKs only 
allows the methods: DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE
and will throw a ProtocolException if you try to call setRequestMethod("PATCH").
, along other difficult moments like customizing Feign builder()
, which did not work at all - see comments of the builder(). And also other difficult moments,
as described below:
   
NB! I have applied Caching capability on URL - don't wonder why you receive same response for same URL!?
You need to turn off the caching if you want to receive updated responses from DB!

NOTE: If we create both @Configuration bean and configuration properties (application.yml),
configuration properties will win.


I created this Feign Client Service thinking, it does not implement yet any Load Balancing functionality.
The feign client does Load Balancing 'Round Robin' by default! 
The applied load balancer is Spring Cloud LoadBalancer, coming directly from Spring.

So, here is explanation of what a minimal creation of Feign Client looks like:

My build gradle dependencies are these:
```gradle
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web
	implementation group: 'org.springframework.boot', name: 'spring-boot-starter-web', version: '3.4.2'
// https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-netflix-eureka-client
	implementation group: 'org.springframework.cloud', name: 'spring-cloud-starter-netflix-eureka-client', version: '4.2.0'
// https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-openfeign
	implementation group: 'org.springframework.cloud', name: 'spring-cloud-starter-openfeign', version: '4.2.0'
	compileOnly 'org.projectlombok:lombok'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

	//Add Validations for the Rest API
	// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-validation
	implementation group: 'org.springframework.boot', name: 'spring-boot-starter-validation', version: '3.4.2'
	// https://mvnrepository.com/artifact/jakarta.validation/jakarta.validation-api
	implementation group: 'jakarta.validation', name: 'jakarta.validation-api', version: '3.1.1'

	// Added for the HttpMessageConverter<?> converter = new MappingJackson2HttpMessageConverter(customObjectMapper());
	// inside the public class CustomFeignClientConfiguration
}
```


My application.yml is this:
```yml
server:
  port: 8082  # Feign Client Service runs on port 8082

spring:
  application:
    name: feign-client-service
  cloud:
    openfeign:


logging:
  level:
    feign: DEBUG  # Logs Feign internals. This is in addition to the configuration in the class ApplicationBeanConfiguration. Both configurations are needed.
    #com.example.dating: DEBUG  # Adjust to your package if needed


eureka:
  client:
    service-url:
      defaultZone: "http://localhost:8761/eureka/"
    register-with-eureka: true
    fetch-registry: true
```

My Feign client is defined like that:

```java
import dating.DTOs.db.UserDbDTO;
import dating.config.CustomFeignClientConfiguration;
import dating.controllers.ProxyDatabaseRestController;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;

@FeignClient(
        contextId = "daoClient",
        name = "backend-service", // Resolved dynamically via Eureka
        configuration = CustomFeignClientConfiguration.class
)
public interface BackendFeignClient {

    @PostMapping("/api/v1/create-new-user")
    ResponseEntity<Object> createNewUser(@RequestBody UserDbDTO userDbDTO);

    @GetMapping("/api/v1/user/{id}")
    ResponseEntity<Object> getUserById(@PathVariable("id") Long id);

//    @GetMapping("/api/v1/user-with-data/{id}")
//    ResponseEntity<UserDbDTO> getUserWithDataById(@PathVariable("id") Long id);

//    @GetMapping("/api/v1/user-with-data/{id}")
//    ResponseEntity<Date> getUserWithDataById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/user-with-data/{id}")
    ProxyDatabaseRestController.Response getUserWithDataById(@PathVariable("id") Long id);

}
```

My ApplicationBeanConfiguration.java is this:

```java
import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeanConfiguration
{

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(5000, 10000); // 5 sec connect, 10 sec read timeout
    }
}
```


My CustomFeignClientConfiguration.java is this:
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CustomFeignClientConfiguration
{

    private static final Logger logger = LoggerFactory.getLogger(CustomFeignClientConfiguration.class);

  


   // Sometimes the backend service will be sending Java objects, which contain not only LocaleDateTime objects,
   // but also LocalDate or Date objects. Therefore, we also need to add serializers/de-serializers
   // for these types also, like so:
   @Bean
   public ObjectMapper customObjectMapper() {
      JavaTimeModule javaTimeModule = new JavaTimeModule();

      // Serializer for LocalDateTime (output format: yyyy-MM-dd'T'HH:mm), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'
      // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addSerializer(LocalDateTime.class,
              new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'")));

      // Deserializer for LocalDateTime with primary and fallback formats
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addDeserializer(LocalDateTime.class,
              new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) {
                 @Override
                 public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText();
                    try {
                       return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
                    } catch (DateTimeParseException e) {
                       logger.debug("Failed to parse LocalDateTime with yyyy-MM-dd'T'HH:mm:ss.SSS: {}, trying other formats", text);
                       try {
                          return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                       } catch (DateTimeParseException e2) {
                          logger.debug("Failed to parse LocalDateTime with yyyy-MM-dd'T'HH:mm: {}, trying ISO format", text);
                          try {
                             return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                          } catch (DateTimeParseException e3) {
                             logger.error("Failed to parse LocalDateTime: {}", text, e3);
                             throw e3;
                          }
                       }
                    }
                 }
              });

      // Serializer for LocalDate (output format: yyyy-MM-dd), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInLocalDate:noMinsInLocalDate o''clock!'
      // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addSerializer(LocalDate.class,
              new LocalDateSerializer(DateTimeFormatter.ofPattern("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInLocalDate:noMinsInLocalDate o''clock!'")));

      // Deserializer for LocalDate with primary and fallback formats
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addDeserializer(LocalDate.class,
              new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")) {
                 @Override
                 public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText();
                    try {
                       return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (DateTimeParseException e) {
                       logger.debug("Failed to parse LocalDate with yyyy-MM-dd: {}, trying other formats", text);
                       try {
                          return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyyMMdd"));
                       } catch (DateTimeParseException e2) {
                          logger.debug("Failed to parse LocalDate with yyyyMMdd: {}, trying ISO format", text);
                          try {
                             return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                          } catch (DateTimeParseException e3) {
                             logger.error("Failed to parse LocalDate: {}", text, e3);
                             throw e3;
                          }
                       }
                    }
                 }
              });

      // Serializer for Date (output format: yyyy-MM-dd'T'HH:mm:ss.SSS), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'
      // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addSerializer(Date.class,
              new JsonSerializer<Date>() {
                 private final SimpleDateFormat formatter = new SimpleDateFormat("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInDate:noMinsInDate o''clock!'");

                 @Override
                 public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    gen.writeString(formatter.format(value));
                 }
              });

      // Deserializer for Date with primary and fallback formats
      // If I don't need it, I can remove it/comment it out!
      javaTimeModule.addDeserializer(Date.class,
              new JsonDeserializer<Date>() {
                 private final SimpleDateFormat primaryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                 private final SimpleDateFormat fallback1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                 private final SimpleDateFormat fallback2 = new SimpleDateFormat("yyyy-MM-dd");

                 @Override
                 public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText();
                    try {
                       return primaryFormat.parse(text);
                    } catch (ParseException e) {
                       logger.debug("Failed to parse Date with yyyy-MM-dd'T'HH:mm:ss.SSS: {}, trying other formats", text);
                       try {
                          return fallback1.parse(text);
                       } catch (ParseException e2) {
                          logger.debug("Failed to parse Date with yyyy-MM-dd'T'HH:mm: {}, trying yyyy-MM-dd", text);
                          try {
                             return fallback2.parse(text);
                          } catch (ParseException e3) {
                             logger.error("Failed to parse Date: {}", text, e3);
                             throw new IOException("Failed to parse Date: " + text, e3);
                          }
                       }
                    }
                 }
              });

      logger.info("Custom date mappers for LocalDateTime, LocalDate, and Date loaded");
      return Jackson2ObjectMapperBuilder.json()
              .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .modules(javaTimeModule)
              .build();
   }

    // NB! At this point, the above ObjectMapper Bean is completely enough for the feign client to work and use it!
   // I don't really need to explicitly customize a new Decoder, as I do below. This is because apparently the default
   // feign Decoder uses the ObjectMapper and this is completely enough to do my custom date formats!
   // Below, the code @Bean public Decoder feignDecoder(ObjectMapper customObjectMapper) can be deleted / commented out
   // and the object mapper above would still be applied in feign client!
   // HOWEVER, for the purpose of a Demo how to customize feign Decoder, I still implement the Bean below:
   // Below is the same configuration for Decoder feignDecoder(...) as above marked with START and END, but
   // just with few additions, to make the configuration more advanced:

   // Sometimes the backend service will be sending not simple POJO objects as a response body, but could probably
   // wrap them in other objects like ResponseEntity<EntityModel<User>> which would throw error if we do not implement
   // the Decoder like this below:
   @Bean
   public Decoder feignDecoder(ObjectMapper customObjectMapper)
   {
       
       
      MappingJackson2HttpMessageConverter jacksonConverter =
              new MappingJackson2HttpMessageConverter(customObjectMapper);
      


      HttpMessageConverters converters = new HttpMessageConverters(jacksonConverter);

      return (response, type) -> {
         logger.info("Decoding response for type: {}", type.getTypeName());

         if (type instanceof ParameterizedType parameterizedType
                 && parameterizedType.getRawType().equals(ResponseEntity.class))
         {
            Type bodyType = parameterizedType.getActualTypeArguments()[0];
            logger.info("Deserializing ResponseEntity with body type: {}", bodyType.getTypeName());
            Object body = new SpringDecoder(() -> converters).decode(response, bodyType);
            return ResponseEntity.status(response.status())
                    .headers(new HttpHeaders())
                    .body(body);
         }
         logger.info("Using default decoder for type: {}", type.getTypeName());
         return new SpringDecoder(() -> converters).decode(response, type);
      };


      // Postman Client sends request to: http://localhost:8082/api/v2/proxy-user/8
      //
      // And this is the Controller in feign client service:
      //        @GetMapping("/proxy-user/{id}")
      //        public ResponseEntity<UserDTO> proxyGetUser(@PathVariable Long id) {
      //        ResponseEntity<UserDTO> userById = feignClient.getUserById(id);
      //        return userById;
      //        }
      //
      // This is the Controller in backend service:
      //        @GetMapping("/user/{id}")
      //        public ResponseEntity<EntityModel<User>> getUserById(@PathVariable Long id) throws JsonProcessingException {
      //        User user = userService.selectUserByPrimaryKey(id).orElse(null);
      //        if (user == null) {
      //            return ResponseEntity.notFound().build();
      //        }
      //        return ResponseEntity.ok(EntityModel.of(user, addLinksToUser(user)));
      //        }
      //
      // And the Postman client receives the response body which looks so:
      //  {
      //      "id": 8,
      //      "ts": "Arrr, it be the 1 day of January in the year 0001 at the hour of 12:00 am o'clock!"
      //  }
      //
      // the custom date format was implemented with success !!!
   }

   public void testDateFormat(ObjectMapper objectMapper) {

      LocalDateTime now = LocalDateTime.now();
      LocalDate today = LocalDate.now();
      Date currentDate = new Date();

      try {
         // Test LocalDateTime
         String formattedDateTime = objectMapper.writeValueAsString(now);
         logger.info("Serialized LocalDateTime: {}", formattedDateTime);
         LocalDateTime deserializedDateTime = objectMapper.readValue(formattedDateTime, LocalDateTime.class);
         logger.info("Deserialized LocalDateTime: {}", deserializedDateTime);

         // Test LocalDate
         String formattedLocalDate = objectMapper.writeValueAsString(today);
         logger.info("Serialized LocalDate: {}", formattedLocalDate);
         LocalDate deserializedLocalDate = objectMapper.readValue(formattedLocalDate, LocalDate.class);
         logger.info("Deserialized LocalDate: {}", deserializedLocalDate);

         // Test Date
         String formattedDate = objectMapper.writeValueAsString(currentDate);
         logger.info("Serialized Date: {}", formattedDate);
         Date deserializedDate = objectMapper.readValue(formattedDate, Date.class);
         logger.info("Deserialized Date: {}", deserializedDate);
      } catch (Exception e) {
         logger.error("Error processing date formats", e);
      }
   }      
    
    
}
```
NB! The only little thing I don't understand is why I still need to include that configuration in the
component scan like this:
```java
@ComponentScan(basePackages = {
"dating.controllers" 
,"dating.config" // that package contains the  CustomFeignClientConfiguration class
})...
```
, since I have already imported that CustomFeignClientConfiguration in the Feign Client itself:
```java
@FeignClient(
        contextId = "daoClient",
        name = "backend-service", // Resolved dynamically via Eureka
        configuration = {CustomFeignClientConfiguration.class}
)
public interface BackendFeignClient (...)
```

, but lets move on, Next:
I have this java DTO object:

```java
public class UserDbDTO {

    Long id;

    @Valid
    @NotNull(message = "Registration data is required")
    private RegistrationDataDbDTO registrationDataDbDTO;

    @Valid
    @NotNull(message = "Profile data is required")
    private ProfileDataDbDTO profileDataDbDTO;

    @Valid
    @NotNull(message = "Description data is required")
    private DescriptionDataDbDTO descriptionDataDbDTO;

    public UserDbDTO() {
    }

    public UserDbDTO(Long id, RegistrationDataDbDTO registrationDataDbDTO, ProfileDataDbDTO profileDataDbDTO, DescriptionDataDbDTO descriptionDataDbDTO) {
        this.id = id;
        this.registrationDataDbDTO = registrationDataDbDTO;
        this.profileDataDbDTO = profileDataDbDTO;
        this.descriptionDataDbDTO = descriptionDataDbDTO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationDataDbDTO getRegistrationDataDbDTO() {
        return registrationDataDbDTO;
    }

    public void setRegistrationDataDbDTO(RegistrationDataDbDTO registrationDataDbDTO) {
        this.registrationDataDbDTO = registrationDataDbDTO;
    }

    public ProfileDataDbDTO getProfileDataDbDTO() {
        return profileDataDbDTO;
    }

    public void setProfileDataDbDTO(ProfileDataDbDTO profileDataDbDTO) {
        this.profileDataDbDTO = profileDataDbDTO;
    }

    public DescriptionDataDbDTO getDescriptionDataDbDTO() {
        return descriptionDataDbDTO;
    }

    public void setDescriptionDataDbDTO(DescriptionDataDbDTO descriptionDataDbDTO) {
        this.descriptionDataDbDTO = descriptionDataDbDTO;
    }

    @Override
    public String toString() {
        return "UserDbDTO{" + '\n' +
                "registrationDataDbDTO=" + registrationDataDbDTO +
                ", profileDataDbDTO=" + profileDataDbDTO +
                ", descriptionDataDbDTO=" + descriptionDataDbDTO +
                '}';
    }
}
```

                                          DESERIALIZATION
Now that we have customized our Feign client to do Deserialization, lets explain what DESERIALIZATION is:
Below is a “zoom‑out” view of what actually happens when a SpringCloudOpenFeign client receives an HTTP response and turns it into
the Java object you asked for. Think of it as a little assembly line:


┌──────────────┐   ① raw bytes
│  TCP socket  │─────────────────►
└──────────────┘
│
▼
┌────────────────┐   ② status‑line, headers, body‑stream
│   Feign core   │   (still just bytes)
└────────────────┘
│
▼
┌──────────────────┐   ③ chooses the Decoder you configured
│  SpringDecoder   │
└──────────────────┘
│
▼
┌────────────────────────┐   ④ walks the ordered list until one accepts
│ HttpMessageConverters  │────────────────────────────────────┐
└────────────────────────┘                                    │
│            │              “Can you read                  │
│ no → next  │              ‎type X & media‑type Y?”        │
▼            ▼                                               ▼
┌────────────────────────────────┐    ⑤ first *matching* converter wins
│ MappingJackson2HttpMessageConv │──────────────────┐
└────────────────────────────────┘                  │
│                                            │
▼                                            │
┌────────────────┐    ⑥ delegates JSON ↔ POJO       │
│  ObjectMapper  │◄─────────────────────────────────┘
└────────────────┘
│
▼
┌────────────────┐   ⑦ fully‑formed Java object (or ResponseEntity<T>)
│ Your code gets │
│      it        │
└────────────────┘



         Step                            	      What’s going on?                               	Where your bean shows up
① Raw bytes arrive over the wire.  
Feign hasn’t looked at them yet.                    Network layer.                                            	—


② Feign core turns the bytes into
a Response object (status, headers, 
body stream).                                       feign.Response                                              —


③ SpringDecoder (the one you 
build in the @Bean) is asked to 
turn that Response into the type 
Feign caller expects 
(Order, List<Customer>, etc.).                      new SpringDecoder(() -> converters)              Your custom Decoder wrapper picks off ResponseEntity<T> then delegates.


④ HttpMessageConverters are just an ordered
list of converters. Spring walks it in the 
order supplied and asks each converter 
“Can you read this media‑type into that class?”      HttpMessageConverters(jacksonConverter)         Putting your MappingJackson2HttpMessageConverter first guarantees it gets asked first.     


⑤ When the content type is application/json 
(or none at all) and the target type is not 
String or byte[], the Jackson converter 
answers “Yes” and wins.                              MappingJackson2HttpMessageConverter                          —


⑥ That converter just calls 
ObjectMapper.readValue(InputStream, targetType). 
Here it uses the exact ObjectMapper instance 
you built, so all those fancy LocalDateTime 
deserializers kick in.                               customObjectMapper()                            Your JavaTimeModule, pirate‑sounding date formats, etc.


⑦ The fully built Java object is returned 
back through SpringDecoder → Feign → your 
service method.


      Why you sometimes see strings or JSON in logs
At the wire level everything is bytes.

Inside Feign the body stays an InputStream until a converter reads it.

If you declare the method return type as String the StringHttpMessageConverter (earlier in the list) will accept the job, 
read the body as plain text, and you’ll get a String.

Likewise for byte[], Resource, XML, etc. - each has its own converter.

      How the order really works
Spring Boot builds a default list (String → ByteArray → Jackson → …).

When you supply new HttpMessageConverters(customJackson), that bean is taken as the authoritative list, so customJackson 
sits at index 0, followed by the defaults
Home.

When SpringDecoder asks to read, Spring loops in list order; the first converter whose canRead() matches both the response 
Content‑Type and the target class is used.

Your code never sees the rest of the converters.

      Putting it all together for your configuration
You override the default Jackson converter with one that uses customObjectMapper().

You wrap SpringDecoder so you can unwrap a generic ResponseEntity<T> before decoding.

For JSON endpoints everything now flows through your ObjectMapper, so the three‑level date‑parsing fallback behaves exactly 
as you coded it.


                                          SERIALIZATION
serialization is the mirror image of what we walked through, just travelling up‑stream instead of down‑stream. 
Here’s the ladder in the opposite direction:

Java POJO ──► Feign method call
│
┌─────▼────────────────────┐ ⑦ Your object (Order, Foo, etc.)
│      Feign proxy         │ calls the encoder
└─────▲────────────────────┘
│
┌─────▼────────────────────┐ ⑥ SpringEncoder picks the
│   SpringEncoder          │ first HttpMessageConverter
└─────▲────────────────────┘ that can **write** the type
│
┌─────▼─────────────────────────┐ ⑤ Ordered list from your bean
│  HttpMessageConverters        │ String → ByteArray → Jackson …
└─────▲─────────────────────────┘      (first match wins)
│
┌─────▼───────────────────┐ ④ MappingJackson2HttpMessageConverter
│ MappingJackson2…        │ uses your **custom ObjectMapper**
└─────▲───────────────────┘
│        writeValue() → bytes
┌─────▼────────────┐ ③ Feign core stuffs bytes + headers
│   RequestTemplate │ into a `feign.Request`
└─────▲────────────┘
│
┌─────▼───────┐ ② underlying client (OkHttp, Apache HC etc.)
│ HTTP client  │ writes to the socket
└─────▲───────┘
│
TCP socket ◄─── bytes on the wire ①
The outcome: same pipeline, opposite flow, with the very same ordering rules and the very same ObjectMapper you customised. 
If you were to put StringHttpMessageConverter ahead of the Jackson one, then calling a Feign method whose parameter is 
String would short‑circuit JSON completely and just write the plain text body - exactly the mirror image of what we saw on 
the read side.

Now, lets finally start the application!
And I start/run the application so:


```java
@EnableFeignClients
@EnableDiscoveryClient
@ComponentScan(basePackages = {
		//"dating.FeignClientService", //no need to specify this package, if the @FeignClient interface is in the same package or a sub-package of FeignClientServiceApplication. The default scanning from @EnableFeignClients will pick it up.
		"dating.controllers" // wherever your controller lives
})
@SpringBootApplication
public class FeignClientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeignClientServiceApplication.class, args);
	}

}
```

And my Application runs fine and according to research with GPT the Load Balancing 'Round Robin' is
already being implemented by my feign client. I just don't see it, because my feign client currently
connects to only one backend service. In reality, I must have 2 or more services and the Feign Client
would load balance among them!

The applied load balancer is Spring Cloud LoadBalancer, coming directly from Spring.

Also, at this point I am not applying any custom configuration for the Load Balancer. If I apply any, 
it would be visible in the source code.

Finally, I started TWO INSTANCES Of the backendservice. How? I simply added a second configuration like so:

```textmate
Run Two Instances of the Same Service

Go to Run > Edit Configurations...

Click the + and choose Spring Boot (or duplicate the existing one).

In the duplicated configuration, go to VM options and set a different port:

-Dserver.port=8079

Now you can run both configurations simultaneously. And notice that response comes from different PORT Numbers!
This is how I can be sure that the load balancer really balances between different services.
BUT later I will implement Caching in the feign client - this will prevent calling the backend service and I will be receiving responses 
directly from the Cache (of the client) - that could confuse me and make me think that the Load Balancer does not work wny more, because
all response will seem to come from same port, but in reality they are coming from the Cache memory of the client
and the backend service was never contacted!

```
Next, when I send a request via Postman client, I see in the terminal logs that either one 
service is called OR the other service is called. So - this proves that the Feign Client indeed
does "Round Robin Load Balancing".
(Next, I can experiment with different configurations of the Load Balancing.),

BUT first I decided to experiment to modify some of the already available parameters:







    IMPROVING LOGGING of requests AND activating logging and reporting MicrometerObservationCapability



First, activate FULL logging level like so:
```yaml
spring:
  application:
    name: feign-client-service
  cloud:
    openfeign:
      client:
        config:
          default:
            loggerLevel: FULL  # Enables FULL logging for all Feign clients


logging:
  level:
    feign: DEBUG  # Logs Feign internals. This is in addition to the configuration in the class ApplicationBeanConfiguration. Both configurations are needed.
    #com.example.dating: DEBUG  # Adjust to your package if needed
    dating.FeignClientService: DEBUG
```
Just by doing this (actually the logging: level:... blabla is probably not really needed) will make spring to output
more details in the console log about the incoming HTTP Requests. THe newly visible details are there:
```cmd
feign.template.Template                  : Explicit slash decoding specified, decoding all slashes in uri
2025-04-06T11:08:00.954+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] ---> GET http://backend-service/api/v1/user/8 HTTP/1.1
2025-04-06T11:08:00.954+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] ---> END HTTP (0-byte body)
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] <--- HTTP/1.1 200 (74ms)
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] connection: keep-alive
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] content-type: application/prs.hal-forms+json
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] date: Sun, 06 Apr 2025 09:08:01 GMT
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] keep-alive: timeout=60
2025-04-06T11:08:01.028+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] transfer-encoding: chunked
2025-04-06T11:08:01.029+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] vary: Access-Control-Request-Headers
2025-04-06T11:08:01.029+02:00 DEBUG 29596 --- [feign-client-service] [nio-8082-exec-3] d.FeignClientService.BackendFeignClient  : [BackendFeignClient#getUserById] vary: Access-Control-Request-Method
2025-04-06T11:08:01.029+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] vary: Origin
2025-04-06T11:08:01.029+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] {"id":8,"ts":"2025-02-09T18:30:46.850014","_links":{"self":{"href":"http://BlaBlaBla:8081/api/v1/user/8"}}}
2025-04-06T11:08:01.029+02:00 DEBUG 29596 --- [BackendFeignClient#getUserById] <--- END HTTP (105-byte body)
```
Ok, that's SUCCESS!
Second, lets activate MicrometerObservationCapability (according to this text 
"MicrometerObservationCapability micrometerObservationCapability: If feign-micrometer is on the classpath and 
ObservationRegistry is available" from the docs: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults )
To achieve this, simply add this to application.yml
```yaml
management: # activates reporting metrics
  observations:
    key-values:
      enabled: true
  metrics:
    enable:
      all: true
  endpoints:
    web:
      exposure:
        include: "*"
```
Now you have an endpoint: http://localhost:8082/actuator/metrics/http.client.requests which provides metrics like:
```json
{"name":"http.client.requests","baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":2.0},{"statistic":"TOTAL_TIME","value":0.0171754},{"statistic":"MAX","value":0.0126384}],"availableTags":[{"tag":"exception","values":["none"]},{"tag":"method","values":["POST","GET"]},{"tag":"error","values":["none"]},{"tag":"uri","values":["none"]},{"tag":"outcome","values":["SUCCESS"]},{"tag":"enabled","values":["true"]},{"tag":"client.name","values":["localhost"]},{"tag":"status","values":["204","200"]}]}
```

Let’s break it down:
✅ You Have the Core Feature Working
Yes — with the /actuator/metrics/http.client.requests endpoint working, you now:
Know how many Feign requests were made
See total time taken
See breakdown by:
HTTP method (GET, POST, etc.)
Status (200, 204, etc.)
Exception (if any)
Outcome (SUCCESS, CLIENT_ERROR, etc.)
That’s the default behavior of MicrometerObservationCapability via feign-micrometer.

🚀 Want More? Here Are Your Upgrade Options:
1. 💬 Add Custom Tags to Metrics
   Let’s say you want to tag requests by feature, user type, service tier, etc. — you can do that by writing a custom ObservationHandler or ObservationFilter.
📌 For example, add a tag like "feature":"user-profile" to all /user/{id} calls.
This gives richer context in dashboards.

2. 🧭 Add Distributed Tracing (Zipkin, Jaeger, OTEL)
   If you want to trace Feign calls across microservices, you can hook into:
Micrometer Tracing
Zipkin, OpenTelemetry, Jaeger
This adds full span context to each request and lets you see what happened across systems in one view.
To enable it:
```yaml
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
```
And then:
management:
tracing:
sampling:
probability: 1.0
🔥 Now every Feign request appears in a trace viewer!

3. 📊 Push Metrics to Prometheus + Grafana
   With this setup:
```yaml
implementation 'io.micrometer:micrometer-registry-prometheus'
```
You get metrics exposed at:
```cmd
http://localhost:8082/actuator/prometheus
```
Hook this into Prometheus and view it in Grafana for alerts, visual dashboards, trend lines, etc.
4. 🧪 Filter or Format URI Tags Better
   You might have noticed your metrics show:
```cmd
"uri": ["none"]
```

That's because Feign doesn't have native URI template awareness like Spring MVC.
You can improve this by:
Switching from uri=none to custom URIs
Using Spring MVC-style Contracts (which help map method + URI more clearly)
Registering a MeterFilter to group or normalize URIs
5. 📉 Alerting & Thresholds
   Once you have the metrics:

Add Grafana alerts (e.g., when Feign latency > 500ms)
Use Micrometer timers to monitor individual client performance
Alert when status=5xx or exception!=none

6. 📄 Export Metrics to Logs (for Splunk, ELK, etc.)
   Want to push metrics to logs instead of a Prometheus endpoint?

You can register a custom MeterRegistry that logs every observation, like:

```cmd
registry.config().meterFilter(MeterFilterCommon.logEachObservation());
```
🧠 Summary: You’re at the “Basecamp” of Observability — and there’s more above if you want it
Feature	                                    Already Enabled 	Can Be Added
Basic Feign request metrics             	✅ Yes	—
Prometheus / Grafana                    	❌ Not yet          ✅ Yes
Distributed Tracing                     	❌ Not yet	        ✅ Yes
Custom Tags	                                ❌ Not yet	        ✅ Yes
URI cleanup                             	❌ Not yet	        ✅ Yes
Alerting	                                ❌ Not yet	        ✅ Yes

A Question: Which one do we implement - MicrometerObservationCapability OR MicrometerCapability?
You are using MicrometerObservationCapability
This is the preferred, newer, more powerful option because:
It integrates with the Observations API (which supports tracing, spans, and better structured metrics)
It’s more extensible (e.g., tagging, custom observation handlers, tracing hooks)
If you ever downgraded to Spring Boot 2.x, or removed the ObservationRegistry, it would fall back to
MicrometerCapability, which provides metrics only (no spans/tracing).
Let me know if you want to test it or even simulate both paths just to feel the difference!


            START OF EXPERIMENT WITH cachingCapability

            # NEXT: CachingCapability cachingCapability
I experiment and try to implement custom cachingCapability in my project. 
My approach is to modify only the settings for cachingCapability locally for the feign client without
affecting or overwriting other default settings. We know so far that to achieve this, we need a custom
configuration class like this example that we already have:

```java

public class CustomFeignClientConfiguration { 
    //... which is not annotated with @Configuration
 } 
```
, then this class is directly added as a configuration file CustomFeignClientConfiguration in the feign so:
```java
@FeignClient(
        contextId = "daoClient",
        name = "backend-service", // Resolved dynamically via Eureka
        configuration = {CustomFeignClientConfiguration.class})
public interface BackendFeignClient { // ...
 }
```
And that's it! The configuration is applied successfully. But when I apply the custom cachingCapability
I must do this:
First, again create the custom configuration class like so:

```java
public class MyCustomCachingCapability implements Capability {

    @Override
    public Client enrich(Client client) {
        // Wrap the existing client with caching logic
        return new CachingClient(client);
    }

    // Internal CachingClient that intercepts requests and caches them
    private static class CachingClient implements Client {
        private static final Logger logger = LoggerFactory.getLogger(CachingClient.class);
        private static final Map<String, Response> CACHE = new ConcurrentHashMap<>();

        private final Client delegate;

        public CachingClient(Client delegate) {
            this.delegate = delegate;
        }

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            String key = generateCacheKey(request);
            logger.info("Feign request to URL: {}", key);

            // Check cache
            Response cachedResponse = CACHE.get(key);
            if (cachedResponse != null) {
                logger.info("Cache HIT for key: {}", key);
                return cloneResponse(cachedResponse); // Return a fresh clone
            }

            logger.info("Cache MISS for key: {}", key);
            // Execute the request
            Response response = null;
            try {
                response = delegate.execute(request, options);
                // Read and cache the body, preserving the original response
                byte[] bodyData = readBody(response);
                Response clonedResponse = Response.builder()
                        .status(response.status())
                        .reason(response.reason())
                        .headers(response.headers())
                        .request(response.request())
                        .body(bodyData)
                        .build();
                CACHE.put(key, clonedResponse);
                // Return a new response with the same body to avoid stream issues
                return Response.builder()
                        .status(response.status())
                        .reason(response.reason())
                        .headers(response.headers())
                        .request(response.request())
                        .body(bodyData)
                        .build();
            } catch (IOException e) {
                logger.error("Failed to execute request for key: {}", key, e);
                throw e;
            } finally {
                // Close the original response body if it exists
                if (response != null && response.body() != null) {
                    try {
                        response.body().close();
                    } catch (IOException e) {
                        logger.warn("Failed to close response body for key: {}", key, e);
                    }
                }
            }
        }

        private String generateCacheKey(Request request) {
            // Include method to avoid collisions between GET/POST
            return request.httpMethod().name() + ":" + request.url();
        }

        private byte[] readBody(Response response) throws IOException {
            if (response.body() == null) {
                return new byte[0];
            }
            try (InputStream inputStream = response.body().asInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                logger.error("Failed to read response body", e);
                throw e;
            }
        }

        private Response cloneResponse(Response original) throws IOException {
            // For cached responses, the body is already a byte array
            byte[] bodyData = readBody(original);
            return Response.builder()
                    .status(original.status())
                    .reason(original.reason())
                    .headers(original.headers())
                    .request(original.request())
                    .body(bodyData)
                    .build();
        }
    }
}
```
And in addition to the upper code I need also this class annotated with @Configuration, like so:
```java
@Configuration
public class MyCustomFeignCachingConfig {

    @Bean
    public Capability myCustomCachingCapability() {
        return new MyCustomCachingCapability();
    }
}
```
And finally this class MyCustomFeignCachingConfig must be added to the FeignClient and its array of
configuration classes like so:
```java
@FeignClient(
        contextId = "daoClient",
        name = "backend-service", // Resolved dynamically via Eureka
        configuration = {CustomFeignClientConfiguration.class, MyCustomFeignCachingConfig.class}
)
public interface BackendFeignClient {
    //...
}
```
The question: Don't we overwrite all defaults in the whole Application by using @Configuration ??
The short answer is: No! Adding a @Configuration-annotated class to your FeignClient’s configuration attribute 
does not by itself stomp (step on) on every other bean in your app.
Spring Cloud OpenFeign spins up a child ApplicationContext for each named client 
(via FeignClientsConfiguration), and then merges in whatever you list in configuration=…. Any @Bean methods there will 
override the defaults only for that client’s context cloud.spring.io
The danger comes if you leave your config class on the classpath under your 
main @ComponentScan (e.g. in the same package as your @SpringBootApplication) and/or annotate it 
with @Configuration. In that case Spring Boot will auto-pick it up in the primary context, 
making its beans global defaults for all Feign clients (and potentially other parts of your app).

How to avoid that:
Don’t annotate your Feign-specific config with @Configuration (Feign will still pick it up when you reference it in @FeignClient).
Or, if you need @Configuration, move it into a package outside of your main component scan (OR explicitly exclude it from @ComponentScan),
so it only gets registered in the Feign child context and never globally

Which packages are scanned automatically upon start of the application? The auto scan comes from the @SpringBootApplication.
If the calls annotated with @SpringBootApplication is located in package dating.FeignClientService, then only the classes and
packages inside that final package FeignClientService will be scanned. Everything located outside FeignClientService (like
dating.config; and dating.controllers; ) will not be included in the application default scan. For that reason I have explicitly
included them in the scan like so:

```java
@EnableFeignClients
@EnableDiscoveryClient
@ComponentScan(basePackages = {
		//"dating.FeignClientService", //no need to specify this package, if the @FeignClient interface is in the same package or a sub-package of FeignClientServiceApplication. The default scanning from @EnableFeignClients will pick it up.
		"dating.controllers" // wherever your controller lives
		,"dating.config"
})
@SpringBootApplication
public class FeignClientServiceApplication {
```

Just as a reminder, the spring docs says this: 
```text
FooConfiguration does not need to be annotated with @Configuration. 
However, if it is, then take care to exclude it from any @ComponentScan that would otherwise include 
this configuration as it will become the default source for all possible classes.
```
This does not work that way only for the public 'class CustomFeignClientConfiguration', which is not only included in the configuration
array of @FeignClient(configuration = {CustomFeignClientConfiguration.class,..) , BUT also has to be inside package which is scanned by
@ComponentScan. This situation contradicts with the explanations above, BUT I have not found explanation for it.

HOWEVER, this is still 100% valid for the rest configurations like MyCustomFeignCachingConfig and LoggingFeignClientConfig - both of them
are located in packages which is not reached by the auto component scan, but are manually included in the configuration array of
@FeignClient(configuration = {...} - AND THEY WORK! As already said - I am not implementing any @ComponentScan at all for them. 
I have put classes in package dating.configExcludedFromComponentScan; as a symbol that all configuration classes are not 
globally included by Spring as such during the component scan, which spring does of the default application package by default.

BUT as mentioned above, this does not work so for 'class CustomFeignClientConfiguration'. Why? - I DONT KNOW!!!

   Will It Work Without @Configuration? - ADVICE FROM GROK AI at X.
Yes, it will likely work in your specific case because of how Spring Cloud OpenFeign processes the 
configuration attribute in the @FeignClient annotation. Feign’s Configuration Mechanism:
The configuration attribute in @FeignClient (e.g., configuration = { LoggingFeignClientConfiguration.class })
allows you to specify classes that define Feign-related beans, such as Client, Encoder, Decoder, or Contract.
Spring Cloud OpenFeign does not strictly require the class listed in configuration to be annotated with
@Configuration. Instead, it scans the class for methods annotated with @Bean and registers those beans in 
a child application context specific to the Feign client (in your case, the daoClient contextId).
As long as the loggingFeignClient method is annotated with @Bean, Spring will detect and register the
LoggingFeignClient bean for the BackendFeignClient.

   Potential Risks of Omitting @Configuration
While it works, omitting @Configuration can lead to subtle issues or confusion in certain scenarios, especially in larger or more complex Spring applications. Here are the risks and considerations:
Spring’s Standard Behavior:
In a typical Spring application, classes that define @Bean methods are expected to be annotated with @Configuration. This annotation tells Spring to treat the class as a source of bean definitions and apply special handling, such as:
Proxying: Spring creates a CGLIB proxy for @Configuration classes to ensure that @Bean methods are only invoked once, guaranteeing singleton behavior for beans (unless explicitly scoped otherwise).

Bean Dependency Injection: @Configuration ensures proper dependency injection for @Bean methods, especially when they depend on other beans.

Without @Configuration, Spring treats the class as a plain @Bean-defining class (sometimes called a "lite" mode). This means:
No CGLIB proxy is created.

If you call @Bean methods directly within the class, you might accidentally create multiple instances of the bean instead of reusing the singleton instance from the Spring context.

In your case, since LoggingFeignClientConfiguration (or another configuration class) is only used by Feign’s child context and the @Bean method is not called directly, this risk is minimal. However, it’s a potential issue in other contexts.

Feign-Specific Child Context:
Feign creates a separate Spring application context for each @FeignClient (scoped to the contextId, e.g., daoClient). The LoggingFeignClientConfiguration class (or another configuration class) is processed in this child context, and Feign’s configuration loader is more lenient than Spring’s standard bean definition scanner.

However, if another developer or part of the application tries to reuse LoggingFeignClientConfiguration (or another configuration class) outside the Feign context (e.g., by importing it with @Import or scanning it), the lack of @Configuration could cause issues, as Spring’s component scanning might not pick it up correctly unless explicitly included.

Code Clarity and Maintainability:
Omitting @Configuration makes the code less explicit about its purpose. Developers familiar with Spring expect @Configuration on classes that define @Bean methods, as it signals that the class is a source of Spring-managed beans.

Without @Configuration, it might be unclear whether the class is intentionally a plain Java class or if the annotation was forgotten, leading to confusion during maintenance or code reviews.

Future Compatibility:
While Spring Cloud OpenFeign currently supports plain classes in the configuration attribute, future versions or different Spring modules might enforce stricter conventions. Using @Configuration aligns with Spring’s best practices and reduces the risk of compatibility issues.

Component Scanning:
If LoggingFeignClientConfiguration (or another configuration class) is in a package that is scanned by Spring’s @ComponentScan, Spring will not automatically pick it up as a configuration class without @Configuration (or another stereotype annotation like @Component). 
In your case, since Feign explicitly references the class in @FeignClient (configuration = {CustomFeignClientConfiguration.class}), this isn’t an issue, but it’s a consideration for broader application contexts.



BUT ACCORDING TO Chat-GPT it is better to annotate such class with @Configuration. 
I STILL DO NOT DO THAT, but here is the long explanation provided by the GPT, why @Configuration is recommended:
First, it helps to clarify how Spring Cloud OpenFeign actually interprets the configuration classes you specify in:
```java
@FeignClient(
    name = "someClient",
    configuration = { /* one or more classes */ }
)
```
Below is why your existing CustomFeignClientConfiguration.class (in that particular case) might “just work” even without @Configuration, while adding MyCustomCachingCapability.class as a plain class does not.
(NB!! Although the AI says that it will work without annotating it with @Configuration - it does not really work! But still, here is the long explanation, why it is expected to work )
1. The “Reflection-Based” Bean Override Mechanism
   When you specify one or more classes in the configuration attribute of @FeignClient, Spring Cloud OpenFeign does two things:

Creates a child Spring context for that Feign client.

Inspects each configuration class to see if it provides overrides for known Feign beans – for example:

Decoder feignDecoder()

Encoder feignEncoder()

Logger feignLogger()

Capability someCapability()

etc.

In many versions of Spring Cloud OpenFeign, there is special reflection logic that looks for public methods in the supplied configuration class with a signature that matches certain known Feign bean types. 
If a method’s signature matches one of the expected beans, Feign picks it up. This means you don’t strictly need @Configuration + @Bean if (a) your class name ends with “Configuration” or is otherwise recognized, 
and (b) your method names/return types match exactly what Feign is looking for.

Why Your CustomFeignClientConfiguration Works (Without @Configuration)
Chances are your CustomFeignClientConfiguration has something like:
```java
public class CustomFeignClientConfiguration {

    public Decoder feignDecoder() {
        // returns a custom Decoder
    }

    public void testDateFormat() {
        // ...
    }

    // maybe some other methods...
}

```
... yes, I really have this Decoder feignDecoder there!
The key is that public Decoder feignDecoder() is recognized by Feign’s internal reflection as a bean override for the Decoder type. Feign sees:

“Oh, you have a method named feignDecoder() returning Decoder? Great, I’ll treat that as an override for the default Feign decoder bean.”

Because of that, Feign successfully uses your custom decoder, even though you never annotated the class with @Configuration or the method with @Bean. It’s basically a “shortcut” that Spring Cloud OpenFeign supports.

2. Why a Plain MyCustomCachingCapability.class Does Not Work
   By contrast, if your MyCustomCachingCapability is:
```java
public class MyCustomCachingCapability implements Capability {

    @Override
    public Client enrich(Client client) {
        // custom logic
        return new CachingClient(client);
    }

    // ...
}

```
This class:

Does not declare any method that returns Capability with a “Feigny” method name like feignCapability() (or something Feign is scanning for).

Is not annotated with @Configuration, so it does not expose a @Bean.

Therefore, the child context has no reason to instantiate it. It’s just a class sitting there; Feign sees no recognized method pattern such as public Capability myCustomCapability(); nor does Spring see @Bean or @Component.

Hence, you end up with no actual Capability bean in the child context.

If you name a public method public Capability feignCapability() (or something else Feign scans for), Spring Cloud OpenFeign might pick it up the same way it does for feignDecoder(). This depends on how your particular Spring Cloud version enumerates potential beans.

3. Two Reliable Ways to Ensure Your Capability is Registered
   A. Annotate with @Configuration + @Bean
   The standard (and recommended) Spring approach is:
```java
@Configuration
public class MyCustomFeignCachingConfig {

    @Bean
    public Capability myCustomCachingCapability() {
        return new MyCustomCachingCapability();
    }
}

```
Then reference that config:

```java
@FeignClient(
    name = "daoClient",
    configuration = { CustomFeignClientConfiguration.class, MyCustomFeignCachingConfig.class }
)
public interface BackendFeignClient { ... }

```
Result:

Now the child context knows it should instantiate a bean of type Capability.

Your MyCustomCachingCapability gets injected as a bean.

It overrides or supplements the default Feign capabilities.

B. Use the Reflection Trick With a Matching Method Signature
If you really want to avoid @Configuration and @Bean, you can rely on the reflection approach by:
```java
public class MyCustomCachingCapability {

    // This is the "magic" method that Feign might look for:
    public Capability feignCapability() {
        return new Capability() {
            // implement your caching logic in here
        };
    }
}

```
Then pass MyCustomCachingCapability.class in @FeignClient(configuration=...). Feign might pick that method as an override. This is more fragile because:

The naming conventions or reflection approach might differ between versions of Spring Cloud.

It’s not the usual “Spring” way of registering beans.

In short: it works in some scenarios if you match the method name & signature that Feign’s reflection logic expects, but it’s not as clean or future-proof.

4. It Won’t “Leak” to Other Feign Clients
   A common worry is:

“Will putting @Configuration on my class make it the default source for all Feign clients?”

No. In fact, specifying that configuration class in:
```java
@FeignClient(configuration = MyCustomFeignCachingConfig.class)

```
scopes it to just that client’s child context unless you also do something else to import it 
globally (e.g., @Import(MyCustomFeignCachingConfig.class) on your main app config). 
Spring Cloud creates a child context specifically for this @FeignClient (or set of clients using 
the same config), so you’re safe.


5. Bottom Line
   Your CustomFeignClientConfiguration “just works” because it has method(s) like public Decoder feignDecoder() that Feign picks up via its reflection-based approach.

A plain MyCustomCachingCapability.class does nothing unless you either:
Mark it as a bean via @Configuration + @Bean, or
Provide a recognized method signature (e.g., public Capability feignCapability()) that Spring Cloud’s reflection logic can pick up.
Most developers go the officially recommended route:
Create a @Configuration class
Return your custom bean with an @Bean method
Reference that config class in your @FeignClient(configuration=...)
That ensures your overrides are recognized reliably and are scoped just to that Feign client.

NB!! As mentioned above, I decided not to annotate the both class MyCustomFeignCachingConfig with @Configuration.

Now when testing the client:
First, start the App, to be sure that the cache is empty.
Second, send a first request from the Postman client.
Third, watch the console logs - it should print:
```text
Cache MISS for key: GET:http://backend-service/api/v1/user/8
```
, because that URL is still not cached in the memory. The feign client will have to execute the Request
till the end and to contact the backend service and to get the object. Watch die mileseconds performance in the Postman - its always around 200ms
Fourth, send a second request from the Postman client.
Fifth: watch the console logs - it should print:
```text
Cache HIT for key: GET:http://backend-service/api/v1/user/8
```
, because the cache contained this URL and the object from it was returned from the cache memory and
not from the original backend service.

Conclusion: Caching in Feign client works!!!

NB!!!!
I want to mention that I am not using anywhere in my project the @EnableCaching annotation.
What Does @EnableCaching Do?:
The @EnableCaching annotation enables Spring’s caching infrastructure, allowing you to use annotations like:
@Cacheable: Cache the result of a method call.
@CachePut: Update the cache with a method’s result.
@CacheEvict: Remove items from the cache.

It activates a CacheManager (e.g., ConcurrentMapCacheManager, Caffeine, EhCache, Redis) to manage caches, which you configure via Spring beans or properties.

It’s typically used for caching at the service or repository layer, not at the HTTP client level like your Feign implementation.

Do You Need @EnableCaching for Feign Caching?:
No, if you’re satisfied with your custom ConcurrentHashMap-based caching:
Your CachingClient handles caching directly, without relying on Spring’s cache abstractions.

You have full control over cache key generation, storage, and response cloning, tailored to Feign’s Request/Response model.

Adding @EnableCaching won’t affect your current Feign caching unless you refactor it to use Spring’s CacheManager (e.g., by integrating a Cache instance into CachingClient).

Yes, if you want to:
Standardize Caching Across the Application:
If other parts of your application (e.g., service or repository layers) need caching, @EnableCaching enables Spring’s caching framework for those components.

For example, you might want to cache database query results in a service method using @Cacheable.

Replace or Enhance Feign Caching:
You could refactor CachingClient to use a Spring Cache (e.g., backed by Caffeine or Redis) instead of ConcurrentHashMap. This would leverage Spring’s cache eviction policies, TTLs, or distributed caching.

Example:
```java

@Autowired
private CacheManager cacheManager;

private Response execute(Request request, Request.Options options) throws IOException {
String key = generateCacheKey(request);
Cache cache = cacheManager.getCache("feignResponses");
Cache.ValueWrapper cached = cache.get(key);
if (cached != null) {
logger.info("Cache HIT for key: {}", key);
return (Response) cached.get();
}
// ... execute and cache
}
```
This requires @EnableCaching and a configured CacheManager.

Use Advanced Cache Features:
Spring’s caching supports TTLs, eviction policies, and distributed caches (e.g., Redis), which ConcurrentHashMap lacks. If your Feign caching needs these (e.g., to prevent memory growth), integrating with Spring’s cache might be beneficial.

When to Avoid @EnableCaching:
If your application only needs Feign caching and you’re happy with the custom ConcurrentHashMap approach, @EnableCaching adds unnecessary overhead (e.g., initializing a CacheManager).

If you don’t plan to use @Cacheable or other Spring caching annotations elsewhere, skip it to keep your configuration lean.

Potential Considerations:
Memory Management:
Your current ConcurrentHashMap cache grows indefinitely. Without eviction, it could consume significant memory for large or frequent requests.

@EnableCaching with a CacheManager like Caffeine could add eviction policies (e.g., size-based or time-based), but you’d need to refactor CachingClient.

Consistency:
If you add @EnableCaching for other application components, consider aligning Feign caching with Spring’s cache for consistency, but this is optional.

Performance:
Your custom caching is lightweight and specific to Feign. Spring’s caching adds abstraction layers, which might introduce minimal overhead, but it’s negligible for most use cases.

NB! One Big Question remains - what will happen if the object is updated in the Database, 
but the cache still returns the old object? This is a problem!



                  END OF experiment with cachingCapability





                  START OF experiment to configure class Client feignClient


First, I read in the official Spring docs: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults
that there is this class Client feignClient: If Spring Cloud LoadBalancer is on the classpath, FeignBlockingLoadBalancerClient is used. If none of them is on the classpath, the default feign client is used.
, which can be configured manually by me.
Second, I notice that in my custom configuration of the cachingCapability (see above), I already do custom configuration
of the Client by adding a new capability to it.
Now, I ask myself, how will the existing configuration relate or affect the new configuration I intend to experiment with?

Here is explanation from the GPT:
① Background: Understanding feign.Client
The feign.Client interface is central to how OpenFeign works—it abstracts the actual HTTP client implementation used by Feign. By default, Feign delegates HTTP calls to one of several possible implementations:

Default Feign Client (feign.Client.Default):

Uses Java’s built-in HttpURLConnection.

Basic, no-frills implementation.

Spring Cloud OpenFeign’s Client:

If Spring Cloud LoadBalancer is on the classpath, it uses FeignBlockingLoadBalancerClient.

Provides integration with Spring Cloud's service discovery and load-balancing mechanisms (such as Eureka or Consul).

② Functionality supported by feign.Client
The primary responsibilities of the Client are:

Execute HTTP requests constructed by Feign interfaces.

Handle network calls (sending requests and receiving responses).

Facilitate integration points for load balancing, caching, metrics, logging, tracing, or custom logic.

In essence, by overriding the Client, you gain access to the raw HTTP request/response lifecycle.

③ Your current customization (MyCustomCachingCapability)
Your existing setup uses a Spring Cloud OpenFeign-specific interface called Capability:
```java
public class MyCustomCachingCapability implements Capability {

    @Override
    public Client enrich(Client client) {
        return new CachingClient(client);
    }

    private static class CachingClient implements Client {
        private final Client delegate;

        public Response execute(Request request, Request.Options options) throws IOException {
            // caching logic...
        }
    }
}

```

What you've already modified:
Your CachingClient already wraps the original Client provided by Spring (typically a FeignBlockingLoadBalancerClient or feign.Client.Default).
This wrapping introduces caching behavior—checking requests against a cache before executing them and storing responses after execution.
By wrapping, you intercept requests before they're executed, adding your custom logic transparently.
Your current invocation chain looks like this:
```text
Feign → Capability#enrich(Client) → CachingClient → Original Feign Client
```

④ How would adding a new custom Client (like LoggingFeignClient) interact with your existing setup?
Consider the new suggestion (LoggingFeignClient):
```java
public class LoggingFeignClient implements Client {
    private final Client delegate;

    public Response execute(Request request, Request.Options options) throws IOException {
        // log request
        Response response = delegate.execute(request, options);
        // log response
        return response;
    }
}

```
If you configure this directly in your Feign configuration class (like CustomFeignClientConfiguration):

```java
//@Configuration - you don't need this here, because it's imported locally via: @FeignClient(configuration = {...,LoggingFeignClientConfiguration.class,...}
public class LoggingFeignClientConfiguration {
    @Bean
    public Client loggingFeignClient(Client defaultFeignClient) {
        return new LoggingFeignClient(defaultFeignClient);
    }
}

```
The question: Don't we overwrite all defaults in the whole Application by using @Configuration ??
The short answer is: No! Adding a @Configuration-annotated class to your FeignClient’s configuration attribute
does not by itself stomp (step on) on every other bean in your app. You can read the longer explanation above in the
explanation for the MyCustomCachingCapability.java.

How to avoid that:
Don’t annotate your Feign-specific config with @Configuration (Feign will still pick it up when you reference it in @FeignClient).
Or, if you need @Configuration, move it into a package outside of your main component scan (or explicitly exclude it),
so it only gets registered in the Feign child context and never globally

Just as a reminder, the spring docs says this:

Now, the question is: "What happens when I have both the Capability enrichment (CachingClient) and explicit Client configuration (LoggingFeignClient)"?
Here’s what would happen:
First, Feign creates your loggingFeignClient bean explicitly because you defined it as a @Bean. It becomes the default client.
Next, your Capability class (MyCustomCachingCapability) is invoked by Spring Cloud OpenFeign to wrap whatever final Client is chosen (your LoggingFeignClient in this case).
Thus, your final invocation chain would become:
```text
Feign → Capability#enrich(Client) → CachingClient → LoggingFeignClient → Original Feign Client

```
This chain means:

Requests pass first through your caching logic.
If no cached response is found, the request proceeds into your logging logic.
After logging, the actual HTTP request happens via the original client.
Finally, the response traverses back through logging and caching layers.

⑤ Will there be contradictions or issues?
No fundamental contradictions, since both wrappers simply delegate calls down the chain. This kind of layering is precisely the intention of the Feign/Spring Cloud design.
However, there are things to consider carefully:
Order of wrapping matters:
Currently, the capability wrapping (CachingClient) always occurs after the explicit bean is configured. Thus, caching occurs before logging. If you intended logging to always happen before caching, you'd need to implement logging as a Capability instead.
Performance & complexity:
Each layer introduces minor overhead.
Logging typically goes close to the HTTP layer. Caching often makes sense to be higher up (as you have now), since you don't log requests that hit the cache.
Response Body Handling:
Your caching logic carefully handles cloning response bodies. If the logging client reads the response body (for logging purposes), you must ensure it also does careful cloning; otherwise, the response body stream could be exhausted. Your example logging client does not read the body (just logs the status), which is safe.

⑥ Recommended Approach
If you simply want to experiment safely without issues, the suggested logging client (which doesn't touch the body) is safe and works fine.
But, if your logging requires accessing the body, ensure careful cloning (as you've done with caching).
Consider clarity: perhaps turn your logging into a Capability as well. This gives you explicit control over wrapping order.

✅ Conclusion / Recommendation
Yes, you have already significantly modified the Feign Client by introducing caching via your Capability.
Adding another wrapper (logging or tracing) is perfectly valid and will seamlessly integrate as another layer—just be mindful of the invocation order and response-body handling.
Thus, your experiment is well-structured and valuable: it clearly shows the composability and flexibility of Spring Cloud OpenFeign’s configuration.

NB! Despite that the Client class has many other methods, which potentially could be 
customized  each class still exposes only the execute method outwardly, but layers its 
concern (proxy, retry, tracing, etc.) around a delegate client. Your caching and logging 
layers are doing exactly the same thing.

A very long list of practical 'exotic' implementations can be found here: resources/explore_ideas/customize_classClient.odt




                  END OF experiment to configure class Client feignClient




                  START OF experiment to customize Feign.Builder

So far I already implemented custom configurations on the @FeignClient like:

```java
@FeignClient(
contextId = "daoClient",
name = "backend-service", // Resolved dynamically via Eureka
configuration = {CustomFeignClientConfiguration.class,
MyCustomFeignCachingConfig.class,
LoggingFeignClientConfiguration.class}
)
public interface BackendFeignClient {...}
```

Next, I decide to learn how to customize the Feign.Builder feignBuilder: FeignCircuitBreaker.Builder
and I see that a very basic Feign.Builder feignBuilder would look like this:

```java
@Configuration
class VanillaFeignBuilderConfig {

    @Bean                    // will be picked up automatically
    @Scope("prototype")      // must be prototype for OpenFeign
    Feign.Builder feignBuilder(ObjectFactory<HttpMessageConverters> converters) {

        var encoder   = new SpringEncoder(converters);
        var decoder   = new SpringDecoder(converters);
        var retryer   = new Retryer.Default(100, TimeUnit.SECONDS.toMillis(2), 3);

        return Feign.builder()
                    .encoder(encoder)
                    .decoder(decoder)
                    .retryer(retryer)
                    .logger(new Slf4jLogger("FEIGN"))
                    .logLevel(Logger.Level.BASIC)
                    .options(new Request.Options(2_000, 5_000))   // connect / read
                    .requestInterceptor(t -> t.header("X‑Caller", "demo‑builder"))
                    .dismiss404()                                 // don’t treat 404 as error
                    .errorDecoder(new CustomErrorDecoder());
    }
}

```
, which makes me think that if I implement such customization in the existing context, such implementation
hides risks of collisions. Why? Because the builder apparently takes classes, which I also manually configure.
For that reason I decide to implement a very basic builder, which practically does not modify or customize
anything, but potentially can do so. This is the explanation:

1. What is the “vanilla” Feign.Builder?
Definition in the Spring Cloud context “vanilla builder” simply means the naked Feign.builder() – no circuit‑breaker, no retry policy, 
no exotic client, nothing except what you explicitly add.

Why it exists Spring Cloud ships the more opinionated FeignCircuitBreaker.Builder 
(and wires it as the default bean named feignBuilder) so that every client automatically gets a 
Resilience4j/Sentinel circuit breaker.
Sometimes you do not want that (e.g. you already have resilience upstream, you are calling a localhost mock, 
you need very fine‑grained control, or you need to test time‑out behaviour). For those cases the docs recommend 
registering your own prototype‑scoped Feign.Builder bean, which replaces the default just for the clients that 
import that configuration.

How it is picked up when FeignClientFactoryBean creates the proxy it:
Looks up a single Feign.Builder bean in the client‑specific child ApplicationContext.

Applies every Capability, Encoder, Decoder, Request.Options, RequestInterceptor, … it finds in the same 
context — these are layered on top of the builder you supplied.
Because of that order your builder only replaces what you touch and inherits the rest.

So a “vanilla” builder is nothing more than:

```java
@Bean                 // in a config class *referenced by the client*
@Scope("prototype")   // <- mandatory
public Feign.Builder feignBuilder() {
    return Feign.builder();          // start plain
}

```
I implement that bean in (any) the general configuration class like:@Configuration public class ApplicationBeanConfiguration

Key takeaway: the builder factory sits at the top of the assembly line; any beans you already have for encoding, 
caching, logging, etc. are bolted on after the builder is obtained, unless you actively override them inside the 
builder.

WHY is it mandatory to have scope 'prototype': scope @Scope("prototype")  ?
Feign.Builder is stateful: every time Spring Cloud creates a client proxy it
pulls a builder bean out of the context, adds that client’s encoders/decoders/interceptors/capabilities,
finally calls builder.target(…) (or, with the CB wrapper, builder.build()) to create the proxy.
After step 3 the builder keeps all the mutations that were just applied.
If the same Java object were reused for the next @FeignClient, that second client would inherit every setting 
of the first one and both threads would be mutating the same builder instance at startup. In practice you would 
see:
cross‑talk between clients (wrong Request.Options, wrong interceptors, duplicated capabilities),
races at application start‑up, and bizarre “already built” errors when two threads call target() on the same 
builder simultaneously.
To prevent that, the default Spring Cloud configuration publishes the builder bean with @Scope("prototype").


Bottom line
The builder is the entry‑point, not the entire configuration.
By supplying your own prototype‑scoped Feign.Builder you change only what you explicitly set in that builder; 
all other pieces—decoders, encoders, capabilities, interceptors, even your logging and caching layers—are still
wired in by Spring Cloud after the builder is retrieved. The one automatic change is that a plain builder 
switches the circuit‑breaker (if such implemented by you) off; use FeignCircuitBreaker.builder(..) if you want to keep it.

Therefore, your existing modular configuration classes will continue to do exactly what they do now, provided you
avoid duplicating beans of the same type or overriding them unintentionally inside the builder.

And this works fine! This builder actually does not build or add anything new!
If you only want to tweak one knob (say, connect/read time‑outs) and leave every other component intact:
```java
@Configuration
class VanillaFeignBuilderConfig {

    @Bean                 // put this class into your @FeignClient configuration list
    @Scope("prototype")
    Feign.Builder feignBuilder(Request.Options customTimeouts) {
        return Feign.builder()
                    .options(customTimeouts);   // touch ONE property
        // everything else (encoder, decoder, capabilities, etc.)
        // will be wired by the framework afterwards
    }

    /** choose one default Request.Options and mark it @Primary or inject it here */
    @Bean @Primary
    Request.Options customTimeouts() {
        return new Request.Options(3_000, 5_000);
    }
}
```
, however, I am not implementing such code.


                  END OF experiment to customize Feign.Builder











                  START OF experiment to customize the Contract feignContract: SpringMvcContract






Before we do any customization, lets first explain what does the Contract do by default.
A Feign Contract is the piece that “reads” your Java interface and its annotations and turns each 
method into a description (a MethodMetadata) of how to make the HTTP request. By swapping it out or 
extending it you can change how annotations are interpreted—but before we dive into that, here’s 
what it does by default (with the Spring-provided SpringMvcContract):

Here EXAMPLE which is not implemented in the project:
1. Scanning your interface
   Given an interface like:
```java
@FeignClient(name = "users")
public interface UserClient {

  @GetMapping("/api/users/{id}")
  UserDto getUser(
      @PathVariable("id") Long id,
      @RequestParam(value = "includeDetails", required = false) boolean includeDetails);

  @PostMapping("/api/users")
  @Headers("X-Auth-Token: {token}")
  void createUser(@RequestHeader("token") String token, @RequestBody CreateUserCommand cmd);
}
```
the Contract will:
Discover each method (getUser, createUser).
For each method, inspect all annotations on the method itself, on its parameters, and on its return 
type.

2. Building the MethodMetadata
   For getUser(...), by default SpringMvcContract will:

See @GetMapping("/api/users/{id}")
HTTP method = GET
Path template = /api/users/{id}
See one @PathVariable("id") → bind parameter 0 into the {id} slot.
See one @RequestParam("includeDetails") → bind parameter 1 into query string ?includeDetails={includeDetails}.

Produce a MethodMetadata roughly equivalent to:

```json
{
  name: "getUser",
  httpMethod: HttpMethod.GET,
  path: "/api/users/{id}",
  pathParams: { 0 -> "id" },
  queryParams: { 1 -> "includeDetails" },
  headers: {},
  bodyIndex: none
}
```
When you actually call userClient.getUser(42L, true), Feign takes that metadata, substitutes id=42 and includeDetails=true, 
builds a RequestTemplate, and invokes the HTTP call.

When you actually call userClient.getUser(42L, true), Feign takes that metadata, substitutes id=42 and includeDetails=true,
builds a RequestTemplate (it's a java object - Builds a request to an http target. Not thread safe.; 
Similar to RestTemplate or WebClient), and invokes the HTTP call.

3. Handling bodies, headers, etc.
   For createUser(...), by default it will:
See @PostMapping("/api/users") → HTTP method = POST, path = /api/users.
See @Headers("X-Auth-Token: {token}") → add a static header template.
See @RequestHeader("token") on the first parameter → bind parameter 0 into the header template.
See @RequestBody on the second parameter → mark parameter 1 as the request body.
Produce metadata like:

```json
{
  name: "createUser",
  httpMethod: HttpMethod.POST,
  path: "/api/users",
  pathParams: {},
  queryParams: {},
  headers: {
    "X-Auth-Token": "{token}"
  },
  headerParams: { 0 -> "token" },
  bodyIndex: 1
}
```
At runtime Feign replaces {token} with the actual header value and serializes your CreateUserCommand
into JSON.

4. Why a Contract?
   Abstraction: Decouples how Feign parses your interface from the transport layer.
Extensibility: You can plug in a totally different Contract (e.g. JAX-RS annotations) or subclass 
SpringMvcContract to add support for your own annotations (e.g. @MyApiVersion).
Consistency: Ensures all clients interpret annotations the same way, no matter which underlying HTTP
client you use.

Which annotations does the Contract concern and process?

Feign’s Contract only looks at your Feign client interface (the one annotated with @FeignClient) and
the Spring MVC (or JAX-RS, etc.) annotations you put on that interface. It does not scan or process 
any of your server-side controllers.
```java
@FeignClient(name="backend-service", …)
public interface BackendFeignClient {

  @GetMapping("/api/v1/user/{id}")
  ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id);
}

```
Feign’s SpringMvcContract will see only that @GetMapping and @PathVariable on the client interface. 
It builds its MethodMetadata from those.

The server-side controller:
```java
@RestController
@RequestMapping("/api/v2")
public class ProxyDatabaseRestController {

  @GetMapping("/proxy-user/{id}")
  public ResponseEntity<UserDTO> proxyGetUser(@PathVariable Long id) {
    return feignClient.getUserById(id);
  }
}

```
is entirely separate: it’s the Spring MVC machinery that handles incoming HTTP on port 8082. Feign 
never inspects these annotations when it’s building its client.

In short:
Feign Contract → only your @FeignClient interface and its method/parameter annotations.
Spring MVC → your @RestController classes and their mappings.
They live in the same process here, but the Contract is purely for client-side metadata.

On the server side such mapping is done by the Spring MVC’s DispatcherServlet (your “front controller”) 
that:
Catches every incoming HTTP request (e.g. on port 8082).
Consults its HandlerMapping beans—by default a RequestMappingHandlerMapping—which were set up at 
startup by scanning all your @Controller/@RestController classes and their @RequestMapping 
(or shortcut) annotations.
Picks the best match (e.g. /api/v2/proxy-user/{id} → ProxyDatabaseRestController.proxyGetUser).
Hands control to a HandlerAdapter—by default RequestMappingHandlerAdapter—which:
Resolves method arguments (path vars, request bodies, headers, etc.)
Invokes your controller method
Uses HttpMessageConverters to serialize the return value (ResponseEntity<UserDTO>) back to the HTTP 
response.

So, in your client service process:
Feign side: Feign’s contract reads only the annotations on your @FeignClient interface to know how to send requests.
Server side: Spring MVC’s DispatcherServlet + HandlerMapping + HandlerAdapter read only the annotations on your @RestController to know how to receive and handle them.

They’re two independent annotation‐processing pipelines—one for client-side stubs, one for server-side controllers.

Now, after knowing what Contract is and does, lets try to customize it:
First, I was thinking I could solve a generic problem feign has - Feign does not send @PATCH requests,
as described in the first sentences of this README.md file (see the start).
Feign uses by default the HttpURLConnection, which does not support PATCH request. By swapping in ApacheHttpClient or OkHttpClient
that problem could be sovled, BUT not by modifying Feign Contract !!!

Why this isn’t a “Contract” problem
The Feign Contract (e.g. SpringMvcContract) is purely about reading your annotations (@GetMapping, 
@PatchMapping etc.) and building the metadata (HTTP method, path, headers, body‐param indices).
But once it knows “PATCH” is the method, it hands that off to your chosen Client implementation. The 
default Client.Default uses HttpURLConnection, which refuses PATCH.

So, lets think of another customization to be applied on Contract!!!?

Now, once that we know what the Contract does, we can customize it.
We have many different methods, which we can @Override with custom logic, but for our demo we need 
to @Override only few, like these:
```java
// called once per class (and once per interface if you use inheritance)
protected abstract void processAnnotationOnClass(MethodMetadata data, Class<?> targetType);

// called for each annotation on each method
protected abstract void processAnnotationOnMethod(MethodMetadata data,
                                                 Annotation methodAnnotation,
                                                 Method method);

// called for each parameter
protected abstract boolean processAnnotationsOnParameter(MethodMetadata data,
                                                         Annotation[] annotations,
                                                         int paramIndex);
```

The TASK will be - get access to the URL Path of @GetMapping, @PostMapping or of them all and
change the outgoing URL Path as you like; For example my BackendFeignClient supports such Mapping:

```java
public interface BackendFeignClient {
   (... other controllers here)
   @GetMapping("/api/v1/user/{id}");
   ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id);
```
, But I could @Override it to map to another URL, like so:
```java
public interface BackendFeignClient {
    (... other controllers here)
   @ApiVersion("api/v1") 
   @GetMapping("/user/{id}") 
   ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id);
}
```
, in other words I can devide the parts of the current URL Path, and based on my conditions, code
logic or simple desires, I could append OR prepend new paths to the existing, e.g.
@ApiVersion("api/v1") + @GetMapping("/user/{id}") WILL PRODUCE: 'api/v1/user/{id}'
OR if I decide to prepend, it WILL PRODUCE: /user/{id}api/v1
OR I could modify something completely different, like the add a custom HEADER!
Here is how I achieve this custom configuration:

1. How to override the default Contract
   Anything you declare as a @Bean Contract in a client‐scoped configuration will replace the default. You must:

Create a configuration class that is not picked up by your main @ComponentScan
Declare a prototype‐scoped Contract bean in it
Reference that config in your @FeignClient(configuration=…)
First, create class:
```java
package dating.configExcludedFromComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    /**
     * e.g. "v1", "v2", etc.
     */
    String value();
}
```

Next, create:

```java
import feign.Contract;
import feign.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Custom Contract that applies @ApiVersion at class and method level
 * by rewriting only the path (not the full target URL) and adding a header.
 */
public class ApiVersionContract extends SpringMvcContract {

    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> targetType) {
        // 1) let SpringMvcContract handle @RequestMapping at class level
        super.processAnnotationOnClass(data, targetType);

        // 2) then apply our @ApiVersion if present
        ApiVersion apiVer = targetType.getAnnotation(ApiVersion.class);
        if (apiVer != null) {
            String versionPrefix = "/" + apiVer.value();
            // set base path
            data.template().uri(versionPrefix);
            data.template().header("X-API-Version", apiVer.value());
        }
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data,
                                             Annotation ann,
                                             Method method) {
        super.processAnnotationOnMethod(data, ann, method);

        ApiVersion ver = method.getAnnotation(ApiVersion.class);
        if (ver != null) {
            String version = ver.value().startsWith("/")
                    ? ver.value()
                    : "/" + ver.value();
            String existing = data.template().url();  // e.g. "/user/{id}"
            // only prefix if it isn’t already there
            if (!existing.startsWith(version)) {
                data.template().uri(version + existing, false);
                data.template().header("X-API-Version", ver.value());
            }
        }
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data,
                                                    Annotation[] annotations,
                                                    int paramIndex) {
        // no changes to parameter handling
        return super.processAnnotationsOnParameter(data, annotations, paramIndex);
    }
}
```

Next create:

```java
@Configuration
public class ApiVersionFeignConfig {
    @Bean
    @Scope("prototype")
    public Contract feignContract() {
        return new ApiVersionContract();
    }
}
```

Then add the configuration manually to the configuration array of the feign client:
```java
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
public interface BackendFeignClient {...}
```

Finally, the controllers of the feign client would look so:

```java
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
    @PostMapping("create-new-user")  // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<Object> createNewUser(@RequestBody UserDbDTO userDbDTO);

    //@GetMapping("/api/v1/user/{id}")  //<-- Use this when @ApiVersion("/api/v1") is not applied at all!
    //@ApiVersion("api/v1") //<-- dont apply it here on method level, if you have applied it on class level.
    @GetMapping("user/{id}") // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id);

//    @GetMapping("/api/v1/user/{id}")
//    UserDTO getUserById(@PathVariable("id") Long id);

    //@GetMapping("/api/v1/user-with-data/{id}")   //<-- Use this when @ApiVersion("/api/v1") is not applied at all!
    //@ApiVersion("api/v1") //<-- dont apply it here on method level, if you have applied it on class level.
    @GetMapping("user-with-data/{id}") // <-- use it in combination with @ApiVersion("/api/v1") - either on class level or method level.
    ResponseEntity<UserDbDTO> getUserWithDataById(@PathVariable("id") Long id);

}
```

Conclusion - on all places we apply the  @ApiVersion("/api/v1") would override the PATH and 
will add a new HEADER.

Now send a request: http://localhost:8082/api/v2/proxy-user/8 via Postman or curl and you will
get the usual response:
```json
{
    "id": 8,
    "ts": "Arrr, it be the 1 day of January in the year 0001 at the hour of 12:00 am o'clock!"
}
```
On the backend service side, to confirm that the request really contains the new header, you can
add this:
ni backend-service project:
```java
@GetMapping("/user/{id}")
    public ResponseEntity<EntityModel<User>> getUserById(@PathVariable Long id,
                                                         @RequestHeader(value = "X-API-Version", required = false) String apiVersion) throws JsonProcessingException {

        System.out.println(">>>> Received X-API-Version: " + apiVersion);
   ...
```
And it worked!
NB!! When sending POST requests via Postman, be careful with the DATE serilization, because currently 
I am using 'funny' date format to use when SERIALIZING:
```java
new SimpleDateFormat("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInDate:noMinsInDate o''clock!'");
```
and it cannot be converted back when sending POST with normal date:  "regDate": "2023-12-25T10:00:00", 

Before we end this DEMO for Contract, let's customize the Contract a bit more to show off other things we can
dynamically change.  To have your Contract automatically append an API key (or any static credential) to every 
request — so you never have to remember to add it in each method. Here’s how to do it:
Declare your secret key in application.yml:
```yml
# application.yml
feign:
  api:
    key: 12345-SECRET-KEY
```

Inject the Api Key in the configuration for Contract like so:
```java
@Configuration
public class ApiVersionFeignConfig {

    @Value("${feign.api.key}")  // The Secret API Key is taken from the application.yml.
    private String apiKey;

    @Bean
    @Scope("prototype")
    public Contract feignContract() {
        return new ApiVersionContract(apiKey);
    }
}
```
, then update the constructor for public Contract feignContract() in public class ApiVersionContract, so:
```java
public class ApiVersionContract extends SpringMvcContract {

    private final String apiKeyValue;   // this is the newly added API KEY Value

    public ApiVersionContract(String apiKeyValue) {
        super();
        this.apiKeyValue = apiKeyValue;     // this is the newly added API KEY Value
    }


    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> targetType) {
        // 1) let SpringMvcContract handle @RequestMapping at class level
        super.processAnnotationOnClass(data, targetType);

        // 2) then apply our @ApiVersion if present
        ApiVersion apiVer = targetType.getAnnotation(ApiVersion.class);
        if (apiVer != null) {
            String versionPrefix = "/" + apiVer.value();
            // set base path
            data.template().uri(versionPrefix);
            data.template().header("X-API-Version", apiVer.value());
        }
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data,
                                             Annotation ann,
                                             Method method) {
        super.processAnnotationOnMethod(data, ann, method);

        ApiVersion ver = method.getAnnotation(ApiVersion.class);
        if (ver != null) {
            String version = ver.value().startsWith("/")
                    ? ver.value()
                    : "/" + ver.value();
            String existing = data.template().url();  // e.g. "/user/{id}"
            // only prefix if it isn’t already there
            if (!existing.startsWith(version)) {
                data.template().uri(version + existing, false);
                data.template().header("X-API-Version", ver.value());
            }
        }
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data,
                                                    Annotation[] annotations,
                                                    int paramIndex) {
        // no changes to parameter handling
        return super.processAnnotationsOnParameter(data, annotations, paramIndex);
    }



   // HERE WE @Override this method in order to add a secret API KEY Value
    @Override 
    public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
        // 1) let SpringMvcContract build all the normal metadata
        List<MethodMetadata> list = super.parseAndValidateMetadata(targetType);

        // 2) then for each method, add '?apiKey=...' to its template
        for (MethodMetadata md : list) {
            md.template().query("apiKey", apiKeyValue);
        }
        return list;
    }

}
```

This configuration file is already added to the configuration array of Feign client, so no need to add it again.
Now, when each request the feign client sends out to the backend service will have API Key secret added like so:
```bash
GET http://backend-service/api/v1/user/42?apiKey=12345-SECRET-KEY
```

And this is printed on the console as log:
```cmd
[BackendFeignClient#getUserById] ---> GET http://backend-service//api/v1/user/8?apiKey=12345-SECRET-KEY HTTP/1.1
[BackendFeignClient#getUserById] X-API-Version: /api/v1
[BackendFeignClient#getUserById] ---> END HTTP (0-byte body)
```
This pattern is equally powerful if you’d rather inject a header:
```java
// in parseAndValidateMetadata():
md.template().header("X-API-Key", apiKeyValue);
```
,so you can adapt it to whatever your backend expects.



CONCLUSION:

What we’ve achieved by customizing the Feign Contract

@ApiVersion support

Your client interface can now carry an @ApiVersion("…") on a class or method.
The Contract detects the annotation and applies your custom logic automatically.

Dynamic path rewriting
Instead of hard-coding version segments in every @GetMapping (or other mapping), the Contract prefixes the path template at build-time.
E.g. @ApiVersion("api/v1") + @GetMapping("/user/{id}") → final request to /api/v1/user/123.

Automatic header injection
Every request carrying @ApiVersion also gets an X-API-Version: <value> header added by the Contract.

Idempotent, Eureka-friendly behavior
A simple guard (if (!existing.startsWith(version)) …) ensures the version prefix is only applied once.
By only rewriting the path and never stomping on Feign’s original service‐ID host, you retain full Eureka/LoadBalancer resolution.

Why this matters—key benefits
DRY versioning: Declare the API version once, at the method level (or class), instead of repeating path fragments everywhere.
Per-endpoint flexibility: Mix v1, v2, etc. within a single client by annotating individual methods.

Header-based routing: Backends that switch behavior based on an X-API-Version header now get that header for free.
Clear documentation: Your interface code explicitly shows which version each call targets—no guesswork or hidden wiring.

Resilient and safe: The guard logic prevents accidental double-prefixing and keeps service discovery intact.
Extensible pattern: You’ve learned how to hook into SpringMvcContract’s lifecycle to support any custom annotation or mapping convention.




                     END OF experiment to customize the Contract feignContract: SpringMvcContract


                     









                     START OF experiment to enable X-Forwarded-Host and X-Forwarded-Proto support
X-Forwarded-Host and X-Forwarded-Proto support can be enabled by setting following flag:
```yml
... some other configs

spring:
   application:
      name: feign-client-service
   cloud:
      openfeign:
         client:
            config:
               default:
                  loggerLevel: FULL  # Enables FULL logging for all Feign clients
      loadbalancer:
         x-forwarded:
            enabled: true   # <---  HERE is the x-forwarded ENABLED!!!

...some other configs
```
, next to debug and actually see that these headers are now set, go to the backend service
instance, choose a method and add this argument: @RequestHeader Map<String,String> headers and
print the headers, like so:
```java

//... some code
@GetMapping("/user-with-data/{id}")
    public ResponseEntity<EntityModel<UserDbDTO>> getUserWithDataById(@PathVariable Long id,
                                                                      @RequestHeader Map<String,String> headers) throws JsonProcessingException {

        System.out.println(">>>>>>>> Printing Request Headers:");
        headers.forEach((key, value) -> System.out.println(key + ": " + value));
   //... some code

```

The printed result will be:
```cmd
>>>>>>>> Printing Request Headers:
x-forwarded-host: BlaBlaBla
x-forwarded-proto: http
x-api-version: /api/v1
accept: */*
user-agent: Java/17.0.7
host: BlaBlaBla:8079
connection: keep-alive
```


                     END OF experiment to enable X-Forwarded-Host and X-Forwarded-Proto support








                     




                     START OF experiment to customize the default LOAD BALANCER in Feign


The feign client does Load Balancing 'Round Robin' by default!
The applied load balancer is Spring Cloud LoadBalancer, coming directly from Spring.
This is how I change the algorithm from 'Round Robin' to 'Random':

First, create the new LB configuration so:
```java
package dating.configExcludedFromComponentScan;

import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class BackendLoadBalancerConfig {

    /** 1) switch algorithm to Random instead of RR */
    @Bean
    ReactorServiceInstanceLoadBalancer randomLB(
            Environment env, LoadBalancerClientFactory factory) {

        String name = env.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                factory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name);
    }

    /** 2) compose the *supplier* chain: discovery → weighting → zone → cache */
    @Bean
    ServiceInstanceListSupplier weightedSupplier(ConfigurableApplicationContext ctx) {
        return ServiceInstanceListSupplier.builder()
                .withBlockingDiscoveryClient()   // ← change this line
                .withWeighted()
                .withZonePreference()
                .withCaching()
                .build(ctx);
    }

}
```
, next declare this configuration in the Spring Boot Application class so with @LoadBalancerClient:

```java
/**
 * Isolated only for backend-service.  Put the class OUTSIDE @ComponentScan
 * or omit @Configuration to prevent it from becoming a global default.
 */
@LoadBalancerClient(
        name = "backend-service",
        configuration = BackendLoadBalancerConfig.class)
@EnableFeignClients
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        //"dating.FeignClientService", //no need to specify this package, if the @FeignClient interface is in the same package or a sub-package of FeignClientServiceApplication. The default scanning from @EnableFeignClients will pick it up.
        "dating.controllers" // wherever your controller lives
        ,"dating.config"
})
@SpringBootApplication
public class FeignClientServiceApplication {

   public static void main(String[] args) {
      SpringApplication.run(FeignClientServiceApplication.class, args);
   }

}

```

Start the app and test. How to test, debug - read the explanation above about the load balancer and the
two instances of the backend service. Don't forget that feign implements cache on the URL, meaning
if you hit the same URL twice, the second response will come from the cache and not from the backend
service.

   EXPLANATION:

1 What you get out-of-the-box
When you pull in spring-cloud-starter-loadbalancer every Feign call is routed through a child 
“load-balancer context” whose default bean is RoundRobinLoadBalancer. The algorithm is chosen 
per service-id and can be swapped (or replaced entirely) without touching your Feign interfaces.

2 Two places you can customise

| Surface                                                  | Typical bean you override            | What you can change                                                                                 |
| -------------------------------------------------------- | ------------------------------------ | --------------------------------------------------------------------------------------------------- |
| **Algorithm** (how to pick *one* instance)               | `ReactorServiceInstanceLoadBalancer` | Round-robin → Random, weighted, your own “least-conn”, etc.                                         |
| **Instance list / ordering** (what the algorithm *sees*) | `ServiceInstanceListSupplier` chain  | Zone filtering, sticky-session, health-check, “canary only”, custom metadata filters, caching, etc. |


Because Feign uses the blocking adapter (FeignBlockingLoadBalancerClient) it still honours whichever
beans you install in the child context; you don’t need extra Feign code.


3 Ready-made algorithms & filters you can switch on

| Feature                                                       | How to activate                                                                                   | Docs        |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ----------- |
| **Random** pick                                               | Provide a `RandomLoadBalancer` bean for the service                                               | ([Home][1]) |
| **Weighted** pick (metadata key `weight`)                     | `withWeighted()` in the supplier builder *or* `spring.cloud.loadbalancer.configurations=weighted` | ([Home][1]) |
| **Zone preference**                                           | `withZonePreference()` or set `spring.cloud.loadbalancer.zone=<zone>`                             | ([Home][1]) |
| **Same-instance-preference** (sticky, cookie-less)            | `withSameInstancePreference()` or `configurations=same-instance-preference`                       | ([Home][1]) |
| **Request-based sticky session** (cookie `sc-lb-instance-id`) | `withRequestBasedStickySession()` or `configurations=request-based-sticky-session`                | ([Home][1]) |
| **Health-check supplier**                                     | `withHealthChecks()`                                                                              | ([Home][1]) |
| **Caffeine cache** & TTL/capacity                             | Add Caffeine to the classpath and tweak `spring.cloud.loadbalancer.cache.*`                       | ([Home][1]) |
| **Hint-based routing**                                        | Set `spring.cloud.loadbalancer.hint.*` or send header `X-SC-LB-Hint`                              | ([Home][1]) |

[1]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html "Spring Cloud LoadBalancer :: Spring Cloud Commons"


7 Key points to remember
Child context – every service-id gets its own mini-ApplicationContext; put your beans there via @LoadBalancerClient so they don’t leak globally.
Feign sees everything – it calls the blocking adapter, which delegates to the very same beans used by WebClient/RestTemplate.
Supplier chain > algorithm – start by shaping the list (filters, caching, zones) and only then think about picking logic.
With those hooks you can move far beyond round-robin while keeping your existing Feign configuration completely intact.












