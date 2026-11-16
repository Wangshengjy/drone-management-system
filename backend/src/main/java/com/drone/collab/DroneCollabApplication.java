package com.drone.collab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.drone.collab.mapper")
public class DroneCollabApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroneCollabApplication.class, args);
    }
}
