/**
 * Class 12 - CHAT SERVER
 *
 */

package sk.hax.software.sample.level8

import sk.hax.Utils
import sk.hax.software.os.Channel
import sk.hax.software.os.Datagram
import sk.hax.software.os.Kernel
import sk.hax.software.os.Terminal
import sk.hax.system.Task

/*
 *
 */
class ChatServer implements Task {

	/*
	 * Set the task to be resident so that it remains running in memory
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
	 */
	Terminal TERMINAL

	/*
	 * Command line arguments.
	 * This is the array that will contain the arguments provided to the script on the command line.
	 * The item at index 0 is the first argument, the item at index 1 the second argument, etc.
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
		 * Network port numbers in Hax are also of type 'int' (integer). However, it is recommended and common practice to only use port numbers from 0 to 65535, to be closer to real world networking.
		 * The listening port number is contained in the first command line argument (with index 0), again as a string. The sanity check above (isInteger() method) ensures it can be converted to an integer without errors.
		 * We can use the Groovy method toInteger() to convert the string to an integer value. We store the value in the 'port' variable.
		 */
		int port = Integer.parseInt(ARGS[0])

		/*
		 * We use the kernel method openPort() to open an asynchronous network channel.
		 * Since we want to open a channel on the exact port that was specified in the command line arguments, we use the openPort() method form which accepts the port number as an integer on input.
		 * The method returns a 'Channel' object. Note that this is not the same type of channel as in the previous samples, but rather a representation of an asynchronous channel.
		 * Although an asynchronous channel is able to publish datagrams to the network, it is not able to wait synchronously for a response to the transmission.
		 * On the other hand, it is possible to subscribe a listener object to the channel, which is notified about all the datagrams that the channel receives, and can execute some routine in response.
		 * The channel object is stored in the 'channel' property of the task.
		 */
		channel = KERNEL.openPort(port)

		/*
		 * The subscribe() method of the channel object is used to assign a handler for received datagrams.
		 * The subscriber is defined here using a Groovy closure (basically an anonymous method).
		 * The closure has a single input parameter called 'datagram' containing the received datagram object.
		 * In its body, the closure simply invokes the receiveData() method of this task class (defined below), with the received datagram as its input.
		 * As a result, every time the channel receives data from the network, the subscribed closure is executed, which in turn just invokes the receiveData() method.
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
	}

	/*
	 * We will use the stop() method of the 'Task' interface to do cleanup of resources.
	 * Specifically, we need to close the network channel that we might have previously opened.
	 */
	void stop() {

		/*
		 * If a network channel has been previously opened, we need to close it to free up operating system resources.
		 * Instead of using the try-finally construct like in the previous sample, we decided to put the close() method call into the stop() method.
		 * This decision is based mainly on the fact that we defined the 'channel' variable as a task property rather than a local variable.
		 * The reason we used a task property instead of a local variable is merely to demonstrate a different approach to cleaning up resources.
		 * This specific task could have been implemented with a local variable for the channel, but there are cases when this is not possible.
		 * Another new thing we demonstrate is a Groovy feature that helps to avoid null checks using 'if' statements.
		 * Instead of checking if 'channel' is null using an 'if' statement, we use the Groovy operator '?.'.
		 * This operator guarantees that if 'channel' is null (i.e. no channel was opened in the course of the task), the close() method is not called,
		 * This way, the null pointer exception that occurs when calling a method on a null variable is prevented.
		 * On the other hand, if 'channel' is not null, the close() method is properly called and the network channel is closed as required.
		 */
		channel?.close()
	}

	/*
	 * This is the handler method that is subscribed to the listening channel to be invoked upon receiving some data from the network.
	 * It first checks whether the incoming datagram's sender is an existing client. If not, the sender is added to the set of clients.
	 * If the sender is an existing client, it checks whether the received datagram is a null datagram (has an empty payload).
	 * If so, this is treated as a request to disconnect from the chat, and the sender is removed from the client set.
	 * Otherwise, the payload of the received datagram is sent out to all of the clients contained in the client set (except the original sender).
	 */
	void receiveData(Datagram datagram) {

		/*
		 *
		 */
		def key = [datagram.getAddress(), datagram.getPort()]

		/*
		 *
		 */
		String keyString = "[${datagram.getAddressString()}:${datagram.getPort()}]"

		/*
		 *
		 */
		byte[] payload

		/*
		 *
		 */
		if (!clients.contains(key)) {

			/*
			 *
			 */
			clients.add(key)

			/*
			 *
			 */
			payload = "${keyString} : has joined\n".getBytes()

			/*
			 *
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
