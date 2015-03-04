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
 * You can see the version of Hax in the NOTICE file in the root directory of Hax.
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


/*
 * The task class 'sk.hax.software.sample.level7.AuthBrute' is defined as an interactive task.
 * The way it will work is that upon startup, it will only do sanity checks on command line arguments and initialize the list of passwords it will try.
 * It will then prompt the user to press the ENTER key to start the password cracking process.
 * After pressing ENTER, the password cracking process is started in the background.
 * The task will report its progress to the terminal as it gradually goes through the password list and tries to authenticate with each password.
 * During this, the user can press the ESC key to immediately cancel the password cracking process and return to the shell.
 * Note that this only applies to Hax version 2.2 and above. In earlier versions, the user interface will be blocked and no user input will be processed until the task finishes.
 * If the task finds the correct password, or goes through the whole password list without finding the correct password, it prints an according message to the terminal and returns to the shell.
 */
class AuthBrute extends InteractiveTask {

	/*
	 * Current working directory provided by the shell.
	 * This will hold the directory from which the task was started.
	 * It can be used to resolve relative paths.
	 */
	String PWD

	/*
	 * This boolean will hold the current state of the task.
	 * If true, this indicates that password cracking is in progress.
	 */
	boolean running = false

	/*
	 * The synchronous network channel used to communicate with the remote authentication service.
	 */
	SyncChannel channel = null

	/*
	 * The list of passwords that the task will try to login with.
	 * It is either loaded from a dictionary file, or generated by the task itself.
	 */
	String[] wordlist = null

	/*
	 * When the password cracking is in progress, we want the task to report its progress, i.e. how many passwords from the word list have already been tried.
	 * In order not to flood the console, we will only report progress after each tenth of the word list has been processed.
	 * This property will hold the number of tries after which a progress report will be issued.
	 * Basically, it will be equal to one tenth of the length of the 'wordlist' array property.
	 */
	int threshold = 1

