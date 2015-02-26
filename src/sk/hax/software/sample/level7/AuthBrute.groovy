/**
 * Class 11 - AUTH BRUTEFORCER
 *
 */

package sk.hax.software.sample.level7

import sk.hax.Utils
import sk.hax.software.os.Datagram
import sk.hax.software.os.InteractiveTask
import sk.hax.software.os.Kernel
import sk.hax.software.os.SyncChannel
import sk.hax.software.os.Terminal


class AuthBrute extends InteractiveTask {

	/*
	 * Current working directory provided by the shell.
	 */
	String PWD

	boolean running = false

	SyncChannel channel = null

	String[] wordlist = null

	void start() {

		super.start()

		/*
		 * We print the title of this task to the terminal.
		 */
		TERMINAL.writeln "&w-AuthBrute v1.0 authentication brute-forcing tool&00"

		/*
		 * We check the command line arguments.
		 * We need at least three arguments (the first three are actually used), and the second one (with index 1) has to be an integer (this will be the destination port to connect to).
		 * Note the use of the Groovy method isInteger() to check if a string represents a valid integer value.
		 */
		if (ARGS.length < 3 || !ARGS[1].isInteger()) {

			/*
			 * If the arguments are incorrect, we print a usage hint to the terminal and finish the task.
			 */
			TERMINAL.writeln "&y-Usage: AuthBrute.groovy ADDRESS PORT USERNAME&00"

			KERNEL.stopTask(this)
			return
	 	}

		def wordlistFile = KERNEL.readFile("passwords.lst", PWD)
		if (wordlistFile instanceof String) {
			wordlist = wordlistFile.split("\\r?\\n")
			TERMINAL.writeln "&w-loaded wordlist with ${wordlist.length} words&00"
		} else {
			wordlist =
				("a".."z").collect {x ->
					("a".."z").collect([x]) {y ->
						("a".."z").collect([x+y]) { z ->
							x+y+z
						}
					}
				}.flatten()
			TERMINAL.writeln "&w-generated wordlist with ${wordlist.length} words&00"
		}

		TERMINAL.writeln "&w-press ENTER to start brute-forcing authentication at ${ARGS[0]}:${ARGS[1]}, ESC to cancel...&00"
	}

	void stop() {
		channel?.close()
		super.stop()
	}

	void handle(byte[] data) {
		if (!running) {
			if (!data) {
				TERMINAL.writeln "&r-cancelled by user&00"
				KERNEL.stopTask(this)
				return
			}

			brute()

			KERNEL.stopTask(this)
		} else if (!data) {
			running = false
			TERMINAL.writeln "&r-terminated by user&00"
		}
	}

	private void brute() {
		channel = KERNEL.openSyncPort(this)

		Datagram connect = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger())
		Datagram username = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger(), (ARGS[2]+Utils.NEWLINE).getBytes())
		Datagram password = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger())

		Datagram response = null

		running = true
		int index = 0

		TERMINAL.writeln "&w-brute-forcing started, press ESC to terminate...&00"

		while (running) {

			response = channel.query(connect, 1000)
			if (!response?.getData()) {
				running = false
				TERMINAL.writeln "&r-remote service is not responding&00"
				break
			} else if (!toString(response.getData())?.startsWith('Sample Authentication Server')) {
				running = false
				TERMINAL.writeln "&r-remote service is not supported&00"
				break
			}

			response = channel.query(username, 1000)
			if (!response?.getData()) {
				running = false
				TERMINAL.writeln "&r-error sending username&00"
				break
			}

			password.setData((wordlist[index++]+Utils.NEWLINE).getBytes())

			response = channel.query(password, 1000)
			if (toString(response.getData())?.contains("success")) {
				running = false
				TERMINAL.writeln "&g-password found in wordlist: ${wordlist[index - 1]}&00"
			} else if (index == wordlist.length) {
				running = false
				TERMINAL.writeln "&r-password not found in wordlist&00"
			} else if (running && index % 10 == 0) {
				TERMINAL.writeln "&w-...already tried ${index} of ${wordlist.length} passwords...&00"
			}
		}
	}
}
