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
 * The sample data center consists of a workstation, a server, a secondary workstation, and a switch to interconnect all the nodes.
 * It occupies the network address space of 192.168.1.0/24, meaning the machines can have addresses in the range between 192.168.1.1 and 192.168.1.254.
 *
 * The workstation is the machine you as the user directly interact with, as if you had physical access to it.
 * It has a keyboard and display (or terminal), enabling direct login from the Hax graphical user interface.
 * It also has a storage device (hard disk), and a network interface.
 * The machine is running HaxOS as its operating system.
 * The /sample directory on the storage device contains all the sample scripts.
 * The storage device itself is not persistent, so any changes you make to files are discarded when the data center is shut down.
 * For convenience, the HaxOS working directory is changed to /sample after machine startup.
 * The workstation has the network address 192.168.1.10.
 *
 * The secondary workstation is basically a copy of the primary workstation, with a changed name and network address (192.168.1.11).
 * It is used for testing higher level samples about advanced networking, which require multiple client connections.
 *
 * The server is a machine that is hidden from the user running the data center. It does not have a terminal to interact with.
 * It runs a couple of network services on various ports. It is used for testing samples about networking.
 * Its network address is 192.168.1.50, but it can also be accessed by its domain name (server1).
 */

/*
 * Machines are defined using the 'machine' directive.
 * Inside the directive, you can specify all components and settings for the machine using other specific directives.
 * The 'machine' directive is special in that it returns a reference to the newly created machine as its result.
 * This result can then be used to connect the machine's network interface to another machine's network interface with a wire.
 * Here, we assign the reference to the new machine into the 'workstation1' variable, to be able to connect it later to our switch.
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

		/*
		 * Another directive to specify a system configuration file is 'network'.
		 * This directive defines the contents of the '/etc/network' configuration file, used by operating systems to configure networking.
		 * For HaxOS, the file only needs to contain the network address of the machine, which in this case is 192.168.1.10.
		 */
		network "192.168.1.10"
	}
}

/*
 * This machine is the secondary workstation of the data center.
 * It is used for testing higher level samples about advanced networking, which require multiple clients.
 * The machine is an exact copy of the 'workstation1' machine, except for the name and the network address.
 * We assign the reference to this machine into the 'workstation2' variable, to be able to connect it later to our switch.
 */
def workstation2 = machine {
	name "workstation2"
	firmware()
	terminal()
	network()
	storage {
		boot "${HAX_HOME}/soft/haxos.jar"
		data "/sample", "src/sk/hax/software/sample"
		startup "cd /sample", "ls"
		network "192.168.1.11"
	}
}

/*
 * Here is the definition of the server machine.
 * The server is also running HaxOS as its operating system.
 * However, we don't need to have a terminal for it, since all running services can be started using the HaxOS startup configuration.
 * When connecting to the server, it can be referenced either by its network address, or by its domain name (equal to its name 'server1').
 * The reference to the server machine is stored in the 'server1' variable, so that we can later easily connect it to our switch.
 */
def server1 = machine {
	/*
	 * The server will be called server1.
	 */
	name "server1"

	/*
	 * Don't forget the 'firmware' directive.
	 */
	firmware()

	/*
	 * Like the workstation, the server will also have only a single network interface, since it is running HaxOS.
	 * We will later connect this network interface to one of the network interfaces of our switch.
	 */
	network()

	/*
	 * The server will have a storage device hosting the operating system and data.
	 */
	storage {
		/*
		 * The operating system will again be HaxOS.
		 */
		boot "${HAX_HOME}/soft/haxos.jar"

		/*
		 * We add all the sample scripts to the '/sample' directory, just like on the workstation.
		 * We will want to run some of the sample scripts as services published on the network, that's why we need to have them on the storage.
		 */
		data "/sample", "src/sk/hax/software/sample"

		/*
		 * We publish three of the sample scripts as network services on various ports.
		 * As you can remember from the Hax tutorial on networking, this is very simple using the remote task daemon (rtaskd) of HaxOS.
		 * We only need to specify the network port number and the task that should be bound to it.
		 * We start the 'Hello World' task on port 1010, the 'Introduce' task on port 1020, and the 'Authenticate' task on port 1030.
		 * We will connect to these network services from the client networking sample tasks.
		 */
		startup "rtaskd 1010 /sample/level0/hello.groovy", "rtaskd 1020 /sample/level3/Introduce.groovy", "rtaskd 1030 /sample/level4/Authenticate.groovy", "/sample/level8/ChatServer.groovy 6666"

		/*
		 * We set the server's network address to 192.168.1.50.
		 */
		network "192.168.1.50"
	}

	/*
	 * The 'domain' directive adds the server's name to the list of published domain names.
	 * This allows to transparently use the server's name instead of the server's network address for the purpose of networking.
	 * E.g. on the user level, instead of 'telnet 192.168.1.50 23', one can use 'telnet server1 23'. On the developer level, Hax provides a simple API which encapsulates domain-to-address resolution.
	 * Important: the 'domain' directive MUST always be used after specifying the '/etc/network' configuration file. Best practice is to put it at the end of the machine configuration.
	 */
	domain()
}

