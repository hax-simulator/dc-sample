/**
 * Script 5 - GUESS THE NUMBER
 *
 * This is a simple Guess the Number program.
 * It generates a random number and expects the user to guess it within the specified number of tries.
 *
 * This is yet another stateful interactive Hax task to demonstrate how user interaction can be handled.
 * The states in this program are used a little bit differently than in the previous ones.
 * The state is equal to the number of remaining tries to guess the generated number.
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
 * This task will use states from 0 to 5. The state denotes the number of remaining guesses the user has.
 */
@groovy.transform.Field int state = 5

/*
 * This is the random number generator that will be used for generating the random numbers to guess.
 * Only one generator should be created per task instance and reused when generating any random numbers, therefore it is stored in a variable.
 */
@groovy.transform.Field Random rng = new Random()

/*
 * This variable is used to store the generated number to guess.
 * The number is immediately initialized to a random number between 1 and 50.
 */
@groovy.transform.Field int number = rng.nextInt(50) + 1

/*
 * The method contained in the 'handler' variable (the 'process' method) is subscribed to the terminal to handle incoming input.
 */
TERMINAL.subscribe(handler)

/*
 * We print the initial instruction for the user, specifying the range from which the number is.
 * This is only displayed upon starting the task, therefore it is not part of the 'prompt' method.
 */
TERMINAL.writeln "I'm thinking of a number between 1 and 50..."

/*
 * This call produces the initial prompt for the user.
 * The prompt contains the number of remaining guesses.
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
		 * We declare a variable that will hold the number that the user guessed.
		 * The variable is of type 'Integer' rather than 'int', to be able to assign it 'null' in case the user entered a value that is not a number.
		 * In the assignment, we check if the input is a numeric string. If so, it is converted to a number.
		 * If the input is not numeric, the value 'null' is assigned to the variable.
		 */
		Integer inputNumber = input.isNumber() ?  Integer.valueOf(input) : null

		/*
		 * If the 'inputNumber' variable is 'null', the user specified a non-numeric string, so we show him an according message.
		 */
		if (inputNumber == null) {
			TERMINAL.writeln "Please specify a number."

		/*
		 * If the 'inputNumber' variable holds a value that is not in the range from 1 to 50, we show the user an according message.
		 */
		} else if (!(1..50).contains(inputNumber)) {
			TERMINAL.writeln "I said I'm thinking of a number from 1 to 50, so please try something from that range..."

		/*
		 * If the user guessed the correct number, we congratulate him.
		 * After that, the task is terminated.
		 */
		} else if (inputNumber == number) {
			TERMINAL.writeln "&g-CORRECT!&00 I was thinking of the number ${number}. Congratulations!"
			KERNEL.stopTask(this)
			return

		/*
		 * Otherwise, the user guess was not correct, so we need to process it accordingly.
		 */
		} else {

			/*
			 * The state (remaining number of guesses) is decreased by 1.
			 * The '--' operator is used for that.
			 */
			state--

			/*
			 * If no more guesses remain, we inform the user that he failed and tell him the number we were thinking of.
			 * After that, the task is terminated.
			 */
			if (state == 0) {
				TERMINAL.writeln "&r-FAILED!&00 I was thinking of the number ${number}. You failed to guess it. Pity!"
				KERNEL.stopTask(this)
				return
			}

			/*
			 * We show a message to the user depending on whether the number on input is greater or less than the number to guess.
			 */
			if (inputNumber < number) {
				TERMINAL.writeln "No, I am thinking of a larger number. Try again."
			} else {
				TERMINAL.writeln "No, I am thinking of a smaller number. Try again."
			}
		}

		/*
		 * After processing the input, print out the prompt containing the number of remaining guesses.
		 */
		prompt()

	/*
	 * If the data on input is empty (the user presses ESC), the task is immediately terminated.
	 * This is a means of supporting exiting the task at any time.
	 */
	} else {
		KERNEL.stopTask(this)
		return
	}
}

/*
 * Method for displaying a prompt to the user.
 * The prompt contains the number of remaining guesses the user has to find out the correct number.
 */
void prompt() {

	/*
	 * Show the user the prompt to guess the number.
	 * Note that the state containing the remaining number of guesses is embedded into the prompt as a GString.
	 */
	TERMINAL.write "What number am I thinking of? (remaining tries: ${state}): "
}