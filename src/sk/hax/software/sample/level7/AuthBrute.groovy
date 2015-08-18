/**
 * Class 11 - AUTH BRUTEFORCER
 *
 * This is another task that demonstrates the implementation of two real-world password cracking techniques, the brute-force attack and the dictionary attack on a remote authentication service.
 * What the task does is basically connect over and over again to a remote service, submit a specific username and password combination, and check for the authentication result.
 *
 * The remote network address and port, as well as the username, are taken from the command line arguments. The list of passwords to try is constructed one of two possible ways.
 *
 * It can either be taken from a text file containing a list of words commonly used as passwords. This is known in real-world hacking as a dictionary attack, and the text file is usually called a wordlist or dictionary.
 * This approach to crack a password is very fast, since the amount of attempts is relatively small. Obviously, it does not work against strong passwords which are not contained in the dictionary.
 *
 * The list of passwords can also be constructed on the fly, programmatically, by assembling every possible combination of characters. This is known in real-world hacking as a brute-force attack.
 * Usually, brute-force attacks should be avoided, and other techniques should be tried first (social engineering, guessing, dictionary attacks), because even passwords of as little as 6 characters in length take a huge amount of time to crack.
 * When in brute-force mode, this task only tries passwords of up to 3 lower-case letters in length, because the algorithm shown here only serves demonstration purposes and is far from optimal.
 * The problem is that the whole password list is generated into memory, rather than generating one password at a time. The available memory might not be enough to hold the whole list for greater password lengths.
 *
 * This sample task combines a number of concepts shown in previous samples, such as interactive tasks, file system access, command line arguments, client networking.
 * It introduces the concept of a background task, which executes a possibly long-running operation, at the same time allowing the user to interact with it.
 * It also shows how to use the working directory provided by the HaxOS shell to reference files using relative paths.
 *
 * Note that background tasks only work in Hax version 2.2 and up, so be sure to update to such a version if you have not done so yet.
 * In earlier versions, you will not be able to cancel the task until its execution finishes, and the Hax GUI will not respond to input during the task's execution.
 * You can see the version of Hax you are using in the NOTICE file, located in the root directory of Hax.
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
 * Upon startup, it will only do sanity checks on command line arguments and initialize the list of passwords to use in login attempts.
 * It will then prompt the user to press the ENTER key to start the password cracking process. After pressing ENTER, the password cracking process is started.
 * The task will report its progress to the terminal as it gradually goes through the password list and attempts to authenticate with each password.
 * During this, the user can press the ESC key to immediately cancel the password cracking process and return to the shell.
 * Note that this only applies to Hax version 2.2 and above. In earlier versions, the user interface will be blocked and no user input will be processed until the task finishes.
 * If the task finds the correct password, or goes through the whole password list without finding the correct password, it prints an according message to the terminal and returns to the shell.
 */
class AuthBrute extends InteractiveTask {

	/*
	 * Current working directory provided by the HaxOS shell.
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
	 * The list of passwords that the task will attempt to login with.
	 * It is either loaded from a dictionary file, or generated by the task itself.
	 */
	String[] wordlist = null

	/*
	 * When the password cracking process is running, we want the task to report its progress, i.e. how many passwords from the word list have already been tried.
	 * In order not to flood the terminal, we will only report progress after each tenth of the word list has been processed.
	 * This property will hold the number of attempts after which a progress report will be issued.
	 * Basically, it will be equal to one tenth of the length of the 'wordlist' array.
	 */
	int threshold = 1

	/*
	 * We need to do some custom initialization, so we override the start() method of the 'InteractiveTask' base class.
	 */
	void start() {

		/*
		 * Don't forget to call the superclass' start() method.
		 * Remember, this is where the handle() method of this task is subscribed to receive input from the terminal.
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
			 * Note that unlike the previous sample task ('BannerGrab'), this task is an interactive one.
			 * This means that it is not sufficient to just return from the start() method, as the HaxOS kernel would keep the task in its task list running.
			 * Thus, we need to also tell the kernel to stop the task. We do this using the stopTask() kernel API method, just like in some of the previous samples.
			 */
			KERNEL.stopTask(this)
			return
	 	}