/*
 * The definition of the switch to interconnect the machines of the data center.
 * The switch has four network interfaces. More could be added if required.
 * The switch reference is stored in the 'switch1' variable so that we can later easily attach wires to its network interfaces.
 */
def switch1 = machine {
	/*
	 * The switch will be called 'switch1'.
	 */
	name "switch1"

	/*
	 * Don't forget the 'firmware' directive.
	 */
	firmware()

	/*
	 * Five network interfaces are added to the switch.
	 * The first one is the default interface to be connected to the outside network. Since we have no outside network here, it will remain disconnected.
	 * The second one will be connected to the workstation.
	 * The third one will be connected to the server.
	 * The fourth one will be connected to the secondary workstation.
	 * The fifth is reserved for future use and will currently remain disconnected.
	 */
	network()
	network()
	network()
	network()

	/*
	 * The storage device of a switch contains the switch operating system and the network configuration.
	 */
	storage {
		/*
		 * Upon startup, the machine boots the switch operating system.
		 */
		boot "${HAX_HOME}/soft/haxswitch.jar"

		/*
		 * Note that again, the '/etc/network' file is used by the switch operating system for network configuration.
		 * For a switch, the file must contain the definition of the local subnet, either in the form of the base address and mask, or in CIDR notation.
		 * As you might remember, the switch uses this information in conjunction with the destination address of incoming data to determine whether to forward the data to the default network interface (the first one), or one of the internal ones.
		 * For more information, see also the Hax tutorial on networking.
		 * As mentioned earlier, the local subnet of the sample data center is 192.168.1.0/24 (using CIDR notation). This is what we set the network configuration to.
		 */
		network "192.168.1.0/24"
	}
}

/*
 * Now it's time to wire up the machines of our data center.
 * Wiring is done using the 'wire' directive. This directive has several forms of syntax for convenience.
 * At the end of the day, we basically need to specify a pair of network interfaces to be connected by the wire.
 * A network interface is specified by the machine that contains it, and its (0-based) index among all network interfaces of that machine.
 * E.g. machines running HaxOS will have only a single network interface, with an index of 0. The switch above has network interfaces 0, 1, 2 and 3.
 * For convenience, the 'wire' directive supports omitting the network interface index when specifying network interface 0 of a machine.
 *
 * The following directive connects network interface 1 (the second one) of the switch (referenced by variable 'switch1') to network interface 0 (the only one) of the workstation (referenced by variable 'workstation1').
 * Thus, we have our workstation connected to the switch, and they are able to exchange data through the network.
 * Note that we omit specifying the index 0 for the workstation network interface.
 */
wire switch1, 1, workstation1

/*
 * The following directive connects network interface 2 (the third one) of the switch (referenced by variable 'switch1') to network interface 0 (the only one) of the server (referenced by variable 'server1').
 * Thus, we have our server connected to the switch, and they are able to exchange data through the network.
 * Note that we omit specifying the index 0 for the server network interface.
 *
 * Furthermore, since the switch forwards all received data based on its destination address, the workstation can now communicate with the server and vice versa if they provide the correct destination address.
 * So, we have created a very simple computer network with two interconnected nodes which are able to communicate.
 * Also, the switch allows us to easily add more nodes to the network as required.
 * We would simply define the new machine (with a network interface and a unique network address in the 192.168.1.0/24 range), add a network interface to our switch, and connect them with a wire.
 */
wire switch1, 2, server1

/*
 * The following directive connects the secondary workstation to network interface 3 (the fourth one) of the switch.
 * This enables network communication between all three sample machines (workstation1, workstation2 and server1).
 * Our computer network has now grown to three interconnected nodes which are able to communicate.
 * Note that we again omit specifying the index 0 for the workstation network interface.
 */
wire switch1, 3, workstation2