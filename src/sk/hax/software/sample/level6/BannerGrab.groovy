/**
 * Class 10 - BANNER GRABBER
 *
 * This is a simple task to demonstrate how to implement a synchronous network client task to connect and interact with a remote service across the network.
 * The task can be used for 'banner grabbing', which is a very basic real-world hacking technique to find out information about the services running on open ports of a remote machine.
 * Basically, we connect to a remote port and possibly send some data to it to trigger a response. That response might reveal information about what service we are dealing with, which version etc.
 *
 * Network communication in Hax is much like in the real world. On the lowest level, streams of bytes is what flows through the network.
 * If we have some data in a byte array which we want to send across the network to a remote machine, we have to wrap it in a 'packet'.
 * A packet is constructed simply by prepending our data bytes, commonly called 'payload', with some additional bytes which form the 'header'.
 * The header contains the source and destination network addresses and ports for the data contained in the payload section.
 * The packet consisting of header and payload is the actual array of bytes that travels through the network to its destination.
 * Some aspects of building and decoding a packet are handled internally by the operating system, but some need to be handled by the task developer.
 * The operating system also provides a set of APIs to the developer to make things simpler when implementing tasks for networking.
 *
 * Note that in Hax, connections or sessions (stateful interaction with a remote service) must be implemented on the application level. There is no implicit support for this in the operating system.
 * The implication of this is that every task developer has to create his own mechanism for handling interaction in sessions.
 * There are some recommendations on how to implement this, which we will discuss in more detail in a later sample on network server tasks.
 * However, for the client side of things, it is useful to know that the recommended way to handle session creation and destruction is using null packets. A null packet is simply a packet with an empty payload.
 * On the server side, if a null packet is received from an address and port for which no session exists, a new session is established and persisted for the source address and port of the received null packet.
 * In turn, if a null packet is received from an address and port for which a session already exists, this is interpreted as a request to close the session and the session is dropped.
 * The remote task daemon of HaxOS (rtaskd) works exactly this way, and it is recommended to implement all network servers like this to preserve consistency.
 *
 * This sample task only sends out a null packet to a destination address and port.
 * It then synchronously waits some time to receive a response from the remote side. If a response arrives, it is printed to the console.
 * After that, the task sends out another null packet to the same destination, to close the session possibly established previously.
 * Note that this is a very basic form of banner grabbing, since we are not interacting further with the remote service, we only expect to receive its initial prompt.
 * However, it is sufficient to demonstrate some of the APIs that HaxOS provides for developing networking tasks.
 *
 * We will also use this task to demonstrate the use of command line arguments.
 * When starting a task from the HaxOS shell prompt, the user can provide additional arguments on the command line, separated by spaces.
 * E.g. if this task is started by typing 'BannerGrab.groovy server1 1010', the strings 'server1' and '1010' are the command line arguments.
 * The HaxOS shell automatically sets these arguments into a string array called 'ARGS'. The task is then able to access these arguments and use them as needed.
 * Actually, you can initially try starting this task with the exact same arguments mentioned above. You should get the string 'Hello world' printed to the console.
 * This is because the data center contains another machine besides your own, called 'server1', which has the level0 Hello world script running as a service on network port '1010'.
 *
 * Note: if you do not understand any line of code with no comment on it, that line was certainly explained before, so try looking for it in the previous samples.
 */

package sk.hax.software.sample.level6

/*
 * Note that there are numerous imports here to simplify the code.
 * This includes all the HaxOS APIs and helper classes, as well as additional utility classes.
 */
import sk.hax.network.NetworkUtils
import sk.hax.software.os.Datagram
import sk.hax.software.os.Kernel
import sk.hax.software.os.SyncChannel
import sk.hax.software.os.Terminal
import sk.hax.system.Task


/*
 * The task class is called 'BannerGrab' (fully qualified name is 'sk.hax.software.sample.level6.BannerGrab').
 * It just runs a single routine and finishes (it is not an interactive task) so it merely implements the basic 'sk.hax.system.Task' interface.
 * This means that we have to provide all necessary properties (KERNEL, TERMINAL etc.), as well as implementations of both the start() and stop() methods of the 'sk.hax.system.Task' interface.
 */
class BannerGrab implements Task {

	/*
	 * Kernel providing HaxOS APIs.
	 * These include the APIs we will use for networking.
	 */
	Kernel KERNEL

	/*
	 * Terminal to write the received data to.
	 * Note that we do not need to subscribe a listener to the terminal, as we will not be dealing with user input in this task.
	 */
	Terminal TERMINAL

	/*
	 * Command line arguments.
	 * This is the array that will contain the arguments provided to the script on the command line.
	 * The item at index 0 is the first argument, the item at index 1 the second argument, etc.
	 */
	String[] ARGS

