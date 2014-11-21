/**
 * Script 7 - INTRODUCE
 *
 * This is the Introduction task from level2, rewritten as a class.
 * A reimplementation of the script task was chosen to better demonstrate the difference between the two approaches.
 *
 * The program demonstrates the implementation of stateful interactive tasks as classes.
 * Also, it shows the use of enum types for states, which is not possible in scripts (since types defined in scripts are global).
 */

/*
 * The mandatory task package.
 */
package sk.hax.software.sample.level3

/*
 * Imported classes used throughout the task implementation.
 */
import sk.hax.system.Task


/*
 * The task class is called 'Introduce' (fully qualified name is 'sk.hax.software.sample.level3.Introduce').
 * Again, the 'sk.hax.system.Task' interface is implemented (note the use of the short name 'Task' thanks to the import we have above).
 * The task contains the mandatory 'start' and 'stop' methods, as well as the 'process' and 'prompt' methods from the original script.
 * Note that the 'process' and 'prompt' methods are mostly copied from the source script unchanged.
 * Again, we can omit the '@groovy.transform.Field' declaration throughout the program.
 */
class Introduce implements Task {

	/*
	 * We need to define a property for the reference to the kernel we will be using (for terminating the task).
	 * The property is automatically set by HaxOS, so we do not have to worry about providing a value for it.
	 */
	def KERNEL

	/*
	 * We need to define a property for the reference to the terminal we will be writing output to.
	 * The property is automatically set by HaxOS, so we do not have to worry about providing a value for it.
	 */
	def TERMINAL

	/*
	 * The 'RESIDENT' property specification, telling HaxOS not to immediately terminate the started task, but rather keep it loaded and running.
	 * In classes, it is simply defined as a property.
	 */
	boolean RESIDENT = true

	/*
	 * The variable 'handler' of an unspecified type will contain a reference to the 'process' method (see below), used to process incoming data from the terminal.
	 * This is exactly the same notation as in the script.
	 */
	def handler = this.&process

	/*
	 * Here we define the states of the task as an 'enum'.
	 * An enum is basically a set of predefined values. We will need the states NAME and AGE.
	 * Note that this is just a type declaration, we still need to define the property holding the current task state.
	 */
	enum State {
		NAME, AGE
	}

	/*
	 * This is the property that will hold the current task state.
	 * The state will be represented as the previously defined enum type 'State'.
	 * The initial state will be NAME. Note that enum values must be prefixed by the enum name.
	 */
	State state = State.NAME

	/*
	 * This property is used to store the name of the user.
	 * The name must be temporarily stored for later use while inquiring the user's age.
	 * The name and age are both displayed later as part of the welcome message.
	 */
	String name

	/*
	 * The start method is an equivalent of the main routine of a script.
	 * It is called once upon startup of the task, and used for initialization of the task instance.
	 * In this case, the terminal input handler is registered, and the initial prompt is displayed to the user.
	 */
	void start() {
		/*
		 * The method contained in the 'handler' variable (the 'process' method) is subscribed to the terminal to handle incoming input.
		 */
		TERMINAL.subscribe(handler)

		/*
		 * This call produces the initial prompt for the user.
		 * As the initial state of the task is 0 (name), the user is prompted to specify his name.
		 */
		prompt()
	}

	/*
	 * The stop method is an equivalent of the 'stop' method of a script.
	 * It is called before the task is terminated, and used for cleanup and freeing of resources occupied by the task instance.
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
	 * Note that this is simply a copy of the 'process' method as implemented in the script.
	 * One difference is the use of enum values for the task state.
	 * The possibility to immediately terminate the task using the ESC key was also added.
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
				 * This is the routine executed in case the current task state is NAME.
				 * Basically, we need to store the input in the 'name' variable, and proceed the task to state AGE.
				 */
				case State.NAME:

					/*
					 * Assign the input to the 'name' variable for later use.
					 */
					name = input

					/*
					 * Transfer the task to state AGE, to allow the user to enter his age.
					 */
					state = State.AGE

				/*
				 * Do not forget the 'break' statements at the end of each 'case'.
				 */
				break

				/*
				 * This is the routine executed in case the current task state is AGE.
				 * The input is checked to be a number, if it is then the welcome message is displayed and the task is terminated.
				 * If the input is not a number, a message is displayed and the task remains running in state AGE.
				 */
				case State.AGE:

					/*
					 * It is very simple to check a string to be a number in Groovy.
					 * Note that this check is far from perfect, as it also allows e.g. negative numbers to be entered.
					 */
					if (input.isNumber()) {

						/*
						 * Display the welcome message containing the name and age of the user.
						 * The name is taken from the 'name' variable assigned earlier, the age is taken directly from the input.
						 * Both are embedded in the output using GStrings.
						 */
						TERMINAL.writeln("Let's all welcome ${name}, age ${input}!")

						/*
						 * Notify the kernel to terminate this task, and abort the task execution afterwards.
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
						 * The current state remains being AGE, and the task keeps on requesting the age again and again until a valid numeric string is specified.
						 */
					}

				/*
				 * Again, do not forget the 'break' at the end of the 'case'.
				 */
				break
			}

		/*
		 * If the data on input is empty (the user presses ESC), the task is immediately terminated.
		 * This is a means of supporting exiting the task at any time.
		 */
		} else {
			KERNEL.stopTask(this)
			return
		}

		/*
		 * After processing the input, the user is prompted for some data.
		 * What is requested of the user is determined by the current state of the task.
		 * Note that this is only executed if the task was not previously terminated (due to the 'return' statements following 'stopTask').
		 */
		prompt()
	}

	/*
	 * This method is used to display a prompt for some data to the user.
	 * The actual prompt depends on the current task state.
	 * Note that this is simply a copy of the 'prompt' method as implemented in the script.
	 * The only difference is the use of enum values for the task state.
	 */
	void prompt() {

		/*
		 * A 'switch' statement is used to branch based on the various task states.
		 */
		switch (state) {

			/*
			 * If the task state is NAME, the user is prompted for his name.
			 */
			case State.NAME:
				TERMINAL.writeln "What is your name?"
			break

			/*
			 * If the task state is AGE, the user is prompted for his age.
			 * Note that the age prompt also contains the value of the 'name' variable, where the previously specified user name is stored.
			 */
			case State.AGE:
				TERMINAL.writeln "How old are you, ${name}?"
			break
		}
	}
}








