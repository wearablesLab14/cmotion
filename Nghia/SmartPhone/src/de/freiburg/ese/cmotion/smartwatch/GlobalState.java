package de.freiburg.ese.cmotion.smartwatch;

import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Application;
import android.util.Log;

public class GlobalState extends Application {

	private static DatagramSocket socket;

	/**
	 * Creates a new datagram socket if not already created or closed and returns it.
	 * @return A datagram socket
	 * @throws SocketException
	 * @deprecated
	 */
	public static DatagramSocket getSocket() throws SocketException {
		if (socket == null || socket.isClosed()) {
			socket = new DatagramSocket();
			Log.d(MainActivity.TAG, "socket was null and will be created");
		}
		return socket;
	}
}
