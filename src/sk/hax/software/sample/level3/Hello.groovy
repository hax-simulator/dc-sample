/**
 * Class 6 - HELLO WORLD
 *
 * This is the Hello World task from level0 implemented as a class.
 * A reimplementation of the script task was chosen to better demonstrate the difference between the two approaches.
 */

/*
 * Package definition of the class.
 * All classes should be contained in a package.
 * Packaging guidelines are the same as for Java.
 */
package sk.hax.software.sample.level3

/*
 * Imported classes used throughout the task implementation.
 * These allow you to use only the names of the classes instead of the fully qualified names.
 * E.g., if there is 'import sk.hax.system.Task', it is sufficient to refer to this class as 'Task'.
 */
import sk.hax.system.Task


/*
 * The task class is called 'Hello' (fully qualified name is 'sk.hax.software.sample.level3.Hello').
 * Every class that wishes to act as a Hax task must implement the 'sk.hax.system.Task' interface (note the use of the short name 'Task' thanks to the import we have above).
 * This interface contains the methods 'start' and 'stop', which need to be contained in every class implementing the interface.
 * The 'start' method is called by the HaxOS upon startup of the task. It contains the code you would put into the main routine of a script.
 * The 'stop' method is called by the HaxOS upon terminating the task. It contains the code you would put into the 'stop' method of a script.
 * Any other methods you would have in a script can be put into the class as methods.
 * Also, any properties defined in a script can be put into the class as properties. Note that you can omit the '@groovy.transform.Field' declaration in classes.
 */
class Hello implements Task {

	/*
	 * We need to define a property for the reference to the terminal we will be writing output to.
	 * The property is automatically set by HaxOS, so we do not have to worry about providing a value for it.
	 */
	def TERMINAL

	/*
	 * The start method is an equivalent of the main routine of a script.
	 * It is called once upon startup of the task, and used for initialization of the task instance.
	 * In this case, it simply outputs the 'Hello world' string to the terminal.
	 */
	void start() {
		TERMINAL.writeln "Hello world"
	}

	/*
	 * The stop method is an equivalent of the 'stop' method of a script.
	 * It is called before the task is terminated, and used for cleanup and freeing of resources occupied by the task instance.
	 * In this case, the method is empty as there is nothing to clean up.
	 */
	void stop() {
	}
}
