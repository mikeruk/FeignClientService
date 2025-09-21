//package dating.config;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.*;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
//import feign.codec.Decoder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
//import org.springframework.cloud.openfeign.support.SpringDecoder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//
//import java.io.IOException;
//import java.lang.reflect.ParameterizedType;
//import java.lang.reflect.Type;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.Date;
//
//@Configuration
//public class CustomFeignClientConfiguration_a_backup_copy
//{
//
//
//    private static final Logger logger = LoggerFactory.getLogger(CustomFeignClientConfiguration_a_backup_copy.class);
//
//    public CustomFeignClientConfiguration_a_backup_copy() {
//        logger.info("CustomFeignClientConfiguration instantiated");
//    }
//
//    // NB! https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults
//    //
//    // Spring Cloud lets you take full control of the feign client by declaring
//    // additional configuration (on top of the FeignClientsConfiguration) using
//    // @FeignClient. Example:
//    // @FeignClient(name = "stores", configuration = FooConfiguration.class)
//    // public interface StoreClient {}
//    //
//    //  NB!!!! In this case the client is composed from the components already in FeignClientsConfiguration together with
//    //  any in FooConfiguration (where the latter will override the former).
//    //
//    //
//    // FooConfiguration does not need to be annotated with @Configuration. However,
//    // if it is, then take care to exclude it from any @ComponentScan that would
//    // otherwise include this configuration as it will become the default source for
//    // feign.Decoder, feign.Encoder, feign.Contract, etc., when specified. This can be
//    // avoided by putting it in a separate, non-overlapping package from any
//    // @ComponentScan or @SpringBootApplication, or it can be explicitly excluded in
//    // @ComponentScan.
//    //
//    // Spring Cloud OpenFeign provides the following beans by default for feign (BeanType beanName: ClassName):
//    // See the list of Configuration classes in the official Docs:
//    // https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults
//
//    // Other beans Feign takes directly from Spring Core Framework - see the docs above.
//
//
//    // NB!! Here my Chat-GPT thread, explaining it - see in the SpringDocs folder, here inside the project !!!
//    // The ObjectMapper used by SpringDecoder and SpringEncoder comes from Jackson.
//    // You can customize it by creating a Jackson2ObjectMapperBuilder bean.
//    //
//    // HERE the Docs for: com.fasterxml.jackson.databind.ObjectMapper  !!!!!!!!!
//    // https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html
//    // Start 'thinking and analyzing' at that point from here (see above) and customize it !!!
//
//    // BUT then only thanks to the Chat-GPT I found out about the
//    // return Jackson2ObjectMapperBuilder.json().featuresToDisable... - see below:
//
//    // HOW ELSE could I make the connection between the official statement:
//    // , taken from here: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults
//    //
//    // Spring Cloud OpenFeign provides the following beans by default for feign (BeanType beanName: ClassName):
//    // Decoder feignDecoder: ResponseEntityDecoder (which wraps a SpringDecoder)
//    //
//    // AND THE below implemented code feign.optionals.OptionalDecoder:
//    //
//    //  public Decoder feignDecoder() {
//    //  ...
//    //          return new ResponseEntityDecoder(new SpringDecoder(() -> new HttpMessageConverters(converter)));
//    // }
//    //
//
//
//    //  THE ANSWER IS:
//    // First, open the docs for each of the mentioned classes:
//    // 'Decoder' => 'ResponseEntityDecoder' => 'SpringDecoder'
//    // Let's first check the implementation 'SpringDecoder' - see: https://www.javadoc.io/doc/org.springframework.cloud/spring-cloud-netflix-core/1.3.5.RELEASE/org/springframework/cloud/netflix/feign/support/SpringDecoder.html
//    // and we see that in takes ObjectFactory<HttpMessageConverters> as argument in the constructor.
//    // Logically, if we modify the HttpMessageConverters to use our DateFormat, and we pass this converter as argument to instantiate
//    // the SpringDecoder, then the decoder will be using our Date Format inside.
//    // And to instantiate HttpMessageConverters we need to pass a 'HttpMessageConverter<?>... additionalConverters'
//    // Now the question is, what available types ? of HttpMessageConverter we have?
//    // First, lets see what converters we have in the package: Package org.springframework.http.converter - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/package-summary.html
//    // and, we find the interface: Interface HttpMessageConverter<T> - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/HttpMessageConverter.html
//    // Next, read that list of available converters. We gonna convert from JSON to HTTP object - is there something suitable for us in that list?
//    // We find 'MappingJackson2HttpMessageConverter' there. Let's try to instantiate it and pass it
//    // as argument to instantiate the new HttpMessageConverters( )
//
//
//    //  This article also explains this internal OOP relation: https://sabzblog.medium.com/spring-feign-client-f89e4d63b0cb
//
//
//
////                        //  START of configuration   -   WORKS SUCCESS !!!!
////
////    // we need to configure two things:
////    // 1. a custom ObjectMapper - we must modify the object mapper and make him serialize and serialize using OUR custom DATE Formats!
////    // 2. feign Decoder - it must use our new custom ObjectMapper
////
////    // To Achieve this goal, this is how we do a very basic customization of the ObjectMapper
////    // This below is syntactical correct, but does not help us much:
//////    @Bean
//////    public ObjectMapper customObjectMapper() {
//////        return Jackson2ObjectMapperBuilder.json()
//////                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//////                .dateFormat(new SimpleDateFormat("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'"))
//////                .modules(new JavaTimeModule())
//////                .build();
//////    }
////    //, therefore we need to customize the ObjectMapper like this:
////
////    @Bean
////    public ObjectMapper customObjectMapper() {
////        JavaTimeModule javaTimeModule = new JavaTimeModule();
////
////        // Converting Java Object to JSON String is called 'serializing'.
////        // Converting JSON String to Java Object is called 'deserializing'.
////
////        // Our customObjectMapper will SERIALIZE when converting the
////        // java object to JSON String to send it to Postman as response.
////
////        // Our customObjectMapper will DE-SERIALIZE when converting
////        // the JSON Response, coming from backend-service, into a JAVA Object
////
////        // We need to say - what DateTimeFormatter.ofPattern to be used when serializing,
////        // sending the JSON Response back to Postman client,
////        // so:
////        javaTimeModule.addSerializer(LocalDateTime.class,
////                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'")));
////
////        // And we need to say - what DateTimeFormatter.ofPattern to be used when de-serializing,
////        // converting the JSON Response, coming from backend-service, into a JAVA Object.
////        // We expect that the backend-service will be sending us objects containing LocalDateTime variables,
////        // therefore we need to add such deserializer specifically, so:
////        javaTimeModule.addDeserializer(LocalDateTime.class,
////                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
////                {
////                    @Override
////                    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
////                        String text = p.getText();
////                        try {
////                            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
////                        } catch (DateTimeParseException e) {
////                            logger.warn("Failed to parse date with yyyy-MM-dd'T'HH:mm: {}, trying ISO format", text);
////                            try {
////                                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
////                            } catch (DateTimeParseException e2) {
////                                logger.error("Failed to parse date: {}", text, e2);
////                                throw e2;
////                            }
////                        }
////                    }
////                });
////
////        // If the backend-service will be sending us objects containing other date formats like:
////        // Date, LocalDateTime variables, etc, then add more  LocalDateTimeDeserializers accordingly here below...
////
////        logger.info("Pattern yyyy-MM-dd'T'HH:mm must have been loaded");
////        return Jackson2ObjectMapperBuilder.json()
////                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
////                .modules(javaTimeModule)
////                .build();
////    }
////
////
////    // Now, lets create the feign Decoder - it must use our new custom ObjectMapper
////    // Custom Feign Decoder -
////    // the returned feign decoder is modified only for the part of
////    // MappingJackson2HttpMessageConverter, which contains objectMapper with our custom DateFormat
////    @Bean
////    public Decoder feignDecoder()
////    {
////        // By default, the HttpMessageConverters object is called by getObject()
////        // Spring provides an interface called ObjectFactory<T>, which is essentially a factory pattern abstraction for creating instances on demand.
////        // ObjectFactory<T> is a functional interface, meaning it has only one method: getObject()
////        // We have to make sure that when calling this method getObject(),
////        // it will return the HttpMessageConverters, which already contains our custom ObjectMapper.
////        // We achieve this by @ Overriding it!
////        // The overriding is achieved by creating the so called an anonymous inner class,
////        // so:
////        ObjectFactory<HttpMessageConverters> objectFactory = new ObjectFactory<>() { // this is new anonymous inner class
////            @Override
////            public HttpMessageConverters getObject() {
////
////                // First, lets see what converters we have in the package: Package org.springframework.http.converter - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/package-summary.html
////                // and, we find the interface: Interface HttpMessageConverter<T> - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/converter/HttpMessageConverter.html
////                // Next, read that list of available converters. We gonna convert from JSON to HTTP object - is there something for us in the list?
////                // We find 'MappingJackson2HttpMessageConverter' there. Let's try to instantiate it and pass it
////                // as argument to instantiate the new HttpMessageConverters( )
////                return new HttpMessageConverters(new MappingJackson2HttpMessageConverter(customObjectMapper()));
////            }
////        };
////        SpringDecoder springDecoder = new SpringDecoder(objectFactory);
////        return springDecoder;
////        //// WORKS SUCCESS !!! - just a shorter syntax of the code above
////        //// OR alternative (the short version) of the above code is this - it does exactly the same:
////        // HttpMessageConverter<?> converter = new MappingJackson2HttpMessageConverter(customObjectMapper());
////        // return new ResponseEntityDecoder(new SpringDecoder(() -> new HttpMessageConverters(converter)));
////
////
////        //           NB !!! MOST IMPORTANT REMARK for feign Decoder:
////        // it works with SUCCESS only if we send/receive POJO Objects like: userDbDTO, userDTO, etc.
////        // In other words the backend-service sends as response userDbDTO and that feign client service also receives
////        // and sends forward such POJO userDbDTO, here example java code:
////        //
////        //
////        // Postman Client sends request to: http://localhost:8082/api/v2/proxy-user/8
////        //
////        // And this is the Controller in feign client service:
////        //              @GetMapping("/proxy-user/{id}")
////        //              public UserDTO proxyGetUser(@PathVariable Long id) {
////        //              UserDTO userById = feignClient.getUserById(id);
////        //              return userById;
////        //
////        // This is the Controller in backend service:
////        //              @GetMapping("/user/{id}")
////        //              public User getUserById(@PathVariable Long id) throws JsonProcessingException {
////        //              User user = userService.selectUserByPrimaryKey(id).orElse(null);
////        //              return user;
////        // And the Postman client receives the response body which looks so:
////        //  {
////        //      "id": 8,
////        //      "ts": "Arrr, it be the 1 day of January in the year 0001 at the hour of 12:00 am o'clock!"
////        //  }
////        //
////        // the custom date format was implemented with success !!!
////
////
////
////    }
////
////                        // END of configuration -   WORKS SUCCESS !!!!
//
//
//
//
//
//
//
//
//    // Below is the same configuration for customObjectMapper(...) as above marked with START and END, but
//    // just with few additions, to make the configuration more advanced:
//
//
//
//    // Sometimes the backend service will be sending Java objects, which contain not only LocaleDateTime objects,
//    // but also LocalDate or Date objects. Therefore, we also need to add serializers/de-serializers
//    // for these types also, like so:
//    @Bean
//    public ObjectMapper customObjectMapper()
//    {
//        JavaTimeModule javaTimeModule = new JavaTimeModule();
//
////        javaTimeModule.addSerializer(LocalDateTime.class,
////                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
//
//        // Serializer for LocalDateTime (output format: yyyy-MM-dd'T'HH:mm), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'
//        // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
//        // If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addSerializer(LocalDateTime.class,
//                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'")));
//
//        // Deserializer for LocalDateTime with primary and fallback formats
//        // If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addDeserializer(LocalDateTime.class,
//                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) {
//                    @Override
//                    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                        String text = p.getText();
//                        try {
//                            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
//                        } catch (DateTimeParseException e) {
//                            logger.debug("Failed to parse LocalDateTime with yyyy-MM-dd'T'HH:mm:ss.SSS: {}, trying other formats", text);
//                            try {
//                                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
//                            } catch (DateTimeParseException e2) {
//                                logger.debug("Failed to parse LocalDateTime with yyyy-MM-dd'T'HH:mm: {}, trying ISO format", text);
//                                try {
//                                    return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//                                } catch (DateTimeParseException e3) {
//                                    logger.error("Failed to parse LocalDateTime: {}", text, e3);
//                                    throw e3;
//                                }
//                            }
//                        }
//                    }
//                });
//
//
////        javaTimeModule.addSerializer(LocalDate.class,
////                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
//        // Serializer for LocalDate (output format: yyyy-MM-dd), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInLocalDate:noMinsInLocalDate o''clock!'
//        // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
//        // If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addSerializer(LocalDate.class,
//                new LocalDateSerializer(DateTimeFormatter.ofPattern("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInLocalDate:noMinsInLocalDate o''clock!'")));
//
//        // Deserializer for LocalDate with primary and fallback formats
//        // If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addDeserializer(LocalDate.class,
//                new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")) {
//                    @Override
//                    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                        String text = p.getText();
//                        try {
//                            return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//                        } catch (DateTimeParseException e) {
//                            logger.debug("Failed to parse LocalDate with yyyy-MM-dd: {}, trying other formats", text);
//                            try {
//                                return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyyMMdd"));
//                            } catch (DateTimeParseException e2) {
//                                logger.debug("Failed to parse LocalDate with yyyyMMdd: {}, trying ISO format", text);
//                                try {
//                                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
//                                } catch (DateTimeParseException e3) {
//                                    logger.error("Failed to parse LocalDate: {}", text, e3);
//                                    throw e3;
//                                }
//                            }
//                        }
//                    }
//                });
//
//
////        javaTimeModule.addSerializer(Date.class,
////                new JsonSerializer<Date>() {
////                    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
////
////                    @Override
////                    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
////                        gen.writeString(formatter.format(value));
////                    }
////                });
//        // Serializer for Date (output format: yyyy-MM-dd'T'HH:mm:ss.SSS), but I replaced with: 'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of' hh:mm a 'o''clock!'
//        // Here I basically say - when converting the java object into JSON String, then make the date looks like so:
//        //If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addSerializer(Date.class,
//                new JsonSerializer<Date>() {
//                    private final SimpleDateFormat formatter = new SimpleDateFormat("'Arrr, it be the' d 'day of' MMMM 'in the year' yyyy 'at the hour of noHourInDate:noMinsInDate o''clock!'");
//
//                    @Override
//                    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//                        gen.writeString(formatter.format(value));
//                    }
//                });
//
//        // Deserializer for Date with primary and fallback formats
//        // If I don't need it, I can remove it/comment it out!
//        javaTimeModule.addDeserializer(Date.class,
//                new JsonDeserializer<Date>() {
//                    private final SimpleDateFormat primaryFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//                    private final SimpleDateFormat fallback1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
//                    private final SimpleDateFormat fallback2 = new SimpleDateFormat("yyyy-MM-dd");
//
//                    @Override
//                    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                        String text = p.getText();
//                        try {
//                            return primaryFormat.parse(text);
//                        } catch (ParseException e) {
//                            logger.debug("Failed to parse Date with yyyy-MM-dd'T'HH:mm:ss.SSS: {}, trying other formats", text);
//                            try {
//                                return fallback1.parse(text);
//                            } catch (ParseException e2) {
//                                logger.debug("Failed to parse Date with yyyy-MM-dd'T'HH:mm: {}, trying yyyy-MM-dd", text);
//                                try {
//                                    return fallback2.parse(text);
//                                } catch (ParseException e3) {
//                                    logger.error("Failed to parse Date: {}", text, e3);
//                                    throw new IOException("Failed to parse Date: " + text, e3);
//                                }
//                            }
//                        }
//                    }
//                });
//
//        logger.info("Custom date mappers for LocalDateTime, LocalDate, and Date loaded");
//        return Jackson2ObjectMapperBuilder.json()
//                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//                .modules(javaTimeModule)
//                .build();
//    }
//
//
//    // NB! At this point, the above ObjectMapper Bean is completely enough for the feign client to work and use it!
//    // I don't really need to explicitly customize a new Decoder, as I do below. This is because apparently the default
//    // feign Decoder uses the ObjectMapper and this is completely enough to do my custom date formats!
//    // Below, the code @Bean public Decoder feignDecoder(ObjectMapper customObjectMapper) can be deleted / commented out
//    // and the object mapper above would still be applied in feign client!
//    // HOWEVER, for the purpose of a Demo how to customize feign Decoder, I still implement the Bean below:
//    // Below is the same configuration for Decoder feignDecoder(...) as above marked with START and END, but
//    // just with few additions, to make the configuration more advanced:
//
//    // Sometimes the backend service will be sending not simple POJO objects as a response body, but could probably
//    // wrap them in other objects like ResponseEntity<EntityModel<User>> which would throw error if we do not implement
//    // the Decoder like this below:
//    @Bean
//    public Decoder feignDecoder(ObjectMapper customObjectMapper)
//    {
//        // By default, the Feign uses Jackson for JSON serialization/deserialization
//        // (and does not support XML context for serialization/deserialization)
//        // if the jackson-databind library is on the classpath.
//        // The default Encoder (e.g., SpringEncoder) and default Decoder (e.g., SpringDecoder) serialize Java objects into JSON and vice versa for
//        // the request body (with Content-Type: application/json).
//        // My existing configuration below still uses the default JSON context for conversion, but it simply specifies which
//        // exactly converter to be used - the MappingJackson2HttpMessageConverter for JSON. That's all!
//
//        //   BELOW I am OVERWRITING the default HttpMessageConverters of Feign !!!
//        //By Default Feign contains these all Converters:
//        //Spring Boot’s default HttpMessageConverters typically include:
//        //MappingJackson2HttpMessageConverter: For JSON (Content-Type: application/json),
//        //if jackson-databind is on the classpath (which it usually is in Spring Boot applications).
//        //StringHttpMessageConverter: For plain text (Content-Type: text/plain).
//        //ByteArrayHttpMessageConverter: For binary data (Content-Type: application/octet-stream).
//        //Others, depending on the classpath (e.g., MappingJackson2XmlHttpMessageConverter for XML if jackson-dataformat-xml is present).
//        MappingJackson2HttpMessageConverter jacksonConverter =
//                new MappingJackson2HttpMessageConverter(customObjectMapper);
//        // However, AFTER the ABOVE specific configuration implementation, I’ve overridden
//        // the default HttpMessageConverters to use only: MappingJackson2HttpMessageConverter !!!
//        // Here, you’ve explicitly defined only one converter (MappingJackson2HttpMessageConverter) for
//        // JSON. This means your Feign client’s decoder is configured to handle only
//        // JSON responses (Content-Type: application/json). If the server returns a response with a
//        // different Content-Type (e.g., application/xml or application/soap+xml), the decoder will
//        // fail unless you add support for those formats. And also the dependency: jackson-dataformat-xml
//
//        // NB!!!! So, if you want the feign client to turn into a wide range WEB Client
//        // capable of sending HTTP or SOAP
//        // then add support for all formats and converters!!!
//
//
//        //      Feign client is Client-side HTTP request library for calling remote services (REST, SOAP, etc.).
//        // So, how does it compare other clients, which use native SOAP and XML?
//        //      1. Understanding Feign and Its Capabilities
//        //Feign is a client-side HTTP request library designed to simplify making HTTP requests to
//        // remote services. It is primarily used for RESTful APIs but can be configured to work with
//        // other types of HTTP-based communication, including services that use XML or SOAP-like
//        // payloads. Here’s a breakdown:
//        //HTTP-Based: Feign operates over HTTP/HTTPS, using standard HTTP methods (GET, POST, PUT,
//        // etc.) and headers. It sends HTTP requests and receives HTTP responses, with the request/response
//        // body formatted according to the configured Encoder/Decoder.
//        //Body Format: Feign is agnostic about the content of the request/response body. By default,
//        // it supports JSON (application/json) via MappingJackson2HttpMessageConverter, but you can
//        // configure it to handle other formats, such as XML (application/xml) or SOAP XML (application/soap+xml),
//        // by providing appropriate converters. Not a SOAP Client by Design: Feign is not a dedicated
//        // SOAP client. SOAP is a protocol that uses XML for message formatting and typically operates
//        // over HTTP (though it can use other transports like SMTP). While Feign can send HTTP requests
//        // with SOAP-formatted XML bodies, it does not natively understand SOAP’s protocol-specific features
//        // (e.g., SOAP envelopes, namespaces, faults, or WS-* standards like WS-Security).
//        //       2. Can Feign Send Requests Using the SOAP Protocol?
//        //The short answer is: Feign can send HTTP requests with SOAP-formatted XML bodies, but it does not
//        // natively implement the full SOAP protocol. Let’s clarify what this means:
//        //Feign and HTTP with SOAP XML
//        //Feign can send an HTTP request with a body formatted as SOAP XML (e.g.,
//        // Content-Type: application/soap+xml) if you manually customize the Encoder to serialize a Java
//        // object into a SOAP-compliant XML message and a Decoder to deserialize the SOAP response.
//        ///     ...
//        //      4. Feign vs. SOAP Protocol
//        //To address your confusion about Feign sending requests “via HTTP but capable of having an XML body”
//        // versus using the SOAP protocol:
//        //Feign’s Transport: Feign always uses HTTP/HTTPS as its transport mechanism. It sends HTTP requests
//        // with headers, methods, and bodies as configured.
//        //XML Body: Feign can include an XML body (e.g., application/xml or application/soap+xml) if you
//        // configure an Encoder to produce XML. For SOAP, this XML must conform to SOAP’s structure (envelope, body, etc.).
//        //SOAP Protocol: SOAP is a protocol that defines:
//        //A specific XML message format (envelope, header, body, fault).
//        //Rules for processing messages (e.g., handling faults).
//        //Optional features like WS-* standards.
//        //Transport independence (HTTP, SMTP, etc., though HTTP is most common).
//        //Feign can send an HTTP request with a SOAP XML body, which a SOAP server (e.g., a Spring-WS @Endpoint)
//        // will interpret as a SOAP request. However, Feign itself does not “speak” the SOAP protocol—it treats
//        // the SOAP XML as a payload and relies on external libraries (e.g., spring-ws-core) to format it correctly.
//        // This is why Feign requires custom configuration to handle SOAP, unlike dedicated SOAP clients.
//        // I COULD CONCLUDE that I'd better use Feign client to send HTTP with XML body, instead of trying to
//        // communicate with WEB Service utilizing SOAP protocol, since Feign uses HTTP natively.
//        // Even better - to communicate with SOAP service, better use the client libraries. Here is comparison:
//        //      5. Comparison: Feign with XML/SOAP vs. Dedicated SOAP Clients
//        //To clarify Feign’s ability to send SOAP requests, let’s compare it to dedicated SOAP clients like
//        // Spring Web Services’ WebServiceTemplate or Apache CXF:
//
//        //      Aspect:                      Feign with SOAP XML:                       Dedicated SOAP Client (e.g., WebServiceTemplate):
//
//        //      Transport                   HTTP/HTTPS only.                            HTTP, SMTP, JMS, etc. (HTTP most common).
//
//
//        //      SOAP Protocol Support       Limited; sends SOAP XML over HTTP           Full SOAP protocol support, including envelopes, faults, and WS-*.
//        //                                  but doesn’t natively handle SOAP
//        //                                  features (e.g., WS-*).
//        //
//        //      Configuration               Requires custom                             Configured with marshallers and SOAP-specific settings.
//        //                                  Encoder Decoder(e.g.,
//        //                                  using spring-ws-core or JAXB).
//
//        //       XML Handling               Supports XML with jackson-dataformat-xml    Native XML/SOAP handling via marshallers (e.g., JAXB, XStream).
//        //                                  or custom SOAP XML with spring-ws-core
//
//        //       Ease of Use for SOAP       Complex; requires manual SOAP message       Simple; designed for SOAP with built-in support for envelopes, faults.
//        //                                  construction.
//
//        //      Use Case                    Best for REST; can be adapted for SOAP      Built for SOAP web services.
//        //                                  with effort.
//
//
//        //Key Point: Feign can send SOAP XML over HTTP, but it’s not a natural fit for SOAP due to the need for custom
//        // configuration and lack of native SOAP protocol support. Dedicated SOAP clients are typically easier for
//        // SOAP-based communication.
//        //      7. Addressing Your Misunderstanding
//        //You mentioned thinking that “Feign sends only via HTTP but is capable of having an XML body.” This is correct,
//        // and it aligns with Feign’s capabilities:
//        //HTTP Only: Feign always uses HTTP/HTTPS as its transport. It cannot use other SOAP transports like SMTP.
//        //XML Body: Feign can include an XML body (including SOAP XML) if configured with an appropriate Encoder.
//        // For SOAP, this XML must conform to SOAP’s structure, which requires a library like spring-ws-core or JAXB to
//        // generate correctly.
//        //SOAP Protocol: Feign does not natively implement the SOAP protocol (e.g., it doesn’t handle SOAP faults or
//        // WS-* standards). It can send SOAP XML over HTTP, which is sufficient for many SOAP services that use HTTP
//        // as the transport, but it’s not a full SOAP client.
//        //The example Feign client with consumes = "application/soap+xml" demonstrates that Feign can send SOAP XML over
//        // HTTP, but it relies on a custom configuration to format the SOAP message correctly.
//        //      9. Recommendations
//        //For XML Requests: Configure Feign with jackson-dataformat-xml and MappingJackson2XmlHttpMessageConverter to
//        // handle application/xml. This is straightforward and works for generic XML payloads.
//        //For SOAP Requests: You can configure Feign to send SOAP XML over HTTP, as shown in the example, but it requires
//        // custom Encoder/Decoder logic (e.g., using spring-ws-core). However, for SOAP, consider using a dedicated SOAP client like:
//        //Spring Web Services’ WebServiceTemplate: Designed for SOAP, with built-in support for envelopes, faults,
//        // and WS-* standards.
//        //Apache CXF: A robust SOAP client/server framework.
//        //These are easier for SOAP than adapting Feign.
//        //Verify Server Support: Ensure the backend-service accepts application/soap+xml and responds correctly.
//        //Test with a tool like Postman or SoapUI to confirm the SOAP endpoint’s behavior.
//
//
//        // Above is explained about Feign, XML, SOAP and other SOAP native clients.
//        // But what about the WEB Service with SOAP? it looks like this:
//        //@Endpoint
//        //public class MyEndpoint {
//        //
//        //    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "nameOfTheRequest")
//        //    @ResponsePayload
//        //    public MyCustomResponse getSomeObject(@RequestPayload final GetSomeRequest request) {
//        //        // Process the request and return a response
//        //    }}
//        //The @Endpoint annotation in Spring Web Services (Spring-WS) marks a class as a SOAP endpoint, which handles incoming SOAP requests and produces SOAP responses. Spring-WS is a framework for building SOAP-based web services, focusing on contract-first development (starting with a WSDL or XSD schema). The methods in an @Endpoint class process XML payloads (typically SOAP messages) based on their namespace and local part, as defined by the @PayloadRoot annotation.
//        //@Endpoint: Marks the class as a SOAP endpoint, similar to how @RestController marks a class for REST in Spring MVC.
//        //@PayloadRoot: Specifies which incoming SOAP requests this method handles, based on the XML payload’s namespace (NAMESPACE_URI) and root element name (nameOfTheRequest). For example, it matches a SOAP request with a <nameOfTheRequest> element in the specified namespace.
//        //@RequestPayload: Indicates that the method parameter (GetSomeRequest) is the deserialized XML payload from the SOAP request body. Spring-WS uses a marshaller (e.g., JAXB, XStream) to convert the XML into a Java object.
//        //@ResponsePayload: Indicates that the method’s return value (MyCustomResponse) will be serialized into XML for the SOAP response body.
//        //How It Works
//        //SOAP Request Handling: When a SOAP request arrives (e.g., with Content-Type: application/soap+xml), Spring-WS routes it to the appropriate @Endpoint method based on the @PayloadRoot criteria.
//        //Deserialization: The XML payload (e.g., <nameOfTheRequest>) is deserialized into a GetSomeRequest object using a configured marshaller.
//        //Serialization: The MyCustomResponse object returned by the method is serialized into XML for the SOAP response.
//        //XML by Default: Spring-WS is designed for XML-based communication (specifically SOAP), so it natively handles application/soap+xml and application/xml without requiring additional configuration for XML support.
//        //2. Is This a Feign Client Implementation?
//        //No, this is not a Feign client implementation. Here’s why:
//        //Feign Client: A Feign client (e.g., your BackendFeignClient) is a client-side interface used to make HTTP requests to a remote service (REST or otherwise). It’s defined with @FeignClient and uses annotations like @GetMapping or @PostMapping to specify HTTP methods, paths, and headers. Feign handles serialization/deserialization using Encoder/Decoder and HttpMessageConverters.
//        //Spring-WS Endpoint: An @Endpoint class is a server-side component that handles incoming SOAP requests. It’s part of a web service implementation, not a client. It processes XML payloads (typically SOAP) and produces XML responses, using Spring-WS’s marshalling infrastructure.
//        //In summary:
//        //Feign client = Client making HTTP requests (e.g., to a REST or SOAP service).
//        //Spring-WS @Endpoint = Server handling SOAP requests.
//
//
//
//
//        HttpMessageConverters converters = new HttpMessageConverters(jacksonConverter);
//
//        return (response, type) -> {
//            logger.info("Decoding response for type: {}", type.getTypeName());
//
//            if (type instanceof ParameterizedType parameterizedType
//                    && parameterizedType.getRawType().equals(ResponseEntity.class))
//            {
//                Type bodyType = parameterizedType.getActualTypeArguments()[0];
//                logger.info("Deserializing ResponseEntity with body type: {}", bodyType.getTypeName());
//                Object body = new SpringDecoder(() -> converters).decode(response, bodyType);
//                return ResponseEntity.status(response.status())
//                        .headers(new HttpHeaders())
//                        .body(body);
//            }
//            logger.info("Using default decoder for type: {}", type.getTypeName());
//            return new SpringDecoder(() -> converters).decode(response, type);
//        };
//
//
//        // Postman Client sends request to: http://localhost:8082/api/v2/proxy-user/8
//        //
//        // And this is the Controller in feign client service:
//        //        @GetMapping("/proxy-user/{id}")
//        //        public ResponseEntity<UserDTO> proxyGetUser(@PathVariable Long id) {
//        //        ResponseEntity<UserDTO> userById = feignClient.getUserById(id);
//        //        return userById;
//        //        }
//        //
//        // This is the Controller in backend service:
//        //        @GetMapping("/user/{id}")
//        //        public ResponseEntity<EntityModel<User>> getUserById(@PathVariable Long id) throws JsonProcessingException {
//        //        User user = userService.selectUserByPrimaryKey(id).orElse(null);
//        //        if (user == null) {
//        //            return ResponseEntity.notFound().build();
//        //        }
//        //        return ResponseEntity.ok(EntityModel.of(user, addLinksToUser(user)));
//        //        }
//        //
//        // And the Postman client receives the response body which looks so:
//        //  {
//        //      "id": 8,
//        //      "ts": "Arrr, it be the 1 day of January in the year 0001 at the hour of 12:00 am o'clock!"
//        //  }
//        //
//        // the custom date format was implemented with success !!!
//
//
//    }
//
//
//
//
//
//
//    public void testDateFormat(ObjectMapper objectMapper) {
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDate today = LocalDate.now();
//        Date currentDate = new Date();
//
//        try {
//            // Test LocalDateTime
//            String formattedDateTime = objectMapper.writeValueAsString(now);
//            logger.info("Serialized LocalDateTime: {}", formattedDateTime);
//            LocalDateTime deserializedDateTime = objectMapper.readValue(formattedDateTime, LocalDateTime.class);
//            logger.info("Deserialized LocalDateTime: {}", deserializedDateTime);
//
//            // Test LocalDate
//            String formattedLocalDate = objectMapper.writeValueAsString(today);
//            logger.info("Serialized LocalDate: {}", formattedLocalDate);
//            LocalDate deserializedLocalDate = objectMapper.readValue(formattedLocalDate, LocalDate.class);
//            logger.info("Deserialized LocalDate: {}", deserializedLocalDate);
//
//            // Test Date
//            String formattedDate = objectMapper.writeValueAsString(currentDate);
//            logger.info("Serialized Date: {}", formattedDate);
//            Date deserializedDate = objectMapper.readValue(formattedDate, Date.class);
//            logger.info("Deserialized Date: {}", deserializedDate);
//        } catch (Exception e) {
//            logger.error("Error processing date formats", e);
//        }
//    }
//
//
//}
