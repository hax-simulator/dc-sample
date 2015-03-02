/**
 * Class 11 - AUTH BRUTEFORCER
 *
 * This is another task that demonstrates an implementation of two real-world password cracking techniques, the brute-force attack and the dictionary attack on a remote authentication service.
 * What the task does is basically connect over and over again to a remote service, submit a specific username and password combination and check for the authentication result.
 *
 * The remote network address and port, as well as the username, are taken from the command line arguments. The list of passwords to try is constructed in one of two possible ways.
 *
 * It can either be taken from a text file containing a list of words commonly used as passwords. This is known in real-world hacking as a dictionary attack, and the text file is usually called a wordlist or dictionary.
 * This approach to crack a password is very fast, since the amount of tries is relatively small. Obviously, it does not work against strong passwords which are not in the dictionary.
 *
 * The list of passwords can also be constructed on the fly, programmatically, by trying every possible combination of characters. This is known in real-world hacking as a brute-force attack.
 * Usually, brute-force cracking should be avoided, trying other techniques first (social engineering, guessing, dictionary attacks), because even passwords of as little as 6 characters in length take a huge amount of time to crack.
 * This task only tries passwords of up to 3 lower-case letters in length when in brute-force mode, because the approach shown here only serves demonstration purposes and is far from optimal.
 * The problem is that the complete password list is generated into memory, rather than generating one password at a time. The available memory might not be enough to hold the complete list for greater password lengths.
 *
 * This sample task combines a number of concepts shown in previous samples, such as interactive tasks, file system access, command line arguments, client networking.
 * It introduces the concept of a background task, which executes a possibly long-running operation, and at the same time allowing the user to interact with it.
 * It also shows how to use the working directory of the launching shell for referencing files using relative paths.
 *
 * Note that background tasks only work in Hax version 2.2 and up, so be sure to update to this version if you have not done so yet.
 * In earlier versions, you will not be able to cancel the task until its execution finishes, and the Hax GUI will not respond to input during the task's execution.
 *
 * The machine 'server1' is running the level4 sample authentication server on port 1030, which is the one targeted by this task.
 * You can try to run the task with the command 'AuthBrute.groovy server1 1030 admin'. This will launch a dictionary attack on the service, and the password should be found in the dictionary.
 * You can also try to run the task with the command 'AuthBrute.groovy server1 1030 ignorant -brute'. This will launch a brute-force attack on the service, and the password should be eventually found (since the user has a very weak password).
 *
 * The default mode of operation is dictionary mode, provided that a dictionary file named 'passwords.lst' is found in the directory from which this task is started (not necessarily the same directory where the task file resides!).
 * If the dictionary file is not present, or the string '-brute' is added as the last command line parameter, the task operates in brute-force mode.
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

	int threshold = 1

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
			TERMINAL.writeln "&y-Usage: AuthBrute.groovy ADDRESS PORT USERNAME [-brute]&00"

			KERNEL.stopTask(this)
			return
	 	}

		if (ARGS.length < 4 || ARGS[3] != "-brute") {
			def wordlistFile = KERNEL.readFile("passwords.lst", PWD)
			if (wordlistFile instanceof String) {
				wordlist = wordlistFile.split("\\r?\\n")
				TERMINAL.writeln "&w-loaded wordlist with ${wordlist.length} words&00"
			} else {
				TERMINAL.writeln "&w-warning: wordlist not found on path ${KERNEL.absoluteFilePath('passwords.lst', PWD)}&00"
			}
		}

		if (!wordlist) {
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

		threshold = wordlist.length / 10

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
			} else if (running && index % threshold == 0) {
				TERMINAL.writeln "&w-...already tried ${index} of ${wordlist.length} passwords...&00"
			}
		}
	}
}