	/*
	 * We need to do some custom initialization, so we override the start() method of the 'InteractiveTask' base class.
	 */
	void start() {

		/*
		 * Don't forget to call the superclass' start() method.
		 * Remember, this is where the handle() method of this task is subscribed to receive terminal input.
		 */
		super.start()

		/*
		 * We print the title of this task to the terminal.
		 */
		TERMINAL.writeln "&w-AuthBrute v1.0 authentication brute-forcing tool&00"

		/*
		 * We check the command line arguments.
		 * We need at least three arguments (the first three or four are actually used), and the second one (with index 1) has to be an integer (this will be the destination port to connect to).
		 * Note the use of the Groovy method isInteger() to check if a string represents a valid integer value.
		 */
		if (ARGS.length < 3 || !ARGS[1].isInteger()) {

			/*
			 * If the arguments are incorrect, we print a usage hint to the terminal and finish the task.
			 */
			TERMINAL.writeln "&y-Usage: AuthBrute.groovy ADDRESS PORT USERNAME [-brute]&00"

			/*
			 * Note that unlike the previous sample task BannerGrab, this task is an interactive one.
			 * This means it is not sufficient to just return from the start() method, as the HaxOS kernel would keep the task in its task list running.
			 * Thus, we need to also notify the kernel that the task is finished. We do this by the stopTask() method of the kernel, just like in some of the previous samples.
			 */
			KERNEL.stopTask(this)
			return
	 	}

		/*
		 * At this point we decide the mode of operation (dictionary or brute-force) and initialize the password list accordingly.
		 * The task operates in dictionary mode by default. This can be overridden by setting the fourth command line argument to the string '-brute'.
		 * Also, the task degrades to brute-force mode if no dictionary (a file called 'passwords.lst') is found in the directory the task was started from.
		 * If we have less than four command line arguments or the fourth argument (with index 3) is not equal to '-brute', the user has not used the override by the command line argument.
		 * In this case we proceed to load the dictionary file.
		 *
		 */
		if (ARGS.length < 4 || ARGS[3] != "-brute") {

			/*
			 * We use a relative reference to the file to find it, using the kernel API method readFile() with two string parameters.
			 * The first parameter is the relative path and name of the file. Here, we simply use the name as the file.
			 * The second parameter is the absolute path of the directory to resolve the first parameter against. Here, we use the 'PWD' property, which the HaxOS shell initializes to the directory we start the task from.
			 * We store the resulting file content into the 'wordlistFile' variable.
			 */
			def wordlistFile = KERNEL.readFile("passwords.lst", PWD)

			/*
			 * We check whether the dictionary file exists and is actually a text file.
			 * We do this by validating the class of the 'wordlistFile' object to be 'String'.
			 * Note that using the 'instanceof' operator we implicitly check the variable not to be 'null'.
			 * For a null variable, 'instanceof' returns false.
			 */
			if (wordlistFile instanceof String) {

				/*
				 * If we found a dictionary file, we initialize the 'wordlist' property of the task to contain the dictionary.
				 * We use the split() method of the 'String' class with a regular expression just like in previous samples to load the rows of the dictionary file to a string array.
				 */
				wordlist = wordlistFile.split("\\r?\\n")

				/*
				 * We inform the user that we have successfully loaded the dictionary, including the number of passwords that it contains.
				 * We use the GString feature of Groovy to incorporate the length of the 'wordlist' property in the message.
				 */
				TERMINAL.writeln "&w-loaded wordlist with ${wordlist.length} words&00"

			/*
			 * If we get here, this means that either the dictionary file does not exist in the current working directory of the shell, or that it is not a text file.
			 * We only print a warning to the terminal to inform the user that no dictionary file was found.
			 */
			} else {

				/*
				 * If the dictionary file could not be successfully loaded, we print a warning to the user that we will not be running in dictionary mode.
				 * For transparency, we want to include the absolute path where we were looking for the dictionary file.
				 * The kernel API provides the method absoluteFilePath() to calculate the absolute path of a relative file reference and absolute base path.
				 * This method has two string parameters on input, which have the same meaning as in the previously described readFile() method.
				 * The method returns the absolute path to the file referenced by the input parameters as a string.
				 * We again use the GString feature of Groovy to incorporate the absolute path in the warning message.
				 * Note that the GString can contain an arbitrary expression resolving to a string, even an invocation of a class method.
				 */
				TERMINAL.writeln "&w-warning: wordlist not found on path ${KERNEL.absoluteFilePath('passwords.lst', PWD)}&00"
			}
		}

		/*
		 * At this point, we check whether we have already initialized the password list.
		 * If the password list is not null or empty, it means that the user did not explicitly request operating in brute-force mode and we have successfully loaded a dictionary file.
		 * In this case, we are ready to start the password cracking process.
		 * On the other hand, if the password list is null or empty, either the user explicitly wanted to brute-force the password, or no dictionary file could be loaded.
		 * In this case, we need to initialize the password list for brute-force mode.
		 * For this, we will generate all possible words from lower-case letters of length 1, 2 and 3, and store the result in the 'wordlist' property of the task.
		 */
		if (!wordlist) {

			/*
			 * This 'magical' Groovy expression generates our password list.
			 * Basically, we iterate all characters from 'a' to 'z' in three nested loops, building the items of the password list by concatenating the loop control variables.
			 * The expression below is rather advanced Groovy code that uses the collect() method in combination with closures instead of a classic for-loop.
			 * If you do not understand the code, check the Groovy documentation for details.
			 * Also, below you can find the same algorithm implemented in a more traditional way.
			 */
			wordlist =
				("a".."z").collect {x ->
					("a".."z").collect([x]) {y ->
						("a".."z").collect([x+y]) { z ->
							x+y+z
						}
					}
				}.flatten()

			/*
			 * This is an equivalent of the above code using a more traditional approach.
			 * It adds the generated passwords incrementally into the 'wordlist' array using three nested loops.
			 * Note that this code is significantly slower than the Groovy code above.
			 */
//			wordlist = [] 					// initializes the 'wordlist' property to an empty list
//			for (x in "a".."z") { 			// loop through all lowercase letters, with 'x' holding the current letter
//				wordlist += x				// add 'x' to 'wordlist' (using the Groovy '+=' operator)
//				for (y in "a".."z") {		// loop through all lowercase letters, with 'y' holding the current letter
//			    	wordlist += x+y			// add the concatenation of 'x' and 'y' to 'wordlist' (using the Groovy '+=' operator)
//			    	for (z in "a".."z") {	// loop through all lowercase letters, with 'z' holding the current letter
//			    		wordlist += x+y+z	// add the concatenation of 'x', 'y' and 'z' to 'wordlist' (using the Groovy '+=' operator)
//			    	}
//			  	}
//			}

			/*
			 * We inform the user that we will be using a generated password list, including the number of passwords to try.
			 * We use the GString feature of Groovy to incorporate the length of the 'wordlist' property in the message.
			 */
			TERMINAL.writeln "&w-generated wordlist with ${wordlist.length} words&00"
		}

		/*
		 * We can now calculate the threshold to be used for progress notification messages during the password cracking process.
		 * We want to report progress after each tenth of the word list has been processed, therefore the threshold will simply be the password list length divided by 10.
		 */
		threshold = wordlist.length / 10

		/*
		 * We tell the user that we are ready to start the password cracking process, reminding him of the address and port of the remote service, and username we will use.
		 * The user has to press ENTER to start the process. He might also use the ESC key to cancel the task and return to the shell.
		 */
		TERMINAL.writeln "&w-press ENTER to start cracking authentication at ${ARGS[0]}:${ARGS[1]} with username ${ARGS[2]}, ESC to cancel...&00"

		/*
		 * This ends the task initialization procedure.
		 * Note that since the task is interactive, the kernel will not immediately terminate it.
		 * The kernel will leave the task running and waiting for user input.
		 */
	}

	/*
	 * Since we also need some custom cleanup in this task, we need to override also the stop() method of the 'InteractiveTask' base class.
	 */
	void stop() {

		/*
		 * If a network channel has been previously opened, we need to close it to free up operating system resources.
		 * Instead of using the try-finally construct like in the previous sample, we decided to put the close() method call into the stop() method.
		 * This decision is based mainly on the fact that we defined the 'channel' variable as a task property rather than a local variable.
		 * The reason we used a task property instead of a local variable is merely to demonstrate a different approach to cleaning up resources.
		 * This specific task could have been implemented with a local variable for the channel, but there are cases when this is not possible and one has to use the approach shown here.
		 * Another new thing we demonstrate is a Groovy feature that helps to avoid null checks using 'if' statements.
		 * Instead of checking if 'channel' is null using an 'if' statement, we use the Groovy operator '?.'.
		 * This operator guarantees that if 'channel' is null (i.e. no channel was opened in the course of the task), the close() method is not called, thus preventing a null pointer exception that occurs when calling a method on a null variable.
		 * On the other hand, if 'channel' is not null, the close() method is properly called and the network channel is closed as required.
		 */
		channel?.close()

		/*
		 * We also need to call the superclass' stop() method.
		 * Remember, this is where the handle() method of this task is removed from the terminal subscribers to no longer receive input.
		 */
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
