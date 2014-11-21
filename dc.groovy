/**
 * Data center configuration file for the Sample data center.
 *
 * This file is simply just another Groovy script, meaning you can use any programming constructs available in Groovy.
 * Besides standard Groovy code, it uses special, implicitly defined directives to create the objects of the data center.
 *
 * A data center consists of one or several machines, and the network wires between them.
 * Machines are constructed of various components, like keyboard, display, network interface and storage.
 * Wires are constructed by specifying the network interfaces they connect to each other.
 *
 * The sample data center consists of a single machine.
 * It has a keyboard and display (or terminal), enabling direct login from the Hax user interface.
 * It also has a storage device (hard disk), and a network interface which is currently not used.
 * The machine is running HaxOS as its operating system.
 * The /sample directory on the storage device contains all the sample scripts.
 * The storage device itself is not persistent, so any changes you make to files are discarded when the data center is shut down.
 * For convenience, the HaxOS working directory is changed to /sample after machine startup.
 */

/*
 * Machines are defined using the 'machine' directive.
 * Inside the directive, you can specify all components and settings for the machine using other specific directives.
 * The 'machine' directive is special in that it returns a reference to the newly created machine as its result.
 * This result can then be used to connect the machine's network interface to another machine's network interface with a wire.
 * Here, we assign the reference to the new machine into the 'workstation1' variable, although we do not use the variable further in the configuration file.
 */
def workstation1 = machine {
	/*
	 * Every machine in the data center must have a name.
	 * The name can be any string, but it must be unique for each machine across the the data center.
	 * Using the 'name' directive, the name of this machine is set to 'workstation1'.
	 */
	name "workstation1"

	/*
	 * Every machine must run some firmware.
	 * The firmware is responsible for booting the operating system.
	 * It also provides some low-level functions to the operating system kernel.
	 * Currently Hax only supports a single kind of firmware, the Hax BIOS.
	 * The 'firmware' directive must be specified in every machine definition.
	 * In future, it might be added a parameter to specify the particular firware to use.
	 */
	firmware()

	/*
	 * The 'terminal' directive is a shortcut for several directives.
	 * It creates and adds a keyboard and display to the machine.
	 * It also creates an access point for the keyboard and display, with the same name as the machine name.
	 * This is to support direct interaction with the machine's keyboard and display in the Hax user interface.
	 *
	 * An access point is something like an external connector of the data center. It is always linked to a specific pair of inbound and outbound devices of some machine.
	 * Any data sent to an access point is forwarded to the underlying inbound device of the machine, e.g. a keyboard or network interface.
	 * Likewise, any data the access point receives from the underlying outbound device, e.g. a display or network interface, is published to the outside world.
	 * The Hax user interface uses the access points bound to terminals of data center machines to create consoles for direct interaction with the machine.
	 * The access points bound to network interfaces can in turn be used to interconnect several data center instances using data center links.
	 * Each access point of the data center has a specific, unique name by which it can be referenced.
	 */
	terminal()

	/*
	 * The 'network' directive adds a network interface to the machine.
	 * The machine can have an arbitrary number of network interfaces.
	 * To reference a particular network interface, e.g. when creating a wire, the 0-based index of the network interface is used, based on the order in which it was added to the machine.
	 * Thus, our 'workstation1' machine will now have a single network interface, with index 0.
	 * Note that HaxOS currently does not support multiple network interfaces. So, for workstations, you will not be defining more than one network interface.
	 * Multiple network interfaces are only supported by network machines, like switches, routers and NAT devices.
	 */
	network()

	/*
	 * The 'storage' directive specifies a storage device (hard disk) for the machine.
	 * The storage device stores the operating system and data files of the machine.
	 * The contents of the storage device are specified inside the 'storage' directive using specific directives.
	 * In theory, multiple storage devices can be configured for a machine.
	 * However, there is currently no firmware or operating system in Hax that would support access to multiple storages.
	 */
	storage {
		/*
		 * The first thing needed on a storage is the boot sector, where the operating system of the machine is stored.
		 * Technically, the boot sector is a storage space with a specific name known by the machine firmware.
		 * Upon startup, the firmware checks this sector for the OS kernel task class, and tries to create an instance and start it.
		 * The boot sector is defined using the special 'boot' directive.
		 * As a parameter, the path to either a directory or a JAR file can be used.
		 * In case of a directory, the kernel class name must be included in the path as the fragment part (i.e. separated with a #).
		 * In case of a JAR file, the kernel class must be set as the main class using the JAR manifest.
		 * You do not need to worry about these things if you are not developing an operating system.
		 * The JAR files of HaxOS and network component operating systems are all pre-built correctly.
		 */
		boot "${HAX_HOME}/soft/haxos.jar"

		/*
		 * A data block of the storage can be defined using the 'data' directive.
		 * The most common case when you use a data block is when you want to put a subdirectory tree to your machine's storage as it is.
		 * The directive takes two parameters, a mount point and the path to the content.
		 * A mount point is the virtual directory of the storage where the data block will be put.
		 * The path to the content of the data block may point to a file or a directory.
		 * If the path points to a file, the file with its original filename is added to the mount point as its child.
		 * E.g., if the mount point is '/testdir' and the file is called 'testfile.txt', the storage will contain the file at '/testdir/testfile.txt'.
		 * If the path points to a directory, the directory contents, including all subdirectories, are added to the mount point.
		 * The structure and names of contained files and subdirectories, along with the complete subdirectory tree structure, are all preserved in this case.
		 * Here, we add the sample scripts to the storage on the '/sample' directory. This directory will contain the scripts in the subdirectories 'level0', 'level1' etc.
		 */
		data "/sample", "src/sk/hax/software/sample"

		/*
		 * There are a few directives that can be used to specify content of specific system configuration files.
		 * One of these configuration files for HaxOS is located at '/etc/startup'.
		 * When HaxOS starts up, it reads this file one line at a time and executes each line using the command interpreter (shell).
		 * Thus, you can specify any commands you would normally type on the machine's console by hand, to execute automatically upon machine startup.
		 * The 'startup' directive allows specifying the contents of the '/etc/startup' file in a convenient way.
		 * The individual commands are provided to the directive simply as a comma-separated list of strings.
		 * Here, we switch the initial working directory of the operating system to '/sample', where the sample scripts are located.
		 * After that, we list the contents of the working directory, so the user immediately sees where he can go from there.
		 */
		startup "cd /sample", "ls"
	}
}