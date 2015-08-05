/**
 * Class 12 - CHAT SERVER
 *
 * This task class represents a very simple chat server.
 * It keeps a list of connected clients. Every time it receives some data from a client, it just forwards that data to all other registered clients.
 * In effect, this provides a very simple chat system, where multiple users may talk (well, write) to each other. A real life analogy could be a single IRC chatroom.
 *
 * For implementing such a service, the common way of using a simple interactive task and publishing it using remote task daemon (rtaskd) is not sufficient.
 * Rtaskd works so that for each client connection, it creates a new instance of the task. Each of these instances is separate, and the only way they can interact with each other is via the machine's storage (file system).
 * Such an implementation would be rather cumbersome. In this case, it is easier to create a task class that will run on the server machine as a single instance.
 * The task must handle all incoming network communication right at the network level rather than on the terminal level. It must also maintain the list of connected clients as part of its state.
 *
 * The task first initializes the list of connected clients to be empty. Then, it creates a channel which listens on a specific network port for incoming data.
 * There are basically three different cases that can occur when the server receives some data from the network.
 *
 * The first case is when a new client connects to the server. This occurs if the sender of the received data is not contained in the list of connected clients.
 * In this case, the server adds the new client to the list of connected clients, and notifies the other clients that a new client has joined the chat.
 *
 * The second case is when an existing client disconnects from the server. This occurs if the sender of the received data is contained in the list of connected clients, but the received data is empty (this is the standard way to send a disconnect request in Hax).
 * In this case, the server removes the client from the list of connected clients, and notifies the other clients that a client has left the chat.
 *
 * The third case is when an existing client sends a chat message. This occurs if the sender of the received data is contained in the list of connected clients and the received data is not empty.
 * In this case, the server just forwards the received data to all the other clients in the list of connected clients.
 *
 * Real life chat systems usually require a special client application to connect to the chat server. This is due to the fact that special network protocols are used to support a wider variety of features.
 * This sample chat server is very simple and uses basically no protocol at all. Any received data is just simply forwarded to all registered clients.
 * Therefore, the built-in 'telnet' task of HaxOS can be used as a client application for the chat system.
 *
 * To join the chat inside the sample data center, simply use the command 'telnet server1 6666' on both workstation machines.
 * This simulates two concurrent users connected to the chat room and being able to communicate.
 */

package sk.hax.software.sample.level8

import sk.hax.Utils
import sk.hax.software.os.Channel
import sk.hax.software.os.Datagram
import sk.hax.software.os.Kernel
import sk.hax.software.os.Terminal
import sk.hax.system.Task


/*
 * The task class 'sk.hax.software.sample.level8.ChatServer' is defined to implement the most basic 'sk.hax.system.Task' interface.
 * This means that we have to provide all necessary properties (KERNEL, TERMINAL etc.), as well as implementations of both the start() and stop() methods of the 'sk.hax.system.Task' interface.
 */
class ChatServer implements Task {

	/*
	 * Set the task to be resident so that it remains running in memory when started.
	 */
	final boolean RESIDENT = true

	/*
	 * Kernel API providing higher level access to hardware and other low-level APIs.
	 * These include the APIs we will use for networking.
	 */
	Kernel KERNEL

	/*
	 * Terminal to write the received data to.
	 * Note that we do not need to subscribe a listener to the terminal, as we will not be dealing with user input in this task.
	 * Also note that this terminal will be actually be the console with the HaxOS shell command line from which the server was started.
	 * The task uses this terminal to display some messages to the server administrator in the console.
	 */
	Terminal TERMINAL

	/*
	 * Command line arguments.
	 * This is the array that will contain the arguments provided to the script on the command line.
	 * The item at index 0 is the first argument, the item at index 1 the second argument, etc.
	 * This task will require a single command line argument - the network port which the server will be listening on.
	 */
	String[] ARGS

	/*
	 * Channel for listening to incoming network traffic.
	 */
	Channel channel = null

	/*
	 * Set structure containing the currently connected clients.
	 * Each entry in the set will be a pair of network address and port.
	 * If a new client connects to the chat server, a corresponding entry will be added to this set.
	 * If an existing client sends some data to the chat server, the data will be forwarded to all other clients contained in this set.
	 * If an existing client disconnects from the chat server, the corresponding entry will be removed from this set.
	 * We use the Hax utility class 'sk.hax.Utils' to construct the set instance.
	 */
	Set clients = Utils.newSet()

