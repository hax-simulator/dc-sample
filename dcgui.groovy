/**
 * Data center GUI configuration file for the Sample data center.
 *
 * This file is simply just another Groovy script, meaning you can use any programming constructs available in Groovy.
 * Besides standard Groovy code, it uses special, implicitly defined directives to configure the Hax user interface for the data center.
 *
 * You can cutomize the Hax user interface for the data center to some extent.
 * This includes changing the window title, the contents of the Machines menu, and the displayed consoles.
 * More customization options might be added in the future.
 */

/*
 * We want to change the title of the user interface window.
 * This allows for the data center to be easily recognized in the task bar and task list of the user's operating system.
 * The 'title' directive does just that. Just specify the desired name of the window as its parameter.
 */
title "Hax - Sample"

/*
 * The contents of the Machines menu can be specified using one or more 'machine' directives.
 * As a parameter, it takes the name of the machine for which an entry should be created in the menu.
 * This menu allows you to turn the machine on and off.
 * You can also use a single 'machines()' directive to automatically add all machines of the data center to the menu.
 * Specifying included machines individually is helpful when creating e.g. a hacking game environment.
 * In this case, you usually want the user to explore and find some hidden machines based on clues, rather than looking into the menu and finding them right away.
 * Here, we want to display the 'workstation1' machine in the menu, so we can turn it on and off if needed.
 */
machine "workstation1"

/*
 * The available consoles to data center machines can be specified using one or more 'terminal' directives.
 * As a parameter, it takes the name of the access point (not the machine!) of the terminal to create a console for.
 * If the terminal of the machine was configured using the 'terminal()' directive in the data center configuration file, the access point will have the same name as the machine.
 * Alternatively, a single 'terminals()' directive can be used to automatically create consoles for all access points named like the machines of the data center.
 * See the data center configuration file for a description of what access points are.
 * Specifying consoles manually is obviously again helpful when creating e.g. a hacking game environment, with some hidden machines.
 * Here, we want to display the console connected to the 'workstation1' access point, which is in turn linked to the terminal of the 'workstation1' machine.
 */
terminal "workstation1"