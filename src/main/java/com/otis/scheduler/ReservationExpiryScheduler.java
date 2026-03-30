package com.otis.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.otis.service.SeatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReservationExpiryScheduler {
	private final SeatService seatService;

	public ReservationExpiryScheduler(SeatService seatService) {
		this.seatService = seatService;
	}

	/**
	 * Release expired reservations every minute.
	 * This background job ensures seats are released when customers don't complete
	 * payment
	 * within the hold duration (10 minutes by default).
	 */
	@Scheduled(fixedRate = 60000) // Every 60 seconds
	public void releaseExpiredReservations() {
		log.debug("Checking for expired reservations...");
		try {
			int released = seatService.releaseExpiredReservations();
			if (released > 0) {
				log.info("Released {} expired reservation seats", released);
			}
		} catch (Exception e) {
			log.error("Error releasing expired reservations: ", e);
		}
	}
}
