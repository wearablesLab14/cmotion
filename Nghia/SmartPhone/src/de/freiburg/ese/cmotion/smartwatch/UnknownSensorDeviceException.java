package de.freiburg.ese.cmotion.smartwatch;

public class UnknownSensorDeviceException extends RuntimeException {

	private static final long serialVersionUID = -5283543310176311952L;

	public UnknownSensorDeviceException() {
	}

	public UnknownSensorDeviceException(String message) {
		super(message);
	}

	public UnknownSensorDeviceException(Throwable cause) {
		super(cause);
	}

	public UnknownSensorDeviceException(String message, Throwable cause) {
		super(message, cause);
	}
}
