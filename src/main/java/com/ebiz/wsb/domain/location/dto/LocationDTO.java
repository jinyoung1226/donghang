package com.ebiz.wsb.domain.location.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationDTO {
    private double latitude;
    private double longitude;
}
