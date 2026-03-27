package com.otis.model;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class Seat {
    private UUID id;
    private UUID eventId;
    private String seatNumber;
    private Boolean reserved;
    private UUID reservationId;
}
