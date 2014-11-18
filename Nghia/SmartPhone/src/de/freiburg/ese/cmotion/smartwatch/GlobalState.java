package de.freiburg.ese.cmotion.smartwatch;

import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Application;

public class GlobalState extends Application {

	private static DatagramSocket socket;

	public static DatagramSocket getSocket() throws SocketException {
		if (socket == null || socket.isClosed()) {
			socket = new DatagramSocket();
		}
		return socket;
	}

}
