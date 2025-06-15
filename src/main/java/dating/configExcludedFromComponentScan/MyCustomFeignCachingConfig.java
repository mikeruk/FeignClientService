package dating.configExcludedFromComponentScan;

import feign.Capability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration - you don't need this here, because it's imported locally via: @FeignClient(configuration = {...,MyCustomFeignCachingConfig.class,...}
public class MyCustomFeignCachingConfig
{

    @Bean
    public Capability myCustomCachingCapability() {
        return new MyCustomCachingCapability();
    }
}

