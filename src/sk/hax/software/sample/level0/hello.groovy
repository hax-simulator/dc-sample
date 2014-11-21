/**
 * Script 1 - HELLO WORLD
 *
 * This is a simple Hello world program.
 * It outputs the string "Hello world" to the console.
 *
 * Tasks in Hax can be implemented using the Groovy programming language either as scripts or classes.
 * In this case, the program is implemented as a script.
 *
 * Besides basic console output, you can also see the various ways to write comments in your programs here.
 */

// SCRIPT HEADER STARTS HERE

/*
 * Package definition of the script.
 * For scripts, the package definition is not mandatory, but it is good practice to define a package for all your programs.
 * Usually you want to use a common package prefix to easily distinguish your programs.
 */
package sk.hax.software.sample.level0

/*
 * This line is mandatory in all Hax task programs which are implemented as scripts.
 * The exact meaning will be explained later.
 */
@groovy.transform.BaseScript sk.hax.system.impl.GroovyTask TASK

// SCRIPT BODY STARTS HERE

/*
 * This line performs output of the string "Hello world" to the console terminal.
 * Every Hax task has access to a terminal, which is a convenient interface to the keyboard and display of the machine. The terminal is accessed using the TERMINAL property.
 * On the terminal, program calls the 'writeln' (write line) operation, which takes a single argument of type String.
 * The operation simply prints out the specified string followed by a newline on the machine display.
 */
TERMINAL.writeln "Hello world"

/*
 * Here are some examples of alternative syntax for the above statement.
 * Groovy is not as strict as most programming languages when it comes to syntax.
 * Try to uncomment some of these lines and run the script, it should still be functional.
 */
//TERMINAL.writeln("Hello world")
//TERMINAL.writeln "Hello world";
//TERMINAL.writeln("Hello world");