		/*
		 * At this point we decide the mode of operation (dictionary or brute-force) and initialize the password list accordingly.
		 * The task operates in dictionary mode by default. This can be overridden by setting the fourth command line argument to the string '-brute'.
		 * Also, the task degrades to brute-force mode if no dictionary (a file called 'passwords.lst') is found in the directory the task was started from (the working directory).
		 * If we have less than four command line arguments or the fourth argument (with index 3) is not equal to '-brute', the user has not used the override by the command line argument.
		 * In this case, we proceed to load the dictionary file.
		 */
		if (ARGS.length < 4 || ARGS[3] != "-brute") {

			/*
			 * We use a relative reference to the file to find it, using the kernel API method readFile() with two string parameters.
			 * The first parameter is the relative path and name of the file. Here, we simply put the name of the file, 'passwords.lst'.
			 * The second parameter is the absolute path of the directory to resolve the first parameter against. Here, we put the 'PWD' property.
			 * The HaxOS shell initializes the 'PWD' property to the directory we started the task from (the working directory).
			 * We store the resulting dictionary file content into the 'wordlistFile' variable.
			 */
			def wordlistFile = KERNEL.readFile("passwords.lst", PWD)

			/*
			 * We check whether the dictionary file exists and is actually a text file.
			 * We do this by validating the class of the 'wordlistFile' object to be 'String'.
			 * Note that using the 'instanceof' operator, we also implicitly check the variable not to be null (i.e. the file does not exist).
			 * For a null variable, 'instanceof' always returns false.
			 */
			if (wordlistFile instanceof String) {

				/*
				 * If we found a dictionary file, we initialize the 'wordlist' property of the task to contain the dictionary.
				 * We use the split() method of the 'String' class with a regular expression just like in previous samples to load the rows of the dictionary file to the 'wordlist' string array.
				 */
				wordlist = wordlistFile.split("\\r?\\n")

				/*
				 * We inform the user that we have successfully loaded the dictionary, including the number of passwords it contains.
				 * We use the GString feature of Groovy to incorporate the length of the 'wordlist' array into the message.
				 */
				TERMINAL.writeln "&w-loaded wordlist with ${wordlist.length} words&00"

			/*
			 * If we get here, this means that either the dictionary file does not exist in the current working directory, or that it is not a text file.
			 * We only print a warning to the terminal to inform the user that no dictionary file was found.
			 */
			} else {

				/*
				 * If the dictionary file could not be successfully loaded, we print a warning to the user that we will not be running in dictionary mode.
				 * For transparency, we want to include the absolute path where we were looking for the dictionary file.
				 * The kernel API provides the method absoluteFilePath() to calculate the absolute path of a relative file reference and absolute base path.
				 * This method has two string parameters on input, which have the same meaning as in the previously described readFile() method.
				 * The method returns the absolute path to the file referenced by the input parameters as a string.
				 * We again use the GString feature of Groovy to incorporate the absolute path into the warning message.
				 * Note that GStrings can contain arbitrary expressions resolving to strings, even the invocation of a method with parameters.
				 */
				TERMINAL.writeln "&w-warning: wordlist not found on path ${KERNEL.absoluteFilePath('passwords.lst', PWD)}&00"
			}
		}

		/*
		 * At this point, we check whether we have already initialized the password list (contained in the 'wordlist' property).
		 * If the password list is not null or empty, it means that the user did not explicitly request operating in brute-force mode and we have successfully loaded a dictionary file.
		 * In this case, we are ready to start the password cracking process.
		 * On the other hand, if the password list is null or empty, either the user explicitly wanted to brute-force the password, or no dictionary file could be loaded.
		 * In this case, we need to initialize the password list for brute-force mode.
		 * For this, we will generate all possible words of lower-case letters of length 1, 2 and 3, and store the result in the 'wordlist' property.
		 */
		if (!wordlist) {

			/*
			 * This magical Groovy expression generates our password list.
			 * Basically, we iterate all characters from 'a' to 'z' in three nested loops, building the items of the password list by concatenating the loop control variables.
			 * The expression is rather advanced Groovy code that uses the collect() method in combination with closures instead of a classic for-loop.
			 * If you do not understand the code, check the Groovy documentation for details. Also, below you can find the same algorithm implemented in a more traditional way.
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
			 * Note that this code is significantly slower than the Groovy code above (feel free to uncomment and try).
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
			 * We inform the user that we will be using a generated password list, including the number of passwords it contains.
			 * We use the GString feature of Groovy to incorporate the length of the 'wordlist' property into the message.
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
		 * Instead, the kernel will leave the task running and waiting for user input.
		 */
	}