	/*
	 * The start() method containing all the logic of the task.
	 * It does some sanity checks on the command line arguments, tries to establish a connection to the network address and port specified by them, and prints the result of the operation to the terminal.
	 */
	void start() {

		/*
		 * We print the title of this task to the terminal.
		 */
		TERMINAL.writeln "&w-BannerGrab v1.0 banner grabbing tool&00"

		/*
		 * We check the command line arguments.
		 * We need at least two arguments (the first two are actually used), and the second one (with index 1) has to be an integer (this will be the destination port to connect to).
		 * Note the use of the Groovy method isInteger() to check if a string represents a valid integer value.
		 */
		if (ARGS.length < 2 || !ARGS[1].isInteger()) {

			/*
			 * If the arguments are incorrect, we print a usage hint to the terminal and finish the task.
			 */
			TERMINAL.writeln "&y-Usage: BannerGrab.groovy ADDRESS PORT&00"

			/*
			 * Note that we only need to quit the start() method with a simple return statement.
			 * We do not need to finish the task with the usual kernel call, because the task is not resident and thus is automatically terminated by the kernel after the start() method returns.
			 */
			return
	 	}

		/*
		 * Network addresses in Hax in raw form are of type 'int' (integer).
		 * However, the destination address we receive as the first command line argument (with index 0) comes in the form of a string.
		 * Hax provides the utility class 'sk.hax.network.NetworkUtils' to conveniently do network address conversions from and to various formats.
		 * The method stringToAddress() converts a network address from its string representation to an integer.
		 * Furthermore, this method also encapsulates the resolution of domain names. This means that the first command line argument can be either a network address in the usual 'x.x.x.x' format, or a domain name.
		 * If the address on input is incorrect or an unknown domain name is given, the method throws a 'sk.hax.network.UnknownDomainException', the start() method returns and the kernel terminates the task immediately.
		 * The destination network address is stored in the 'address' variable.
		 */
		int address = NetworkUtils.stringToAddress(ARGS[0])

		/*
		 * Network port numbers in Hax are also of type 'int' (integer). However, it is recommended and common practice to only use port numbers from 0 to 65535, to be closer to real world networking.
		 * The destination port number is contained in the second command line argument (with index 1), again as a string. The sanity check above (isInteger() method) ensures it can be converted to an integer without errors.
		 * We can use the Groovy method toInteger() to convert the string to an integer value. We store the value in the 'port' variable.
		 */
		int port = ARGS[1].toInteger()

		/*
		 * To send some data through the network from a HaxOS task, there is no need to build the complete packet structure.
		 * We basically need to provide the destination network address and port, and the data itself (the payload).
		 * HaxOS provides the concept of a 'datagram' represented by the class 'sk.hax.software.os.Datagram' which encapsulates exactly these three pieces of information.
		 * The HaxOS kernel provides several constructor methods with various input parameters to create and initialize new datagram objects.
		 * The newDatagram() method in the form below takes the destination address and port as input and builds a new datagram object initialized properly.
		 * After that, we only need to set the payload and the datagram is ready to be sent out to the network.
		 * Note that in this task, we only send null packets, so we leave the payload uninitialized and thus empty.
		 * Our request datagram is stored in the 'request' variable.
		 */
		Datagram request = KERNEL.newDatagram(address, port)

		/*
		 * We now have our request ready and want to send it to the network. Now here comes the tricky part.
		 * HaxOS provides access to the network interface of the machine through an interface called 'channel'.
		 * A channel is a virtual resource, an interface sitting on top of the physical network interface of the machine.
		 * Each channel is associated with a (local) port number, which must be unique across all channels opened in tasks running on the particular machine.
		 * To use a channel for network communication, it first must be opened, including association with a (local) port number.
		 * After that, the channel can be used to send datagrams to the network to any destination address and port, as well as to receive datagrams from the network for any source address and port.
		 * When the channel is not needed anymore, it must be closed to free up resources of the machine. A well-written task will use Java's try-finally construct for this to make sure the channel is properly closed under all conditions.
		 * HaxOS provides two types of channels, asynchronous and synchronous.
		 * Asynchronous channels are used in server tasks to listen for incoming network communication and reply to it. We will get to this in a later sample.
		 * Synchronous channels are used in client tasks to perform request-reply network communication with a remote service. This type of channel will be used in this sample task.
		 * A synchronous channel is capable of sending some data to the network as a request and wait a specified number of milliseconds for a response to the request.
		 */

		/*
		 * Since we will be using the try-finally construct, we need a variable referencing the channel outside of the construct, to be able to access it in both the 'try' and the 'finally' block.
		 * We create the variable 'channel' and initialize its value to 'null'.
		 */
		SyncChannel channel = null;

		/*
		 * This is the 'try' block of the try-finally construct.
		 * It contains the actual opening of the network channel and the data transmission itself. This code might produce some unexpected exception or error.
		 * If we would not embed the code inside a 'try' block, then, in case of an exception, the start() method would immediately return, and the kernel would terminate the task automatically.
		 * This would result in the channel remaining open, as the code to close the channel would never be executed. There would be no way to close the channel afterwards, except restarting the machine.
		 * To mitigate this issue, we can (and always should) embed the code in a 'try' block, which is followed by a 'finally' block, as seen below.
		 * Now, regardless of whether the code in the 'try' block runs without problem or is interrupted by an exception, the code inside the 'finally' block always executes.
		 * You should already be able to guess that the 'finally' block in this case will contain the code to close the channel.
		 */
		try {

			/*
			 * First thing we do is open the network channel, and assign a reference to it to the 'channel' variable.
			 * Remember, a channel needs to be associated with a (local) port number. Now, there are two possibilities to achieve this.
			 * You can provide a port number explicitly. This method is useful if the port is to be used to initiate network communication, e.g. for server tasks listening to incoming connection requests or data.
			 * Or, you can have the kernel assign a port number to your task automatically. This method is useful if the port is not to be used to initiate communication, e.g. for client tasks calling remote services.
			 * The openSyncPort() method of the kernel in the below form uses the second approach, since it has the current task instance (the implicitly defined variable 'this') on input.
			 * The 'channel' variable will contain a reference to the newly opened synchronous network channel.
			 */
			channel = KERNEL.openSyncPort(this)

			/*
			 * Next, we send our request datagram via the network channel out to the network.
			 * We use the query() method of the channel for this, providing to it the request datagram, and the maximum time we wish to wait for a response in milliseconds (in this case 1 second).
			 * The query() method sends the datagram to the network, then waits for a single incoming datagram. If no datagram arrives back within the specified 1 second, the result is 'null'.
			 * Internally, the request datagram is transformed to a packet and sent through the network interface to the destination address and port.
			 * Likewise, the packet that is received as a response to the request is turned into the response datagram, containing the source address, source port and payload of the received packet.
			 * The response datagram is stored in the 'response' variable.
			 */
			Datagram response = channel.query(request, 1000)

			/*
			 * As was already mentioned, as a proper client, we should close any sessions to remote services we have previously established.
			 * The null packet sent previously to the remote service either established a session with the remote service (if the service is stateful), or just generated a one-time response (if the service is not stateful).
			 * The key fact is that if the former is true, we need to close the session by sending another null packet to the service, and if the latter is true, sending another null packet will have no negative effect as the service will again generate another one-time response.
			 * In conclusion, we can (and should) always send another null packet to a service we have previously sent a null packet to, just to make sure any session we might have established is properly closed.
			 * The publish() method of the channel is used to send datagrams to the network without expecting or needing a response.
			 * On input, we provide the 'request' variable, as it still contains the definition of the null packet with the correct destination address and port.
			 */
			channel.publish(request)

			/*
			 * We proceed with checking the response we received from the network to our previous request.
			 * The first case is the complete success story, when the response is not null and the response data (payload) is not empty.
			 * In this case, we print a success message to the terminal, including the response payload converted to a string.
			 */
			if (response && response.getData()) {
				/*
				 * We extract the payload of the response datagram, and convert it to a string.
				 * The conversion is just standard boilerplate code that has been discussed in previous samples.
				 * The resulting string is stored in the 'responsePayload' string variable, to be able to embed it into the message printed to the terminal.
				 */
				String responsePayload = new String(response.getData()).trim()

				/*
				 * We print the success message to the terminal.
				 * To include the banner we received in the response datagram, we use the GString feature of Groovy, which was already discussed in an earlier sample.
				 */
				TERMINAL.writeln "&w-grabbed banner: &g-${responsePayload}&00"

			/*
			 * The next possible outcome of our request is that we receive a response datagram, but with an empty payload.
			 * Note that here we only check if the response is not null, as we already know that the payload is empty.
			 * If it was not empty, the previous case would be true and we would never get to this one.
			 */
			} else if (response) {
				/*
				 * We print a success message to the terminal, stating that we got a response but with an empty payload.
				 */
				TERMINAL.writeln "&w-grabbed banner: &g-EMPTY&00"

			/*
			 * If none of the previous cases were true, we got a timeout on the query() method and the response is null.
			 */
			} else {
				/*
				 * We print an error message to the terminal, stating that we got no response from the remote service.
				 */
				TERMINAL.writeln "&w-grabbed banner: &r-NONE&00"
			}

		/*
		 * This is the fail-safe 'finally' block of the try-finally construct.
		 * Regardless of the execution of the above 'try' block (regarding unexpected exceptions and errors), this code is always executed.
		 * Inside this block, we need to close the channel we opened previously, to free up resources of the machine.
		 */
		} finally {
			/*
			 * We only need to close the channel if we managed to open it previously.
			 * Remember, we initially assign 'null' to the 'channel' variable, and after that, inside the 'try' block, we actually open the channel.
			 * If the channel cannot be opened, the 'channel' variable will still contain 'null'.
			 * Since we cannot call any methods on a null object, we need to check that 'channel' is not 'null' before we make the call to close it.
			 */
			if (channel) {
				/*
				 * The close() method of a channel effectively closes the network channel and frees up any resources it occupies in HaxOS.
				 */
				channel.close()
			}
		}

		/*
		 * And this is it, the end of the start() method.
		 * After the method returns, the kernel will automatically terminate the task (including execution of the stop() method below, which in this case does nothing).
		 * This is due to the fact the task is not resident (does not have the RESIDENT property set to 'true').
		 */
	}

	/*
	 * The stop method can be left empty, as we do not need to clean up any resources.
	 * Note that despite being empty, we need to explicitly declare it because it is part of the 'sk.hax.system.Task' interface.
	 */
	void stop() {
	}
}
