/**
 * Script 3 - INTRODUCE
 *
 * This is a simple Introduction program.
 * It asks the user one at a time for his name and age, eventually welcoming him with the collected information.
 *
 * This Hax task is again interactive, as it requires user input. The task is still implemented as a script.
 * The difference to the Greeting task from the previous level is that this task requires the user to input two pieces of information.
 * This is best implemented by introducing states. Tasks with states are commonly called stateful tasks.
 * Each state of a task is an entity defined by the programmer. All allowed states of a task must be defined in advance.
 * When the task is running, it is at all times in one of the allowed states. On each user input, the current state may change to a different one.
 * The task may behave differently depending on the state it is in, e.g. it may process user input differently or show a different prompt to the user.
 *
 * In practice, for this specific task, we will define two states: one for processing the user's name, the other for processing the user's age.
 * The name state displays the prompt to enter the name, stores it in a variable after it has been entered and changes to the age state.
 * The age state displays the prompt to enter the age, and displays the welcome message containing both name and age.
 * For the age, we will also have a validation so that the user can only specify numeric values.
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
 * This is the variable that will hold the current task state.
 * The state will be represented as an integer.
 * For this particular task, states 0 and 1 will be used, with 0 representing the name state and 1 representing the age state.
 * The task state is initialized to be 0 (name), i.e. the task will start by requesting the user to enter his name.
 * The '@groovy.transform.Field' declaration is used so that the variable is accessible from methods contained in the script.
 * Note that when using classes instead of scripts, it is more convenient to use the 'enum' entity for state definition. We will demonstrate this in a later sample.
 */
@groovy.transform.Field int state = 0

/*
 * This variable is used to store the name of the user.
 * The name must be temporarily stored for later use while inquiring the user's age.
 * The name and age are both displayed later as part of the welcome message.
 */
@groovy.transform.Field String name

/*
 * The method contained in the 'handler' variable (the 'process' method) is subscribed to the terminal to handle incoming input.
 */
TERMINAL.subscribe(handler)

/*
 * This call produces the initial prompt for the user.
 * As the initial state of the task is 0 (name), the user is prompted to specify his name.
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
			 * This is the routine executed in case the current task state is 0 (name).
			 * Basically, we need to store the input in the 'name' variable, and proceed the task to state 1 (age).
			 */
			case 0:

				/*
				 * Assign the input to the 'name' variable for later use.
				 */
				name = input

				/*
				 * Transfer the task to state 1 (age), to allow the user to enter his age.
				 */
				state = 1

			/*
			 * Note that each 'case' of a switch must end with a 'break' (unless you know what you are doing).
			 * If you forget the 'break', the code will compile and run, but the result will not be what you expect.
			 */
			break

			/*
			 * This is the routine executed in case the current task state is 1 (age).
			 * The input is checked to be a number, if it is then the welcome message is displayed and the task is terminated.
			 * If the input is not a number, a message is displayed and the task remains running in state 1 (age).
			 */
			case 1:

				/*
				 * It is very simple to check a string to be a number in Groovy.
				 * Note that this check is far from perfect, as it also allows e.g. negative numbers to be entered.
				 */
				if (input.isNumber()) {

					/*
					 * Display the welcome message containing the name and age of the user.
					 * The name is taken from the 'name' variable assigned earlier, the age is taken directly from the input.
					 * A special feature of Groovy called GStrings is used to construct the message string.
					 * This feature allows to use the '${...}' notation inside double-quoted strings to embed arbitrary Groovy code.
					 * Here, the values of the 'name' and 'input' variables, containing the user's name and age respectively, are embedded into the welcome message.
					 */
					TERMINAL.writeln("Let's all welcome ${name}, age ${input}!")

					/*
					 * Finally, the task is stopped and terminated.
					 * The kernel will first call the 'stop' method of this task.
					 * Aferwards, it will remove the task from the list of running tasks.
					 * The kernel API call is immediately followed by the 'return' statement.
					 * This immediately aborts execution of the current method ('process'), resulting in the definite termination of the task.
					 * Generally, you will always want to have the 'stopTask' statement followed by the 'return' statement.
					 */
					KERNEL.stopTask(this)
					return

				/*
				 * This runs only if the age input is not a numeric string.
				 */
				} else {

					/*
					 * Show the user a message that he has specified an invalid age.
					 * The message is again constructed with the value of the 'name' variable embedded as a GString.
					 */
					TERMINAL.writeln("That is not really your age. Don't lie to me ${name}...")

					/*
					 * Note that the state of the task is not changed at this point, and the task remains running.
					 * The current state remains being 1 (age).
					 * Thus, the task keeps on requesting the age again and again until a valid numeric string is specified.
					 * Also note that there is no way of terminating the task without passing the name and age prompts.
					 * This is generally bad practice. We will show an improved stateful task with the ability to immediately exit in another sample.
					 */
				}

			/*
			 * Again, do not forget the 'break' at the end of the 'case'.
			 */
			break
		}
	}

	/*
	 * After processing the input, the user is prompted for some data.
	 * What is requested of the user is determined by the current state of the task.
	 * Note that this is executed regardless of the input being null or empty.
	 * Thus, if the input was null or empty, the task state does not change and the prompt is simply redisplayed.
	 */
	prompt()
}

/*
 * This method is used to display a prompt for some data to the user.
 * The actual prompt depends on the current task state.
 * The method is called when starting the task and each time after processing some user input.
 * Note that the code here might have been integrated directly into the 'process' method (and the task initialization routine).
 * For better readability, however, it is good practice to put the prompt and the input handler into separate methods.
 */
void prompt() {

	/*
	 * A 'switch' statement is used to branch based on the various task states.
	 */
	switch (state) {

		/*
		 * If the task state is 0 (name), the user is prompted for his name.
		 */
		case 0:
			TERMINAL.writeln "What is your name?"
		break

		/*
		 * If the task state is 1 (age), the user is prompted for his age.
		 * Note that the age prompt also contains the value of the 'name' variable, where the previously specified user name is stored.
		 */
		case 1:
			TERMINAL.writeln "How old are you, ${name}?"
		break
	}
}