	/*
	 * We will use the start() method of the 'Task' interface to do sanity checks on command line arguments and initialize the task.
	 * This includes opening the network port to listen for incoming data, as well as assigning a handler which will be executed when some data arrives from the network.
	 */
	void start() {

		/*
		 * We print the title of this task to the terminal.
		 */
		TERMINAL.writeln "&w-ChatServer v1.0 sample chat server task&00"

		/*
		 * We check the command line arguments.
		 * This task takes a single numeric command line argument (the port on which the task will listen for incoming communication).
		 * Note the use of the Groovy method isInteger() to check if a string represents a valid integer value.
		 */
		if (ARGS.length < 1 || !ARGS[0].isInteger()) {

			/*
			 * If the arguments are incorrect, we print a usage hint to the terminal and finish the task.
			 */
			TERMINAL.writeln "&y-Usage: ChatServer.groovy PORT&00"

			/*
			 * Note that this task has the RESIDENT property set to true.
			 * This means that it is not sufficient to just return from the start() method, as the HaxOS kernel would keep the task in its task list running.
			 * Thus, we need to also tell the kernel to stop the task. We do this by the stopTask() kernel API method, just like in some of the previous samples.
			 */
			KERNEL.stopTask(this)
			return
		}

		/*
		 * Network port numbers in Hax are of type 'int' (integer). However, it is recommended and common practice to only use port numbers from 0 to 65535, to be closer to real world networking.
		 * The listening port number is contained in the first command line argument (with index 0), in form of a string. The sanity check above (isInteger() method) ensures it can be converted to an integer without errors.
		 * We can use the Groovy method toInteger() to convert the string to an integer value. We store the value in the 'port' variable.
		 */
		int port = ARGS[0].toInteger()

		/*
		 * We use the kernel method openPort() to open an asynchronous network channel.
		 * Since we want to open a channel on the exact port that was specified in the command line arguments, we use the openPort() method form which accepts the port number as an integer on input.
		 * The method returns a 'Channel' object. Note that this is not the same type of channel as in the previous samples, but rather a representation of an asynchronous channel.
		 * Although an asynchronous channel is able to publish datagrams to the network, it is not able to wait synchronously for a response to the transmission.
		 * On the other hand, it is possible to subscribe a handler to the channel, which is notified about all the datagrams that the channel receives, and can execute some routine in response.
		 * The channel object is stored in the 'channel' property of the task.
		 */
		channel = KERNEL.openPort(port)

		/*
		 * The subscribe() method of the channel object is used to assign a handler for received datagrams.
		 * The subscriber is defined here using a Groovy closure (basically an anonymous method).
		 * The closure has a single input parameter called 'datagram' containing the received datagram object.
		 * In its body, the closure simply invokes the receiveData() method of this task class (defined below), with the received datagram as its input.
		 * As a result, every time the channel receives data from the network, the receiveData() method is invoked, with the received data on its input.
		 */
		channel.subscribe {Datagram datagram ->
			receiveData(datagram)
		}

		/*
		 * We inform the user that the task has been successfully initialized and is accepting incoming data.
		 * We use the GString feature of Groovy to incorporate the port number, on which the task is listening, into the message.
		 * We also tell the user that in order to stop the server, the 'kill' command of HaxOS should be used.
		 */
		TERMINAL.writeln "&w-chat server listening for data on port ${port} (use 'kill' command to shut down)&00"

		/*
		 * Note that since we did not subscribe a handler to the TERMINAL object, the console returns to the HaxOS shell command line after executing this start() method.
		 * The server runs completely in the background, and the user can normally carry on with his work in the command line - execute commands, start other tasks etc.
		 */
	}

	/*
	 * We will use the stop() method of the 'Task' interface to do cleanup of resources.
	 * Specifically, we need to close the network channel that we might have previously opened.
	 */
	void stop() {

		/*
		 * If a network channel has been previously opened, we need to close it to free up operating system resources.
		 * Just like in the previous example, we use the Groovy operator '?.' to avoid unnecessary if-null checks.
		 */
		channel?.close()
	}

