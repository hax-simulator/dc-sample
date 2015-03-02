/**
 * Class 8 - AUTHENTICATE with password in file
 *
 * This is a simple authentication task, quite similar to the one introduced in level2.
 * The main difference, besides being implemented as a class, is that the user credentials are stored in a file on the file system of the machine.
 * The task reads these credentials from the file system, and compares them with the user input.
 *
 * For storing the usernames and passwords, a users file called 'users.txt' was created, located in the same directory as this task.
 * Each line of the file contains data about one single user, starting with his username and ending with his password.
 * The format of each line is thus 'username:realname:MD5(password)'. Passwords in the file are stored as MD5 hashes instead of plain text.
 * Note that the task uses an absolute path to reference the file, thus the file must not be moved elsewhere.
 *
 * For demonstration purposes, three accounts are contained in this file.
 * The user 'admin' has the password '4dm1n', and the user 'guest' has the password 'guest'.
 * The user 'ignorant', a user ignoring the common recommendations on password length and quality, has the password 'zap'.
 * For any other username/password combinations, the task fails.
 * You can of course try to add more users to this file. To calculate the MD5 hash for the desired password, search for an online MD5 calculator on the Internet.
 *
 * The task is a stateful one again, and we use an enum type to represent the states USERNAME and PASSWORD.
 */

/*
 * The mandatory task package.
 */
package sk.hax.software.sample.level4

/*
 * Imported classes used throughout the task implementation.
 */
import sk.hax.system.Task


/*
 * The task class is called 'Authenticate' (fully qualified name is 'sk.hax.software.sample.level4.Authenticate').
 * As usual, the task implements the 'sk.hax.system.Task' interface (note the use of the short name 'Task' thanks to the import we have above).
 * The task contains the mandatory start() and stop() methods, as well as the process() and prompt() helper methods just like in the original level2 script.
 * The process() method is the core of the task, it is the one that contains the file system interaction this task demonstrates.
 */
class Authenticate implements Task {

	/*
	 * We need to define a property for the reference to the kernel we will be using (for terminating the task and file system interactions).
	 * The property is automatically set by HaxOS, so we do not have to worry about providing a value for it.
	 */
	def KERNEL

	/*
	 * We need to define a property for the reference to the terminal we will be writing output to and receiving input from.
	 * The property is automatically set by HaxOS, so we do not have to worry about providing a value for it.
	 */
	def TERMINAL

	/*
	 * The 'RESIDENT' property specification, telling HaxOS not to immediately terminate the started task, but rather keep it loaded and running.
	 */
	boolean RESIDENT = true

	/*
	 * The variable 'handler' of an unspecified type will contain a reference to the process() method (see below), used to process incoming data from the terminal.
	 * This is exactly the same notation as in the original level2 script.
	 */
	def handler = this.&process

	/*
	 * Here we define the states of the task as an 'enum'.
	 * An enum is basically a set of predefined values. We will need the states USERNAME and PASSWORD.
	 * Note that this is just a type declaration, we still need to define the property holding the current task state.
	 */
	enum State {
		USERNAME, PASSWORD
	}

	/*
	 * This is the property that will hold the current task state.
	 * The state will be represented as the previously defined enum type 'State'.
	 * The initial state will be USERNAME. Note that enum values must be prefixed by the enum name.
	 */
	State state = State.USERNAME

	/*
	 * This property is used to store the username specified by the user in the USERNAME state.
	 * The username must be temporarily stored for later use while inquiring the user's password.
	 * The username and password are both used later as part of the authentication process.
	 */
	String username

	/*
	 * The start() method is an equivalent of the main routine of a script.
	 * It is called once upon startup of the task, and used for initialization of the task instance.
	 * In this case, the terminal input handler is registered, and the initial prompt is displayed to the user.
	 */
	void start() {

		/*
		 * The method contained in the 'handler' variable (the process() method) is subscribed to the terminal to handle incoming input.
		 */
		TERMINAL.subscribe(handler)

		/*
		 * We print a simple welcome message, or banner.
		 * This is only displayed upon starting the task, therefore it is not part of the prompt() method (specified below).
		 */
		TERMINAL.writeln "Sample Authentication Server v2.0 (c) 2015 SampleSoft Inc."

		/*
		 * This call produces the initial prompt for the user.
		 * As the initial state of the task is USERNAME, the user is prompted to specify his username.
		 */
		prompt()
	}

	/*
	 * The stop() method is an equivalent of the stop() method of a script.
	 * It is called before the task is terminated, and used for cleanup and freeing of resources occupied by the task instance.
	 * In our case, we have made a subscription to the terminal, which we need to remove.
	 */
	void stop() {

		/*
		 * The method contained in the 'handler' variable (the process() method) is unsubscribed from listening on the terminal for input.
		 */
		TERMINAL.unsubscribe(handler)
	}

	/*
	 * This method is used to process incoming data from the console terminal.
	 * It is the one that was subscribed to the terminal inside the start() method.
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
				 * This is the routine executed in case the current task state is USERNAME.
				 * Basically, we need to store the input in the 'username' variable, and proceed the task to the PASSWORD state.
				 */
				case State.USERNAME:

					/*
					 * Assign the input to the 'username' variable for later use.
					 */
					username = input

					/*
					 * Transfer the task to state PASSWORD, to allow the user to enter his password.
					 */
					state = State.PASSWORD

				/*
				 * Do not forget the 'break' statements at the end of each 'case'.
				 */
				break

