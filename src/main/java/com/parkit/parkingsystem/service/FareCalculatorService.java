package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	/**
	 * Calculate a ticket fare considering the duration in fraction of time(ie
	 * 30min=0.5hour, 15min=0.25hour).
	 * 
	 * The fare is 0 when duration <= 0.5
	 * 
	 * @param ticket
	 */
	public void calculateFare(Ticket ticket) {
		System.out.println("In== " + ticket.getInTime().toString() + " Out== " + ticket.getOutTime().toString());
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString()
					+ " In time" + ticket.getInTime().toString());
		}

		double duration = (ticket.getOutTime().getTime() - ticket.getInTime().getTime()) / (60.0 * 60.0 * 1000.0);

		if (duration <= 0.5)// <30 min minute (half an hour) parking should be free.
			ticket.setPrice(0.0);
		else {
			switch (ticket.getParkingSpot().getParkingType()) {
			case CAR: {
				ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
				break;
			}
			case BIKE: {
				ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
				break;
			}
			default:
				throw new IllegalArgumentException("Unkown Parking Type");
			}
		}
	}
}