	/*
	 * This is the handler method that is subscribed to the listening channel to be invoked upon receiving some data from the network.
	 * It first checks whether the incoming datagram's sender is an existing client. If not, the sender is added to the set of clients.
	 * If the sender is an existing client, the method checks whether the received datagram is a null datagram (has an empty payload).
	 * If so, this is treated as a request to disconnect from the chat server, and the sender is removed from the set of clients.
	 * Otherwise, the payload of the received datagram is sent out to all of the clients contained in the set of clients (except the original sender).
	 */
	void receiveData(Datagram datagram) {

		/*
		 * We use the local variable 'key' to store the unique identification of the incoming datagram's sender.
		 * The unique representation is simply a vector consisting of the source network address and port.
		 * In Groovy, we can specify a vector using the simple notation with square brackets. Vectors of arbitrary dimensions can be created this way.
		 * Also, the data type of the resulting vector object is not really important at this point, so we simply use the generic 'def' Groovy directive to define a variable of an arbitrary type.
		 */
		def key = [datagram.getAddress(), datagram.getPort()]

		/*
		 * The local variable 'keyString' will contain the string representation of the unique identification of the incoming datagram's sender.
		 * Note that we use the getAddressString() method of the datagram to retrieve the sender's network address, so the address is in a human-readable format.
		 * In real life chat systems, usually the user name is prepended to the messages sent by that user.
		 * Here, the clients have no user names assigned. Therefore, we use this string to identify them.
		 */
		String keyString = "[${datagram.getAddressString()}:${datagram.getPort()}]"

		/*
		 * We declare the local variable 'payload' as a byte array that will contain the payload of the datagram which will be sent to all connected clients.
		 * Since we want to tag the incoming messages with the identification of the sender, we first need to add this tag to the received data before forwarding it.
		 * We will use this variable to store the resulting data.
		 */
		byte[] payload

		/*
		 * This is where the processing of the incoming datagram starts.
		 * First, we check if the datagram's sender is contained in the set of connected clients.
		 * The set will contain the unique keys of the connected clients (vectors of network address and port).
		 * If the key is not contained in the set, this is a new client that joined the chat.
		 */
		if (!clients.contains(key)) {

			/*
			 * We add the new client's unique key to the set of connected clients.
			 * Now the client is officially connected to the chat server.
			 */
			clients.add(key)

			/*
			 * We want to notify all other clients about the new client joining the chat, so we will send them all a notification message with the new client's identification.
			 * Here, we prepare the payload of the message. The payload will contain the string representation of the new client's unique key (embedded in form of a GString).
			 * Also note that we need to add a newline character at the end of the string, so the message is printed to the clients on a separate line.
			 * We declared the 'payload' variable as a byte array, so we use the Groovy method getBytes() on the constructed message string to convert it to a byte array.
			 */
			payload = "${keyString} : has joined\n".getBytes()

			/*
			 * At this point, we also send the notification message to the new client.
			 * This serves merely as a notification that he managed to successfully join the chat.
			 * This would also be the right place to e.g. send him the list of connected clients, chat topic etc. We omit this for the sake of simplicity of the sample.
			 * The publish() method of the 'channel' property is used to send the message. This method accepts a datagram object, and sends that datagram out on the network.
			 * We use the newDatagram() method of the kernel to construct the datagram to be sent. Input parameters are the destination network address and port, as well as the payload in form of a byte array.
			 * The destination can be taken either from the received datagram itself, or from the 'key' variable which we initialized earlier.
			 * We decided to use the vector contained in the 'key' variable. The first element (with index 0) of the vector is the network address, the second (with index 1) is the network port.
			 */
			channel.publish(KERNEL.newDatagram(key[0], key[1], payload))

		/*
		 *
		 */
		} else if (!datagram.getData()){

			/*
			 *
			 */
			clients.remove(key)

			/*
			 *
			 */
			payload = "${keyString} : has left\n".getBytes()

			/*
			 *
			 */
			channel.publish(KERNEL.newDatagram(key[0], key[1], payload))

		/*
		 *
		 */
		} else {

			/*
			 *
			 */
			ByteArrayOutputStream baos = new ByteArrayOutputStream()

			/*
			 *
			 */
			baos.write((keyString + " : ").getBytes())

			/*
			 *
			 */
			baos.write(datagram.getData())

			/*
			 *
			 */
			payload = baos.toByteArray()
		}

		/*
		 *
		 */
		clients.each {

			/*
			 *
			 */
			if (it != key) {

				/*
				 *
				 */
				channel.publish(KERNEL.newDatagram(it[0], it[1], payload))
			}
		}
	}

	/*
	 * The toString() method is overridden here to improve the way the task is displayed when running the HaxOS 'ps' command.
	 * The 'ps' command uses the output of this method to display the current entries of the operating system task list.
	 * We want the user to see not only the task class name, but also the command line arguments (including the listening port).
	 * Thus, we simply return a string built by concatenating the result of the toString() method of the task's superclass and the 'ARGS' property containing the command line arguments, separated by a space.
	 * Note that a 'return' statement is missing here. This is another Groovy-specific feature. In Groovy, we can omit the 'return' keyword at the end of a method, it is optional.
	 */
	String toString() {
		super.toString() + " " + ARGS
	}

}
