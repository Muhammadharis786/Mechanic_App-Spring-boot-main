package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPriceDto {
    private Long requestId;
    private Double finalPrice;

}
