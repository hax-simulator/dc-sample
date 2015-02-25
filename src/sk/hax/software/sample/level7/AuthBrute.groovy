/**
 * Class 11 - AUTH BRUTEFORCER
 *
 */

package sk.hax.software.sample.level7

import sk.hax.network.NetworkUtils
import sk.hax.software.os.Datagram
import sk.hax.software.os.InteractiveTask
import sk.hax.software.os.Kernel
import sk.hax.software.os.SyncChannel
import sk.hax.software.os.Terminal


class AuthBrute extends InteractiveTask {

	boolean running = false

	void start() {

		super.start()

		/*
		 * We print the title of this task to the terminal.
		 */
		TERMINAL.writeln "&w-AuthBrute v1.0 authentication brute-forcing tool&00"

		/*
		 * We check the command line arguments.
		 * We need at least two arguments (the first two are actually used), and the second one (with index 1) has to be an integer (this will be the destination port to connect to).
		 * Note the use of the Groovy method isInteger() to check if a string represents a valid integer value.
		 */
		if (ARGS.length < 2 || !ARGS[1].isInteger()) {

			/*
			 * If the arguments are incorrect, we print a usage hint to the terminal and finish the task.
			 */
			TERMINAL.writeln "&y-Usage: AuthBrute.groovy ADDRESS PORT&00"

			/*
			 * Note that we only need to quit the start() method with a simple return statement.
			 * We do not need to finish the task with the usual kernel call, because the task is not resident and thus is automatically terminated by the kernel after the start() method returns.
			 */
			return
	 	}
		TERMINAL.writeln "&w-press ENTER to start brute-forcing authentication at ${ARGS[0]}:${ARGS[1]}, ESC to cancel...&00"
	}

	void handle(byte[] data) {
		if (!running) {
			if (!data) {
				TERMINAL.writeln "&r-cancelled by user&00"
				KERNEL.stopTask(this)
				return
			}
			TERMINAL.writeln "&w-brute-forcing started, press ESC to terminate...&00"
			running = true
			while (running) {
				TERMINAL.writeln "&g-running...&00"
				Thread.sleep(1000)
				// TODO
				if (false) {
					running = false
					TERMINAL.writeln "&g-found password: &00"
				}				
			}
			KERNEL.stopTask(this)
		} else if (!data) {
			running = false
			TERMINAL.writeln "&r-terminated by user&00"
		}
	}
}
