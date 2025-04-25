package com.ll.carjini.domain.car.controller;


import com.ll.carjini.domain.car.dto.CarResponse;
import com.ll.carjini.domain.car.service.CarService;
import com.ll.carjini.global.dto.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/car")
@Tag(name = "차량 API", description = "선택할 수 있는 차량 조회 API")
public class CarController {

    private final CarService carService;

    @GetMapping
    @Operation(summary = "선택할 수 있는 차량 조회", description = "사용자가 선택할 수 있는 차량들을 조회합니다.")
    public GlobalResponse<List<CarResponse>> getCarOwner(
    ) {
        List<CarResponse> response = carService.getAllCars();

        return GlobalResponse.success(response);
    }

}
