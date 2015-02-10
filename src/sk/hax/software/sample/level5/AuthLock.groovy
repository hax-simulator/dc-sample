/**
 * Class 9 - AUTHENTICATE with password in file and account locking
 *
 * This class is used to demonstrate writing to the file system.
 * It is quite similar to the previous Authenticate task when it comes to the authentication itself.
 * However, it only allows for 3 attempts to authenticate a specific user. Each successful login clears the attempt counter.
 * If 3 consecutive authentication attempts are unsuccessful, the user is permanently locked out and cannot login anymore.
 *
 * The users file 'users.txt', located in the same directory as this task, has the same format as the one for the Authenticate task. It also contains the same users and passwords.
 * However, an additional data field is added to represent the login attempt counter for each user.
 * For the user lockout, we will simply append the string ':locked' to the end of the line representing the user in the file.
 * This trick removes the need to modify the authentication algorithm, as in this case, the line will never end with the user's password.
 * Despite this, we will add a check for locked accounts to be able to display a different error message to the user.
 *
 * The algorithm is as follows: after typing the username and password, the task loads the users file and checks the username and password just like the Authenticate task.
 * If authentication fails, the task takes the current value of the login attempt counter and increments it. If the counter reaches the value 3, the account will be locked.
 * Then, the task writes the users file contents with the updated login counter and possibly locked status to the file system.
 * If the counter reached the value 3, the task terminates with an error message, otherwise the password prompt is displayed again.
 * Another check the task performs is that if the login name and password of a locked user is entered, the task immediately terminates with an error message.
 *
 * Further, this class demonstrates the use of the HaxOS convenience class InteractiveTask.
 * In the previous examples, you could have noticed that a lot of code kept repeating itself throughout all the tasks implemented as a class.
 * To simplify things a bit, HaxOS includes an abstract class called 'sk.hax.software.os.InteractiveTask', which encapsulates all the common code needed to create an interactive task.
 * This way, the developer can focus on the algorithm itself instead of making sure all the required boilerplate code is in place.
 */

/*
 * The mandatory task package.
 */
package sk.hax.software.sample.level5

/*
 * Imported classes used throughout the task implementation.
 * We only need to import the InteractiveTask class, as this class already implements the required sk.hax.system.Task class.
 */
import sk.hax.software.os.InteractiveTask


/*
 * The task class is called 'AuthLock' (fully qualified name is 'sk.hax.software.sample.level5.AuthLock').
 * Note that the task extends the 'sk.hax.software.os.InteractiveTask' abstract class (note the use of the short name 'InteractiveTask' thanks to the import we have above).
 * The 'InteractiveTask' already contains implementations of all the methods of the 'sk.hax.system.Task' interface.
 * The only required thing is to implement the handle() method, which handles all received user input.
 * Also note that the KERNEL and TERMINAL properties are already defined in the 'InteractiveTask' class, so we do not need to redeclare them and can simply access them.
 * Here, we also override and extend the start() method to display the initial message and prompt to the user.
 */
class AuthLock extends InteractiveTask {

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
	 * We override the start() method to display the initial prompt to the user.
	 */
	@Override
	void start() {

		/*
		 * We first call the superclass' start() method, so that all necessary initialization takes place.
		 * Technically, the start() method of the 'InteractiveTask' class performs the subscription to the terminal we previously needed to implement by hand.
		 * Also note that in turn, the stop() method of the 'InteractiveTask' class removes this subscription.
		 * Since we need no additional functionality inside the stop() method, we don't have to override it and completely omit it from our AuthLock class.
		 */
		super.start()

		/*
		 * We print a simple welcome message, or banner.
		 * This is only displayed upon starting the task, therefore it is not part of the 'prompt' method.
		 */
		TERMINAL.writeln "Sample Authentication Server v3.0 (c) 2015 SampleSoft Inc."

		/*
		 * This call produces the initial prompt for the user.
		 * As the initial state of the task is USERNAME, the user is prompted to specify his username.
		 */
		prompt()
	}

