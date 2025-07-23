package com.hodi.liuc.hodi_meter_server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class HodiMeterServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HodiMeterServerApplication.class, args);
    }

}