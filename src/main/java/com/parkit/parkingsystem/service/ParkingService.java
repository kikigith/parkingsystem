package com.parkit.parkingsystem.service;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;

public class ParkingService {

	private static final Logger logger = LogManager.getLogger("ParkingService");

	private static FareCalculatorService fareCalculatorService = new FareCalculatorService();

	private InputReaderUtil inputReaderUtil;
	private ParkingSpotDAO parkingSpotDAO;
	private TicketDAO ticketDAO;

	public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
		this.inputReaderUtil = inputReaderUtil;
		this.parkingSpotDAO = parkingSpotDAO;
		this.ticketDAO = ticketDAO;
	}

	public void processIncomingVehicle() {
		try {
			ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
			if (parkingSpot != null && parkingSpot.getId() > 0) {
				String vehicleRegNumber = getVehichleRegNumber();
				parkingSpot.setAvailable(false);
				parkingSpotDAO.updateParking(parkingSpot);// allot this parking space and mark it's availability as
															// false

				Date inTime = new Date();
				Ticket ticket = new Ticket();
				// ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
				// ticket.setId(ticketID);
				ticket.setParkingSpot(parkingSpot);
				ticket.setVehicleRegNumber(vehicleRegNumber);
				ticket.setPrice(0);
				ticket.setInTime(new Timestamp(new Date().getTime()));
				ticket.setOutTime(null);
				ticketDAO.saveTicket(ticket);
				System.out.println("Generated Ticket and saved in DB");
				System.out.println("Please park your vehicle in spot number:" + parkingSpot.getId());
				System.out.println("Recorded in-time for vehicle number:" + vehicleRegNumber + " is:" + inTime);
			}
		} catch (Exception e) {
			logger.error("Unable to process incoming vehicle", e);
		}
	}

	private String getVehichleRegNumber() throws Exception {
		System.out.println("Please type the vehicle registration number and press enter key");
		return inputReaderUtil.readVehicleRegistrationNumber();
	}

	public ParkingSpot getNextParkingNumberIfAvailable() {
		int parkingNumber = 0;
		ParkingSpot parkingSpot = null;
		try {
			ParkingType parkingType = getVehichleType();
			parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
			if (parkingNumber > 0) {
				parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
			} else {
				throw new Exception("Error fetching parking number from DB. Parking slots might be full");
			}
		} catch (IllegalArgumentException ie) {
			logger.error("Error parsing user input for type of vehicle", ie);
		} catch (Exception e) {
			logger.error("Error fetching next available parking slot", e);
		}
		return parkingSpot;
	}

	private ParkingType getVehichleType() {
		System.out.println("Please select vehicle type from menu");
		System.out.println("1 CAR");
		System.out.println("2 BIKE");
		int input = inputReaderUtil.readSelection();
		switch (input) {
		case 1: {
			return ParkingType.CAR;
		}
		case 2: {
			return ParkingType.BIKE;
		}
		default: {
			System.out.println("Incorrect input provided");
			throw new IllegalArgumentException("Entered input is invalid");
		}
		}
	}

	/**
	 * Process vehicle exiting
	 */
	public void processExitingVehicle() {
		try {
			String vehicleRegNumber = getVehichleRegNumber();
			Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

			ticket.setOutTime(new Timestamp(new Date().getTime()));

			fareCalculatorService.calculateFare(ticket);
			if (ticketDAO.updateTicket(ticket)) {

				if (isRecurringVehicle(vehicleRegNumber)) {
					ticket.setPrice(ticket.getPrice() * 0.95);
					ticketDAO.updateTicket(ticket);
				}

				ParkingSpot parkingSpot = ticket.getParkingSpot();
				parkingSpot.setAvailable(true);
				parkingSpotDAO.updateParking(parkingSpot);
				System.out.println("Please pay the parking fare:" + ticket.getPrice());
				System.out.println("Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber());
			} else {
				System.out.println("Unable to update ticket information. Error occurred");
			}
		} catch (Exception e) {
			logger.error("Unable to process exiting vehicle", e);
		}
	}

	/**
	 * Test if a given vehicle number is a recurring vehicle
	 * 
	 * @param vehicleRegNumber
	 * @return a boolean value
	 */
	public boolean isRecurringVehicle(String vehicleRegNumber) {
		// TODO Auto-generated method stub
		Ticket ticket = null;
		try {
			ticket = ticketDAO.getTicket(vehicleRegNumber);

		} catch (Exception e) {
			logger.error("Unable to retrieve ticket. Error occurred", e);
		}
		return (ticket != null) ? true : false;
	}
}
