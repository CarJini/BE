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
            // 샘플 자동차 데이터 생성
            List<Car> cars = Arrays.asList(
                    new Car( "https://ibb.co/zHxzhf6z", "Hyundai", "Sonata"),
                    new Car("https://ibb.co/Pzm7t9R0", "Kia", "K5"),
                    new Car("https://ibb.co/7xpQ1ywF","Samsung", "SM6")
            );

            carRepository.saveAll(cars);

            System.out.println("==== 초기 자동차 데이터가 성공적으로 추가되었습니다 ====");
            carRepository.findAll().forEach(System.out::println);
        };
    }
}
