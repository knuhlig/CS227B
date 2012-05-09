package util.networking;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

public class NetworkUtils {
	
	public static InetAddress getALocalIPAddress() throws SocketException, UnknownHostException {
		InetAddress address = null;
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	    for (NetworkInterface net : Collections.list(nets)) {
	    	if (!net.isLoopback() && !net.isVirtual() && !net.isPointToPoint() && net.isUp()) {
	    		for (InetAddress curAddress : Collections.list(net.getInetAddresses())) {
	    			if (curAddress != null) {
	    				address = curAddress;
	    			}
	    		}
	    	}
	    	if (address != null) break;
	    }
	    
	    // If we don't have a public IP address just use local host (127.0.0.1)
	    if (address == null) {
	    	address = InetAddress.getLocalHost();
	    }
	    
	    return address;
	}
}
