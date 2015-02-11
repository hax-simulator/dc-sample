/**
 * Script 4 - AUTHENTICATE
 *
 * This is a simple Anuthentication program.
 * It asks the user for a username and password, and evaluates whether the input is correct.
 *
 * This is a stateful interactive Hax task to demonstrate how it is possible to handle authentication in your scripts.
 * There are again two states for entering the username and password, respectively.
 * Also, we demonstrate a way to handle a hard-coded password so that it is not visible in the source code.
 *
 * To be able to pass the authentication, however, here are the correct credentials.
 * username: john
 * password: doedoedoe
 */

/*
 * The mandatory task header.
 */
package sk.hax.software.sample.level2
@groovy.transform.BaseScript sk.hax.system.impl.GroovyTask TASK

/*
 * The 'RESIDENT' property specification, telling HaxOS not to immediately terminate the started task, but rather keep it loaded and running.
 */
RESIDENT = true

/*
 * The variable 'handler' of an unspecified type will contain a reference to the 'process' method (see below), used to process incoming data from the terminal.
 * The '@groovy.transform.Field' declaration is used so that the variable is accessible from methods contained in the script (e.g. the 'stop' method).
 */
@groovy.transform.Field def handler = this.&process

/*
 * The variable that will hold the current task state.
 * For this task, states 0 and 1 will be used, with 0 representing the username state and 1 representing the password state.
 * The task state is initialized to be 0 (username), i.e. the task will start by requesting the user to enter his username.
 * Note that when using classes instead of scripts, it is more convenient to use the 'enum' entity for state definition. We will demonstrate this in a later sample.
 */
@groovy.transform.Field int state = 0

/*
 * This variable is used to store the specified username.
 */
@groovy.transform.Field String username

/*
 * The method contained in the 'handler' variable (the 'process' method) is subscribed to the terminal to handle incoming input.
 */
TERMINAL.subscribe(handler)

/*
 * We print a simple welcome message, or banner.
 * This is only displayed upon starting the task, therefore it is not part of the prompt() method (specified below).
 */
TERMINAL.writeln "Sample Authentication Server v1.0 (c) 2014 SampleSoft Inc."

/*
 * This call produces the initial prompt for the user.
 * As the initial state of the task is 0 (username), the user is prompted to specify his username.
 */
prompt()

/*
 * This is the end of the script's main routine.
 */

/*
 * The 'stop' method is called by the kernel when a task is to be terminated.
 * This method must be used to perform cleanup after everything that was changed in the main routine.
 * In our case, we have made a subscription to the terminal, which we need to remove.
 */
void stop() {

	/*
	 * The method contained in the 'handler' variable (the 'process' method) is unsubscribed from listening on the terminal for input.
	 */
	TERMINAL.unsubscribe(handler)
}

/*
 * This method is used to process incoming data from the console terminal.
 * It is the one that was previously subscribed to the terminal.
 */
