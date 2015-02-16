/**
 * Class 10 - BANNER GRABBER
 *
 * This is a simple task to demonstrate how to implement a synchronous network client task to connect and interact with a remote service across the network.
 * The task can be used for 'banner grabbing', which is a very basic real-world hacking technique to find out information about the services running on open ports of a remote machine.
 * Basically, we connect to a remote port, possibly send some data to it to trigger a response. That response might reveal information about what service we are dealing with, which version etc.
 *
 * Network communication in Hax is much like in the real world. On the lowest level, streams of bytes is what flows through the network.
 * If we have some data in a byte array which we want to send across the network to a remote machine, we have to wrap it in a 'packet'.
 * A packet is constructed simply by prepending our data bytes, commonly called 'payload', with some additional bytes which form the 'header'.
 * The header contains the source and destination network addresses and ports for the data contained in the payload section.
 * The packet consisting of header and payload is the actual array of bytes that travels through the network to its destination.
 * Some aspects of building and decoding a packet are handled internally by the operating system, but some need to be handled by the task developer.
 * The operating system also provides to the developer a set of APIs to make things simpler when implementing tasks for networking.
 *
 * Note that in Hax, connections or sessions (stateful interaction with a remote service) must be implemented on the application level. There is no implicit support for this in the operating system.
 * The implication of this is that every task developer has to create his own mechanism for handling interaction in sessions.
 * There are some recommendations on how to implement this, which we will discuss in more detail in a later sample on network server tasks.
 * For the client side of things, we however need to know the recommended way to handle session creation and destruction.
 * The recommendation in this case is to use null packets for establishing and closing sessions. A null packet is simply a packet with an empty payload.
 * A session is established and persisted on the server side for the source address and port of the received null packet.
 * In turn, if a null packet is received from an address and port for which a session already exists, this is interpreted as a request to close the session.
 * The remote task daemon of HaxOS (rtaskd) works exactly this way, and it is recommended to implement all network servers like this to preserve consistency.
 *
 * This sample task simply sends out a null packet to a destination address and port.
 * It then synchronously waits some time to receive a response from the remote side. If a response arrives, it is printed to the console.
 * Note that this is a very basic form of banner grabbing, since we are not interacting further with the remote service, we only expect to receive its initial prompt.
 * However, this is sufficient to demonstrate some of the APIs that HaxOS provides for developing networking tasks.
 *
 * We will also use this task to demonstrate the use of command line arguments.
 * When starting a task from the HaxOS shell prompt, the user can provide additional arguments on the command line, separated by spaces.
 * E.g. if this task is started by typing 'BannerGrab.groovy server1 1010', the strings 'server1' and '1010' are the command line arguments.
 * The HaxOS shell automatically sets these arguments into a string array called 'ARGS'. The task is then able to access these arguments and use them as needed.
 * Actually, you can initially try starting this task with the exact same command mentioned above. You should get the string 'Hello world' printed to the console.
 * This is because the data center contains another machine besides your own, called 'server1', which has the level0 Hello world script running as a service on network port '1010'.
 *
 * Note: if you do not understand any line of code with no comment on it, that line was certainly explained before, so try looking for it in the previous samples.
 */

package sk.hax.software.sample.level6

import sk.hax.network.NetworkUtils
import sk.hax.software.os.Datagram
import sk.hax.software.os.Kernel
import sk.hax.software.os.SyncChannel
import sk.hax.software.os.Terminal
import sk.hax.system.Task


/*
 * The task class is called 'BannerGrab' (fully qualified name is 'sk.hax.software.sample.level6.BannerGrab').
 * 
 */
class BannerGrab implements Task {

	/*
	 * Kernel providing HaxOS APIs.
	 */
	Kernel KERNEL

	/*
	 * Terminal to write the received data to.
	 */
	Terminal TERMINAL

	/*
	 * Command line arguments.
	 * This is an array that will contain the arguments provided to the script on the command line.
	 */
	String[] ARGS

	/*
	 *
	 */
	void start() {
		TERMINAL.writeln "&w-BannerGrab v1.0 banner grabbing tool&00"
		if (ARGS.length < 2 || !ARGS[1].isInteger()) {
	 		TERMINAL.writeln "&y-Usage: BannerGrab.groovy ADDRESS PORT&00"
			 return
	 	}
		int address = NetworkUtils.stringToAddress(ARGS[0])
		int port = ARGS[1].toInteger()
		Datagram request = KERNEL.newDatagram(address, port)
		SyncChannel channel = null;
		try {
			channel = KERNEL.openSyncPort(this)
			Datagram response = channel.query(request, 1000)
			if (response && response.getData()) {
				String responsePayload = new String(response.getData()).trim()
				TERMINAL.writeln "&w-grabbed banner: &g-${responsePayload}&00"
			} else if (response) {
				TERMINAL.writeln "&w-grabbed banner: &g-EMPTY&00"
			} else {
				TERMINAL.writeln "&w-grabbed banner: &r-NONE&00"
			}
		} finally {
			if (channel) {
				channel.publish(request)
				channel.close()
			}
		}
	}

	/*
	 * The stop method can be left empty as we do not need to clean up any resources.
	 */
	void stop() {
	}
}