	/*
	 * This method is used to process incoming data from the console terminal.
	 * The method is declared as abstract in the 'InteractiveTask' class, so every subclass must provide an implementation.
	 * Technically, this is the method that the 'InteractiveTask' class subscribes to the terminal in its start() method.
	 */
	@Override
	void handle(byte[] data) {

		/*
		 * If the data on input is empty, the task is immediately terminated.
		 * Thus, if the user presses ESC during any of the username or password prompts, he is thrown right back into the HaxOS console.
		 * This is a means of supporting exiting the task at any time.
		 */
		if (!data) {

			/*
			 * Notify the kernel to terminate this task, and abort the task execution afterwards.
			 */
			KERNEL.stopTask(this)
			return
		}

		/*
		 * To facilitate the conversion of the received byte array to a string, the 'InteractiveTask' class provides the toString() method.
		 * It performs all the trimming and processing that is necessary for the conversion.
		 */
		String input = toString(data)

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
			 * Based on the result of the validation, an adequate message is displayed to the user or operation is performed.
			 */
			case State.PASSWORD:

				/*
				 * We create the digest object used to calculate MD5 hashes.
				 * We use the digest object to calculate the MD5 hash of the user-supplied password.
				 * The MD5 representation of the password is stored in the 'passwordHash' variable.
				 */
				def digest = java.security.MessageDigest.getInstance("MD5")
				digest.update(input.bytes)
				String passwordHash = digest.digest().encodeHex()

				/*
				 * Using a kernel API call, we retrieve the contents of the users file '/sample/level5/users.txt'.
				 * We store the file contents into the 'users' variable.
				 */
				String users = KERNEL.readFile("/sample/level5/users.txt")

				/*
				 * If we are unable to load the users file, we immediately terminate the task with an error message.
				 */
				if (!users) {
					TERMINAL.writeln "&r-Unable to load users file.\nAuthentication failed.&00"
					KERNEL.stopTask(this)
					return
				}

				/*
				 * We split the users file contents into separate lines.
				 * The lines of the users file are contained in the 'usersLines' string array.
				 */
				String[] usersLines = users.split("\\r?\\n")

				/*
				 * We search the 'usersLines' array for the string item corresponding to the earlier supplied username.
				 * If we find the entry for the user, we store its index in the 'index' variable.
				 * Note that since we need to be able to update the users file, we need to write its complete contents.
				 * For this, it is more useful to have the index of the item rather than the item value itself.
				 * The findIndexOf() Groovy method in conjunction with a closure is used to find the correct index.
				 */
				int index = usersLines.findIndexOf { it.startsWith(username+":") }

				/*
				 * We check whether there is an entry for the specified username.
				 * If no entry was found (the findIndexOf() method returned '-1'), we show an error message in red color and terminate the task.
				 */
				if (index == -1) {
					TERMINAL.writeln "&r-Invalid username or password [username not found].\nAuthentication failed.&00"
					KERNEL.stopTask(this)
					return
				}

				/*
				 * We check whether the user is locked.
				 * A locked user has is line in the users file terminated by the string ':locked'.
				 * If we find that the user is locked, we show an error message in red color and terminate the task.
				 */
				if (usersLines[index].endsWith(":locked")) {
					TERMINAL.writeln "&r-Invalid username or password [account is locked].\nAuthentication failed.&00"
					KERNEL.stopTask(this)
					return
				}

				/*
				 * We validate the user-supplied password hash (the 'passwordHash' variable) against the password hash stored in the users file.
				 * We do this simply by checking whether the users file entry ends with the user-supplied password hash, preceded by a colon.
				 * We store the result of the check in the boolean variable 'loggedIn'.
				 */
				boolean loggedIn = usersLines[index].endsWith(":"+passwordHash);

				/*
				 * Now we need to get the value of the login attempt counter to be able to update it.
				 * If the login attempt was successful, we need to reset the counter to 0.
				 * If the login attempt failed, we need to increment the counter and lock the account as required.
				 * The easiest way to obtain the counter value is to split the user entry by the colon character.
				 * The resulting string array 'userEntry' contains the counter value on position 2 (0 is the username, 1 is the real name, 2 is the login attempt counter, 3 is the password, 4 is optionally the locked flag).
				 */
				String[] userEntry = usersLines[index].split(":")

				/*
				 * At this point, we should be checking whether the 'userEntry' variable contains a correct value.
				 * We will not do it in this place, relying on the correctness of the data.
				 * Note that manual editing of the users file may corrupt the data, and result in unpredictable behavior of this task.
				 *
				 */

				/*
				 * If the authentication was successful, we simply set the login attempt counter (located at 'userEntry[2]') to zero.
				 * We then update the users file with the reset counter, show a login success message and finish the task.
				 */
				if (loggedIn) {

					/*
					 * Simply set the counter to 0.
					 */
					userEntry[2] = "0"

					/*
					 * Set the updated user entry to the 'usersLines' variable.
					 * To reconstruct the line corresponding to the user, we use the join() method.
					 * This method concatenates all elements of an array, optionally separated by a separator string, into a single string.
					 * Here we use the colon character as the separator.
					 */
					usersLines[index] = userEntry.join(":")

					/*
					 * At this point, the 'usersLines' variable contains the updated content of the users file, with updated login attempt counter and lock if needed.
					 * We need to store the file back to the file system, to make the changes persistent across multiple authentication calls.
					 * We use the kernel API function writeFile() for this, providing the absolute path and the file contents as its parameters.
					 * Note that we first need to convert the 'usersLines' string array to a single string, with each item separated by a newline character.
					 * To achieve this, we use the join() method we already used earlier, with the separator set to the newline character.
					 * The sk.hax.Utils class contains the constant 'NEWLINE' that we can use as the separator when calling the join() method.
					 */
					KERNEL.writeFile("/sample/level5/users.txt", usersLines.join(sk.hax.Utils.NEWLINE))

					/*
					 * Notify the user that he has successfully authenticated. A green message is printed.
					 */
					TERMINAL.writeln "&g-Username and password accepted.\nAuthentication successful.&00"

					/*
					 * Notify the kernel to terminate this task, and abort the task execution afterwards.
					 */
					KERNEL.stopTask(this)
					return


				/*
				 * If the authentication failed, we increment the counter and lock the account if required.
				 */
				} else {

					/*
					 * Calculate the new value for the counter.
					 * We use the parseInt() method of the 'Integer' class to convert the original counter value to an integer.
					 * Afterwards, we increment the value by 1 and assign the result into the 'newCounter' variable.
					 * Thus, 'newCounter' contains the new value of the login attempt counter.
					 */
					int newCounter = Integer.parseInt(userEntry[2]) + 1

					/*
					 * Set the counter inside the user entry to the new value.
					 * We convert the integer variable 'newCounter' to a string using the valueOf() method of the 'String' class.
					 */
					userEntry[2] = String.valueOf(newCounter)

					/*
					 * Set the updated user entry to the 'usersLines' variable.
					 * To reconstruct the line corresponding to the user, we use the join() method.
					 * This method concatenates all elements of an array, optionally separated by a separator string, into a single string.
					 * Here we use the colon character as the separator.
					 */
					usersLines[index] = userEntry.join(":")

					/*
					 * We need to check whether the account should be locked.
					 * The condition for locking is that there have been 3 or more failed login attempts.
					 */
					if (newCounter >= 3) {

						/*
						 * To lock the user account, we simply append the string ':locked' to the corresponding user line.
						 */
						usersLines[index] += ":locked"
					}

					/*
					 * At this point, the 'usersLines' variable contains the updated content of the users file, with updated login attempt counter and lock if needed.
					 * We need to store the file back to the file system, to make the changes persistent across multiple authentication calls.
					 * We use the kernel API function writeFile() for this, providing the absolute path and the file contents as its parameters.
					 * Note that we first need to convert the 'usersLines' string array to a single string, with each item separated by a newline character.
					 * To achieve this, we use the join() method we already used earlier, with the separator set to the newline character.
					 * The sk.hax.Utils class contains the constant 'NEWLINE' that we can use as the separator when calling the join() method.
					 */
					KERNEL.writeFile("/sample/level5/users.txt", usersLines.join(sk.hax.Utils.NEWLINE))

					/*
					 * Notify the user that he has failed to authenticate. A red message is printed.
					 */
					TERMINAL.writeln "&r-Invalid username or password [incorrect password].&00"

					/*
					 * If there have been 3 or more failed login attempts, we have just locked a user, so we terminate the task.
					 * Otherwise, we keep the task in the PASSWORD state, resulting in another password prompt.
					 */
					if (newCounter >= 3) {

						/*
						 * Notify the user that he has made too many failed login attempts and his account has been locked. A red message is printed.
						 */
						TERMINAL.writeln "&r-Too many failed login attempts. Account was locked.\nAuthentication failed.&00"

						/*
						 * Notify the kernel to terminate this task, and abort the task execution afterwards.
						 */
						KERNEL.stopTask(this)
						return
					}
				}

			/*
			 * Again, do not forget the 'break' at the end of the 'case'.
			 */
			break
		}

		/*
		 * After processing the input, print out the prompt that is adequate to the current task state.
		 */
		prompt()
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