	/*
	 * Since we also need some custom cleanup in this task, we need to override the stop() method of the 'InteractiveTask' base class as well.
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

		/*
		 * We also need to call the superclass' stop() method.
		 * Remember, this is where the handle() method of this task is removed from the subscribers of the terminal to no longer receive input.
		 */
		super.stop()
	}

	/*
	 * This is the method that will be subscribed to the terminal to receive and handle user input.
	 * Based on the 'running' property and the input itself, it starts or aborts the password cracking process.
	 * For better clarity, the password cracking process algorithm itself is implemented in a separate method called brute().
	 */
	void handle(byte[] data) {

		/*
		 * We first check whether the password cracking process is running.
		 * If not, we have to decide based on user input whether to start cracking the password or terminate the task.
		 */
		if (!running) {

			/*
			 * If the data received from the terminal is empty, this means the user pressed the ESC key.
			 * In this case, we need to terminate the task without ever starting the password cracking process.
			 */
			if (!data) {

				/*
				 * We print a simple message informing the user that the task was cancelled.
				 */
				TERMINAL.writeln "&r-cancelled by user&00"

				/*
				 * We terminate the task the usual way.
				 * We tell the kernel to stop the task, and finish the execution of this method with a 'return' statement.
				 */
				KERNEL.stopTask(this)
				return
			}

			/*
			 * If the data received from the terminal is not empty, the user pressed the ENTER key.
			 * Thus, we can simply start the password cracking process by calling the brute() method (see below).
			 */
			brute()

			/*
			 * After the brute() method finishes, the password cracking process is complete.
			 * It could have finished with a success or error, or it could have been interrupted by the user.
			 * In any case, we need to finish the task by telling the kernel to stop it.
			 * As this is the last statement of this method, we do not need to issue a 'return' statement (we could, but it would have no effect).
			 */
			KERNEL.stopTask(this)

		/*
		 * If the password cracking process is already running (the 'running' property is true), we again check the user input.
		 * If the data received from the terminal is empty, the user pressed the ESC key, which should interrupt the password cracking process.
		 */
		} else if (!data) {

			/*
			 * To interrupt the password cracking process, we simply need to set the 'running' property to false.
			 * This causes the loop going through the password list and executing the login attempts to stop at the next iteration (see the brute() method below).
			 */
			running = false

			/*
			 * We also notify the user that we have interrupted the process based on his request.
			 */
			TERMINAL.writeln "&r-terminated by user&00"
		}
	}

	/*
	 * This method contains the implementation of the password cracking algorithm.
	 * Notice how the interactions with the remote service and the data that is being sent exactly corresponds to what the targeted remote service (Sample Authentication Server v2.0) expects on input (connect, username, password).
	 * It is very important to understand that while this task works perfectly for the targeted service, it might not work for another service which expects a different order or kind of input.
	 * Because of this, the task also checks the banner of the remote service to make sure it is really targeting the service it is intended for (see below).
	 */
	private void brute() {

		/*
		 * We open a synchronous channel through which the task will be communicating with the remote service.
		 */
		channel = KERNEL.openSyncPort(this)

		/*
		 * We prepare the datagrams the task will be sending out to the remote service.
		 * The first is always the connection request, which is a simple null packet.
		 * Note that we use a new form of the newDatagram() kernel API method here, which accepts the destination network address (first parameter) as a string.
		 * The kernel automatically converts the specified string to an integer representing the address, including resolving the domain name if necessary.
		 * The destination port still has to be an integer, so we use the Groovy method toInteger() to convert from a string.
		 * Remember, the destination address and port are specified using the first two command line arguments, which are always strings.
		 */
		Datagram connect = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger())

		/*
		 * The next datagram we will be sending is the one containing the username we wish to login as.
		 * Again, a new form of the newDatagram() kernel API method is used here, which allows to specify also the payload of the datagram in its third parameter.
		 * The payload is the username specified using the third command line argument (with index 2), followed by a newline character.
		 * Remember, the payload of a datagram must always be provided as a byte array, so we use the getBytes() method of the 'String' class to do the conversion.
		 */
		Datagram username = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger(), (ARGS[2]+Utils.NEWLINE).getBytes())

		/*
		 * The last datagram we will be sending is the one containing the password we are trying to login with.
		 * We initially leave the payload of this datagram empty, and will set it later inside the loop to the passwords from our password list.
		 * Thus, we can use the newDatagram() method in the same form as previously for the connection request datagram.
		 */
		Datagram password = KERNEL.newDatagram(ARGS[0], ARGS[1].toInteger())

		/*
		 * We declare another datagram variable to hold the responses from the remote service.
		 * We initialize it to null.
		 */
		Datagram response = null

		/*
		 * We change the status of the password cracking process to running by setting the appropriate property.
		 */
		running = true

		/*
		 * This variable will contain the index of the password list item we are currently attempting to login with.
		 * The index is 0-based, so we initially set it to the value 0.
		 */
		int index = 0

		/*
		 * We inform the user that we are starting the password cracking process, and that he might terminate it at any time by pressing the ESC key.
		 */
		TERMINAL.writeln "&w-cracking started, press ESC to terminate...&00"

		/*
		 * In this loop, we will repeatedly attempt to authenticate with the remote service using one of the passwords from the password list.
		 * We will use the 'running' property to control the loop. The code block inside the loop will repeat over and over while the 'running' property contains true.
		 * If the property suddenly changes to false (when the password is found, the password list is exhausted or the user presses the ESC key), the loop stops on the next iteration.
		 */
		while (running) {

			/*
			 * We try to connect to the remote service using the null datagram contained in the 'connect' variable.
			 * We use a timeout of 1 second (1000 milliseconds) to wait for a response.
			 * The response datagram is stored in the 'response' variable.
			 */
			response = channel.query(connect, 1000)

			/*
			 * We check the response to our connection request.
			 * If we did not get a response or the response payload is empty, we finish the task immediately.
			 * Note that we again use the '?.' Groovy operator to check for the response being null.
			 * The getData() method of the response datagram object will only be called if the response is not null.
			 * If the response is null, the result of '?.' is also null. In Groovy, null evaluates to false, so we can safely use this construct here.
			 */
			if (!response?.getData()) {

				/*
				 * We set the 'running' property to false to indicate that the cracking process has finished.
				 */
				running = false

				/*
				 * We print an appropriate error message to the terminal.
				 */
				TERMINAL.writeln "&r-remote service is not responding&00"

				/*
				 * The 'break' statement immediately exits the containing while-loop.
				 * After that, the whole brute() method terminates, as there is no more code to execute beyond the loop.
				 */
				break

			/*
			 * Here we check whether the response to our connection request contains a specific banner.
			 * This task only targets the level4 sample task called Sample Authentication Server v2.0.
			 * If the response does not start with the banner of the required service, we finish the task immediately.
			 * Note that we use the toString() convenience method of the 'InteractiveTask' class to convert the response data to a string.
			 * This might result in a null object, so we again use the '?.' operator when calling the startsWith() method to prevent a potential null pointer exception.
			 */
			} else if (!toString(response.getData())?.startsWith('Sample Authentication Server v2.0')) {

				/*
				 * We set the 'running' property to false to indicate that the cracking process has finished.
				 */
				running = false

				/*
				 * We print an appropriate error message to the terminal.
				 */
				TERMINAL.writeln "&r-remote service is not supported&00"

				/*
				 * The 'break' statement immediately exits the containing while-loop.
				 * After that, the whole brute() method terminates, as there is no more code to execute beyond the loop.
				 */
				break
			}

			/*
			 * After we passed the sanity checks of the response to our connection request, we send the username to the remote service.
			 * As input, we use the 'username' variable containing the appropriate datagram and a response timeout of 1 second.
			 * The response datagram is again stored in the 'response' variable.
			 */
			response = channel.query(username, 1000)

			/*
			 * We check the datagram we received as response to sending the username.
			 * If we did not receive any response, or the response data is empty, we finish the task immediately.
			 * The check is done the same way as the check of the response to the connection request (see above).
			 */
			if (!response?.getData()) {

				/*
				 * We set the 'running' property to false to indicate that the cracking process has finished.
				 */
				running = false

				/*
				 * We print an appropriate error message to the terminal.
				 */
				TERMINAL.writeln "&r-error sending username&00"

				/*
				 * The 'break' statement immediately exits the containing while-loop.
				 * After that, the whole brute() method terminates, as there is no more code to execute beyond the loop.
				 */
				break
			}

			/*
			 * Now that we have successfully transmitted the username, it is time to send the password.
			 * First, we need to set up the datagram holding the password. We have a reference to this datagram object in the 'password' variable.
			 * Using the setData() method of the datagram, we set the payload to the index-th item of the 'wordlist' array containing the password list.
			 * We add a newline character to the end of the password and convert the resulting string to a byte array (using the getBytes() method of the 'String' class).
			 * At the same time, we use the '++' operator on the 'index' variable to increment it by one. So, in the next iteration, should it occur, the next password from the list will be taken.
			 */
			password.setData((wordlist[index++]+Utils.NEWLINE).getBytes())

			/*
			 * We send the password datagram contained in the 'password' variable to the remote service, with a response timeout of 1 second.
			 * The response datagram is again stored in the 'response' variable.
			 */
			response = channel.query(password, 1000)

			/*
			 * Now we have completed our authentication attempt, so we review the response we got from the remote service.
			 * We know that the service responds with a message containing the word 'success' if we have sent a valid username/password combination.
			 * So, we check if the reponse datagram's payload contains that word. If it does, we have successfully cracked the password.
			 * Again, note how the use of the '?.' Groovy operator (together with the fact that in Groovy, null evaluates to false) simplifies the check to a single expression, eliminating the need for additional null checks.
			 * Also note that since this is the last statement of the while-loop, it is sufficient to set the 'running' property to false to terminate the loop. There is no need to use the 'break' statement here.
			 */
			if (toString(response?.getData())?.contains("success")) {

				/*
				 * We have found a password, we can terminate the password cracking process by setting the 'running' property to false.
				 */
				running = false

				/*
				 * We print a success message to the terminal, telling the user the correct password that we have discovered.
				 * Note that since we used the '++' operator on the 'index' variable above, the correct password is now the (index-1)-th item of the 'wordlist' array.
				 * Using a GString, we can easily incorporate the correct password into the message.
				 */
				TERMINAL.writeln "&g-password found in wordlist: ${wordlist[index - 1]}&00"

			/*
			 * If we were not successful with the authentication attempt, we need to check whether there are any more passwords left to try.
			 * If the 'index' variable is equal to the length of the 'wordlist' array, we have already tried all the passwords and did not succeed.
			 */
			} else if (index == wordlist.length) {

				/*
				 * We have tried all the passwords from the password list without success, we can terminate the password cracking process by setting the 'running' property to false.
				 */
				running = false

				/*
				 * We print an error message to the terminal, informing the user that we have not succeeded in cracking the password.
				 */
				TERMINAL.writeln "&r-password not found in wordlist&00"

			/*
			 * If we are not through the whole password list yet, we can continue the password cracking process with the next iteration.
			 * But before that, we need to handle the progress notification messages which we want to print to the user, so he knows how far in the process the task currently is.
			 * We will use the mathematical operation modulo (the '%' operator) to check if another progress notification message should be printed.
			 * The below condition will be true each time the 'index' variable is incremented by the number held in the 'threshold' variable.
			 * E.g. if 'threshold' is equal to 10, a message will be printed when 'index' reaches 10, then 20, then 30 etc.
			 * We also include a check of the 'running' property in the condition, so that no message is printed if the password cracking process is already finished.
			 */
			} else if (running && index % threshold == 0) {

				/*
				 * We print a progress message to the terminal.
				 * Using a GString, we incorporate the number of passwords we have already tried, and the length of the whole password list into the message.
				 */
				TERMINAL.writeln "&w-...already tried ${index} of ${wordlist.length} passwords...&00"
			}

			/*
			 * This is the end of the while-loop, and if the 'running' property still contains true, the above code block will repeat once again.
			 * Note that unlike in the previous sample, we are not sending another null datagram to the remote service to finish the session and close the connection.
			 * This is because the remote service we are targeting (Sample Authentication Server) closes the connection after each login attempt, whatever its outcome was.
			 * If this was not the case, we would have to send another null datagram as appropriate, so we don't kill the remote server with too many open connections.
			 * Leaving open connections also allows the remote party to detect us attempting to crack the password, which should also generally be avoided.
			 */
		}
	}
}
