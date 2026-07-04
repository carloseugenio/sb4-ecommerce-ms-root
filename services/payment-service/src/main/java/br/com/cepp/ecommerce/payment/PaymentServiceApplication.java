package br.com.cepp.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Force Spring to scan both the microservice package and the shared config package
@SpringBootApplication(scanBasePackages = {
        "br.com.cepp.ecommerce.payment",
        "br.com.cepp.ecommerce.security"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
