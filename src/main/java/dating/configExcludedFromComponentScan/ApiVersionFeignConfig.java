package dating.configExcludedFromComponentScan;

import feign.Contract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class ApiVersionFeignConfig {

    @Value("${feign.api.key}")
    private String apiKey;

    @Bean
    @Scope("prototype")
    public Contract feignContract() {
        return new ApiVersionContract(apiKey);
    }
}