void process(byte[] data) {

	/*
	 * Incoming data is checked not to be empty.
	 * In Groovy, the boolean value of an array is 'true' if the array is null or empty.
	 */
	if (data) {

		/*
		 * If the received data is not null or empty, it is converted to a string and stored in a local variable.
		 * Additionally, the string is trimmed of leading and trailing whitespace, which is a good practice.
		 * For example, the received array of bytes contains the line ending produced by the Enter key when submitting the user's name to the terminal. Trim will remove this.
		 */
		String input = new String(data).trim()

		/*
		 * This is the heart of the input processing method.
		 * What is done with the received input depends on the current state of the task.
		 * The 'switch' statement is used as a convenient replacement of multiple consecutive 'if' statements.
		 */
		switch (state) {

			/*
			 * This is the routine executed in case the current task state is 0 (username).
			 * Basically, we need to store the input in the 'username' variable, and proceed the task to state 1 (password).
			 */
			case 0:

				/*
				 * Assign the input to the 'username' variable for later use.
				 */
				username = input

				/*
				 * Transfer the task to state 1 (password), to allow the user to enter his password.
				 */
				state = 1

			/*
			 * Do not forget the 'break' statements at the end of each 'case'.
			 */
			break

			/*
			 * This is the routine executed in case the current task state is 1 (password).
			 * This is where the username/password combination is validated to be correct.
			 * Based on the result of the validation, an adequate message is displayed to the user.
			 */
			case 1:

				/*
				 * Since we don't want the credentials to be visible in the source code directly, we will encode them.
				 * We will use MD5 hashes for this. Groovy (and Java) has built-in functionality to work with MD5 hashes.
				 * We first need to create a digest, which acts like a calculator of MD5 hashes.
				 */
				def digest = java.security.MessageDigest.getInstance("MD5")

				/*
				 * We will first create the MD5 hash of the username.
				 * First we feed the username as an array of bytes to the digest.
				 */
				digest.update(username.bytes)

				/*
				 * Then, we convert the calculated hash to a string.
				 * You do not need to worry about how the following line exactly works.
				 * The result is the MD5 hash of the username, stored in the 'usernameHash' variable.
				 * Note that the call to 'digest.digest()' resets the digest so it is ready to calculate another hash.
				 */
				String usernameHash = digest.digest().encodeHex()

				/*
				 * Now we do the same as above for the password.
				 * The result is the MD5 hash of the password stored in the 'passwordHash' variable.
				 */
				digest.update(input.bytes)
				String passwordHash = digest.digest().encodeHex()

				/*
				 * Next, we check whether the hashes are correct.
				 * We have precalculated the MD5 hashes for the correct username and password.
				 * This way, the source code only reveals the hashes, which cannot be converted back to their source values.
				 * So there is no way of figuring out the actual username and password from looking at the source code.
				 */
				if (usernameHash == '527bd5b5d689e2c32ae974c6229ff785' && passwordHash == '8a1880285032d8a989b5034ce64ce437') {

					/*
					 * Notify the user that he has successfully authenticated. A green message is printed.
					 * To display a green message, a DCCC (DragonConsole Color Code) is used.
					 * If you use DCCC, always remember to terminate your string with '&00' to reset the foreground and background colors to the default.
					 * You can find out about DCCCs here: https://github.com/bbuck/DragonConsole/wiki/Color-Codes
					 * Also note the use of the newline character ('\n') to print the message on two lines.
					 */
					TERMINAL.writeln "&g-Username and password accepted.\nAuthentication successful.&00"
				} else {

					/*
					 * Notify the user that he has failed to authenticate. A red message is printed.
					 * To display a red message, a DCCC (DragonConsole Color Code) is used.
					 * If you use DCCC, always remember to terminate your string with '&00' to reset the foreground and background colors to the default.
					 * You can find out about DCCCs here: https://github.com/bbuck/DragonConsole/wiki/Color-Codes
					 * Also note the use of the newline character ('\n') to print the message on two lines.
					 */
					TERMINAL.writeln "&r-Invalid username or password.\nAuthentication failed.&00"
				}


				/*
				 * Notify the kernel to terminate this task, and abort the task execution afterwards.
				 */
				KERNEL.stopTask(this)
				return

			/*
			 * Again, do not forget the 'break' at the end of the 'case'.
			 */
			break
		}

		/*
		 * After processing the input, print out the prompt that is adequate to the current task state.
		 */
		prompt()

	/*
	 * If the data on input is empty, the task is immediately terminated.
	 * Thus, if the user presses ESC during any of the username or password prompts, he is thrown right back into the HaxOS console.
	 * This is a means of supporting exiting the task at any time.
	 */
	} else {

		/*
		 * Notify the kernel to terminate this task, and abort the task execution afterwards.
		 */
		KERNEL.stopTask(this)
		return
	}
}

/*
 * Method for displaying a prompt to the user.
 * The prompt depends on the task state.
 */
void prompt() {

	/*
	 * A 'switch' statement is used to branch based on the various task states.
	 */
	switch (state) {

		/*
		 * If the task state is 0 (username), the user is prompted for his name.
		 * Note that the 'write' method of the terminal is used, which does not insert a newline at the end.
		 */
		case 0:
			TERMINAL.write "username: "
		break

		/*
		 * If the task state is 1 (password), the user is prompted for his password.
		 * Note that the 'write' method of the terminal is used, which does not insert a newline at the end.
		 */
		case 1:
			TERMINAL.write "password: "
		break
	}
}