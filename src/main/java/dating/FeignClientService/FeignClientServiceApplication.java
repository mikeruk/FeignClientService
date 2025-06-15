package dating.FeignClientService;

import dating.configExcludedFromComponentScan.BackendLoadBalancerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


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