				/*
				 * This is the routine executed in case the current task state is PASSWORD.
				 * This is where the username/password combination is validated to be correct.
				 * Based on the result of the validation, an adequate message is displayed to the user.
				 */
				case State.PASSWORD:

					/*
					 * Since we have stored the password in the users file in hashed form rather than plain text, we need a digest object.
					 * A digest object is basically a calculator of hashes, in this case, MD5 hashes.
					 * We will use this object to convert the password value entered by the user to an MD5 hash.
					 * We will then compare this hash with the hash stored in the users file.
					 * If they are equal, the password is correct.
					 */
					def digest = java.security.MessageDigest.getInstance("MD5")

					/*
					 * We calculate the MD5 hash for the user-supplied password.
					 * The result is the MD5 hash of the password stored in the 'passwordHash' variable.
					 * The first line initializes the input of the digest object with the password value, represented as a byte array.
					 * The second line calls the digest() method of the digest object, which performs the MD5 hash calculation.
					 * Since the result of the digest() method is again a byte array, we need to convert the result to the standard representation of MD5 hashes.
					 * The standard representation (also used in the users file) is the string representation of the 16 bytes of the hash in hexadecimal format.
					 * The Groovy method encodeHex() on a byte array achieves exactly the desired output.
					 */
					digest.update(input.bytes)
					String passwordHash = digest.digest().encodeHex()

					/*
					 * Now here comes the interesting part.
					 * Using a kernel API call, we retrieve the contents of the users file.
					 * We use the absolute path of the file to reference it ('/sample/level4/users.txt').
					 * The file contents are stored in the 'users' variable of type string, since this is a text file.
					 */
					String users = KERNEL.readFile("/sample/level4/users.txt")

					/*
					 * To make our task robust, we need to check if the users file is accessible.
					 * To check whether we successfully loaded the file, we check the 'users' variable for being null or empty.
					 * Remember that in Groovy, this is achieved as a simple boolean check.
					 * If the file could not be loaded, we show an error message in red color and terminate the task.
					 */
					if (!users) {
						TERMINAL.writeln "&r-Unable to load users file.\nAuthentication failed.&00"
						KERNEL.stopTask(this)
						return
					}

					/*
					 * We now parse the file contents.
					 * Each user in the file is located on a separate line.
					 * Thus, we split the contents per line into a string array.
					 * Every item of the string array 'usersLines' will contain a single line of the users file.
					 * The split() method of a string object splits the string by the specified regular expression.
					 * The supplied regular expression matches all kinds of line endings.
					 */
					String[] usersLines = users.split("\\r?\\n")

					/*
					 * Next we search the 'usersLines' array for the string item corresponding to the earlier supplied username.
					 * Remember, the format of the lines is 'username:realname:md5_password'. Thus, we need to search for an item which starts with the supplied username.
					 * Luckily, using Groovy methods and closures, this is a rather easy task.
					 * The find() method can be used to search an array or collection for a specific item.
					 * As a parameter, the find() method accepts an arbitrary expression in the form of a closure (an anonymous function, see the Groovy documentation).
					 * It executes this closure for each item of the array. The first item for which the closure returns 'true' is returned as the result of find().
					 * The closure has access to an implicit variable called 'it', which always contains the current item that it is being run against.
					 * So in this case, we check whether the current array item 'it' starts with the supplied username followed by a colon.
					 * If we find the entry for the user, we store it in the 'usersEntry' variable.
					 */
					String usersEntry = usersLines.find { it.startsWith(username+":") }

					/*
					 * We check whether there is an entry for the specified username.
					 * If the find() method above does not find any suitable item, it returns 'null'.
					 * If this was the case, we show an error message in red color and terminate the task.
					 * Note that we again use a boolean expression to check the variable for a value of 'null'.
					 */
					if (!usersEntry) {
						TERMINAL.writeln "&r-Invalid username or password [username not found].\nAuthentication failed.&00"
						KERNEL.stopTask(this)
						return
					}

					/*
					 * Now it's time to check the password hashes and finally authenticate the user.
					 * We have the user-supplied password hash in the 'passwordHash' variable.
					 * So we need to compare this value to the password hash that is part of the 'usersEntry' variable.
					 * We do this simply by checking whether the users file entry ends with the user-supplied password hash, preceded by a colon.
					 * Based on the result of the comparison, we show the relevant success or error message.
					 * As a sidenote, this approach enables us to potentially put more information about the user into the users file, like real name, initial working directory etc.
					 */
					if (usersEntry.endsWith(":"+passwordHash)) {

						/*
						 * Notify the user that he has successfully authenticated. A green message is printed.
						 */
						TERMINAL.writeln "&g-Username and password accepted.\nAuthentication successful.&00"

					} else {

						/*
						 * Notify the user that he has failed to authenticate. A red message is printed.
						 */
						TERMINAL.writeln "&r-Invalid username or password [incorrect password].\nAuthentication failed.&00"
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
			 * If the task state is USERNAME, the user is prompted for his name.
			 * Note that the write() method of the terminal is used, which does not insert a newline at the end.
			 */
			case State.USERNAME:
				TERMINAL.write "username: "
			break

			/*
			 * If the task state is PASSWORD, the user is prompted for his password.
			 * Note that the write() method of the terminal is used, which does not insert a newline at the end.
			 */
			case State.PASSWORD:
				TERMINAL.write "password: "
			break
		}
	}

}
