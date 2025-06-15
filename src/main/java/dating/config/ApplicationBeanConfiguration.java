package dating.config;

import dating.configExcludedFromComponentScan.MyCustomCachingCapability;
import feign.Capability;
import feign.Feign;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ApplicationBeanConfiguration
{

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(5000, 10000); // 5 sec connect, 10 sec read timeout
    }


    @Bean                 // in a config class *referenced by the client*
    @Scope("prototype")   // <- mandatory
    public Feign.Builder feignBuilder() {
        return Feign.builder();          // start plain
        // NB! Since I have already customized the Decoder in the CustomFeignClientConfiguration.java
        // via @Bean public Decoder feignDecoder, as next, I tried hard to use the builder to configure
        // the Encoder, but did not have success. The customization simply never activated!
        // I tried many different ways to avoid collisions between the customization provided by the
        // CustomFeignClientConfiguration.java and the customization provided by the builder, BUT it did
        // not work! I even commented out all other customizations done in separate @Configuration classes,
        // but still had no success with the public Feign.Builder feignBuilder()
    }




}
