package ru.itmo.soa.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = "ru.itmo.soa")
public class BookingApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
