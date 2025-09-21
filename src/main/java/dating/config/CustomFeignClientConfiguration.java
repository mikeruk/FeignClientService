package dating.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import feign.codec.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Configuration
public class CustomFeignClientConfiguration
{


    private static final Logger logger = LoggerFactory.getLogger(CustomFeignClientConfiguration.class);

    public CustomFeignClientConfiguration() {
        logger.info("CustomFeignClientConfiguration instantiated");
    }

    // NB! https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults
    //
    // Spring Cloud lets you take full control of the feign client by declaring
    // additional configuration (on top of the FeignClientsConfiguration) using
    // @FeignClient. Example:
    // @FeignClient(name = "stores", configuration = FooConfiguration.class)
    // public interface StoreClient {}
    //
    //  NB!!!! In this case the client is composed from the components already in FeignClientsConfiguration together with
    //  any in FooConfiguration (where the latter will override the former).
    //
    //
    // FooConfiguration does not need to be annotated with @Configuration. However,
    // if it is, then take care to exclude it from any @ComponentScan that would
    // otherwise include this configuration as it will become the default source for
    // feign.Decoder, feign.Encoder, feign.Contract, etc., when specified. This can be
    // avoided by putting it in a separate, non-overlapping package from any
    // @ComponentScan or @SpringBootApplication, or it can be explicitly excluded in
    // @ComponentScan.
    //
    // Spring Cloud OpenFeign provides the following beans by default for feign (BeanType beanName: ClassName):
    // See the list of Configuration classes in the official Docs:
    // https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults

    // Other beans Feign takes directly from Spring Core Framework - see the docs above.




    // Sometimes the backend service will be sending Java objects, which contain not only LocaleDateTime objects,
    // but also LocalDate or Date objects. Therefore, we also need to add serializers/de-serializers
    // for these types also, like so:
    @Bean
    public ObjectMapper customObjectMapper()
    {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

//        javaTimeModule.addSerializer(LocalDateTime.class,
//                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));

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


//        javaTimeModule.addSerializer(LocalDate.class,
//                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
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


//        javaTimeModule.addSerializer(Date.class,
//                new JsonSerializer<Date>() {
//                    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//
//                    @Override
//                    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//                        gen.writeString(formatter.format(value));
//                    }
//                });
        // Serializer for Date (output format: yyyy-MM-dd'T'HH:mm:ss.SSS), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'
        // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
        //If I don't need it, I can remove it/comment it out!
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
