/**
 * Script 2 - GREETING
 *
 * This is a simple Greeting program.
 * It asks the user for his name. After he types it, it greets the user, says bye and terminates.
 * 
 * Hax tasks involving user interaction (like this one) are called interactive tasks.
 * In this case, the task is again implemented as a script, to simplify the understanding of various concepts of the framework.
 * The usual way to write interactive tasks is using classes. HaxOS even offers a special base class to simplify writing of such tasks.
 * We will see a bit later how this task would be properly written. 
 */

/*
 * The mandatory task header.
 * This should be part of any task implemented as a script.
 */
package sk.hax.software.sample.level1
@groovy.transform.BaseScript sk.hax.system.impl.GroovyTask TASK

/*
 * The 'RESIDENT' property is very important and mandatory for interactive and server-side tasks.
 * If set to true, this tells HaxOS not to immediately terminate the started task, but rather keep it loaded and running.
 * Try to comment out this line and see how the task behaves. It will throw you right back into the shell. 		
 */
RESIDENT = true

/*
 * This is a global script variable definition accessible from throughout the whole script, including contained methods.
 * We define the variable 'handler' of an unspecified type to contain a reference to the 'process' method (see below), used to process incoming data from the terminal.
 * To be able to access the variable from methods contained in the script (in this case, the 'stop' method), the '@groovy.transform.Field' declaration must be used.
 * The 'this.&process' construct is the standard way in Groovy for referencing methods when assigning them into variables.
 */
@groovy.transform.Field def handler = this.&process

/*
 * Here we subscribe the method contained in the 'handler' variable (the 'process' method) for handling incoming input from the terminal.
 * From this point on, if any data is received by the terminal (i.e. keyboard input), the subscribed handler will be called to process it.
 * Thus, any received input from the terminal will be processed by our 'process' method (see below). 
 */
TERMINAL.subscribe(handler)

/*
 * We write the question for the user's name to the console terminal.
 */
TERMINAL.writeln "What is your name?"

/*
 * This is the end of the script's main routine.
 * The main routine is called by the HaxOS kernel upon starting a task implemented as a script.
 * When the routine finishes, the HaxOS kernel checks if the 'RESIDENT' property is set to true.
 * If it is not set to true, it immediately terminates the task invoking the 'stop' method and removing it from the list of running tasks.
 * If however it is set to true, it leaves the task running until the task terminates itself (or is killed by the 'kill' shell command).
 * In our case, we have set the 'RESIDENT' property to true, so the task keeps running. 
 * With the 'process' method subscribed to the terminal, the task waits for the user to input his name.
 */

/*
 * The 'stop' method is called by the kernel when a task is to be terminated.
 * This method must be used to perform cleanup after everything that was changed in the main routine.
 * Most commonly it contains code to unsubscribe handlers from terminals, terminate network connections, and similar tasks.
 * In our case, we have made a subscription to the terminal, which we need to remove.
 * If we failed to do so, HaxOS would become unresponsive to the point we would have to restart the machine.
 * Remember, HaxOS is very basic and does not contain any means of protection against this kind of resource leaks.
 * Note that this method must be named  'stop', otherwise it will not be called by the kernel. 
 */
void stop() {	
	
	/*
	 * We want to say bye to our user, and also to be able to notice when the 'stop' method was actually called.
	 * So we print our goodbye to the console terminal.
	 */
	TERMINAL.writeln "Bye!"
	
	/*
	 * Here we unsubscribe the method contained in the 'handler' variable (the 'process' method) from listening on the terminal for input.
	 * This effectively removes the subscription, and our input handler will no longer be called when the terminal receives some input.
	 * Note that this is the place for which we needed the '@groovy.transform.Field' declaration on the 'handler' variable.
	 * As we are currently inside the 'stop' method, we would otherwise not be able to access the variable in this place. 
	 */
	TERMINAL.unsubscribe(handler)	
}

/*
 * This method is used to process incoming data from the console terminal.
 * This is usually the text that the user types using the keyboard and submits pressing Enter.
 * The data is received in form of an array of bytes. So theoretically, it is possible to transfer any kind of data through a terminal, not just text.
 * The method simply prints out the personalized greeting containing the name of the user that arrives as the 'data' input parameter.
 * After that, the task finishes.
 * Note that, unlike the 'stop' method, this method could have been given any name, with the reference to it updated accordingly.
 */
void process(byte[] data) {	
	
	/*
	 * It is good practice to check incoming data for consistency.
	 * In Groovy, the boolean value of an array is true if that array is not null and not empty. This kind of check is very elegant.
	 * So, the 'if' statement simply checks if the data received from the terminal is not empty.
	 */
	if (data) {
		
		/*
		 * If the received data is not null or empty, we print the greeting out to the console terminal.
		 * The greeting is constructed by concatenating string constants along with the user's name received in the 'data' parameter.
		 * For this, the 'data' parameter needs to be converted to a string. Fortunately, in Groovy (and Java) this is quite easy.
		 * Additionally, the resulting string is trimmed of leading and trailing whitespace, also a good practice.
		 * For example, the received array of bytes contains the line ending produced by the Enter key when submitting the user's name to the terminal. Trim will remove this.
		 */
		TERMINAL.writeln("Hello " + new String(data).trim() + "!")
	}
	
	/*
	 * Finally, this line contains the statement to terminate a task running under HaxOS.
	 * The kernel will first call the 'stop' method of the specified task (in this case, the task is 'this', which is this very same script).
	 * Aferwards, it will remove the task from the list of running tasks.
	 * This effectively returns the user back to the HaxOS shell command prompt. 
	 */
	KERNEL.stopTask(this)
}