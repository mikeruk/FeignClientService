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
