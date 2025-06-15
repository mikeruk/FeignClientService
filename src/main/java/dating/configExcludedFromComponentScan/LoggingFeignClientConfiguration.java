package dating.configExcludedFromComponentScan;

import feign.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration - you don't need this here, because it's imported locally via: @FeignClient(configuration = {...,LoggingFeignClientConfiguration.class,...}
public class LoggingFeignClientConfiguration {

    @Bean
    public Client loggingFeignClient(Client defaultFeignClient) {
        // defaultFeignClient will be injected automatically, usually FeignBlockingLoadBalancerClient
        return new LoggingFeignClient(defaultFeignClient);
    }
}

