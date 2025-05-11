package com.ll.carjini.global.data;

import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.car.repository.CarRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CarData {

    @Bean
    public ApplicationRunner init(CarRepository carRepository) {
        return args -> {
            if (carRepository.count() == 0) {
                List<Car> cars = Arrays.asList(
                        new Car("https://i.ibb.co/Z1X06T20/AVANTE-Hybrid.png", "Hyundai", "Sonata"),
                        new Car("https://i.ibb.co/jZ6cHWds/SANTA-FE.png", "Kia", "K5"),
                        new Car("https://i.ibb.co/HLPCHgWQ/SELTOS.png", "Samsung", "SM6")
                );

                carRepository.saveAll(cars);

                System.out.println("==== 초기 자동차 데이터가 성공적으로 추가되었습니다 ====");
            } else {
                System.out.println("==== 데이터가 이미 존재합니다. 초기화하지 않습니다. ====");
            }

            carRepository.findAll().forEach(System.out::println);
        };
    }
}

