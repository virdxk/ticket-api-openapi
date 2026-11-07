package ru.itmo.soa.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = "ru.itmo.soa")
public class TicketApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
