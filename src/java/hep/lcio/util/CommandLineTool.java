package hep.lcio.util; 

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

/**
 * CommandLineTool provides a command-line interface
 * to LCIO utilities, similar to how the cvs command 
 * works.
 * 
 * lcio [global_options] [command] [command_options]
 * 
 * 
 * 
 * @see hep.lcio.util.Concat concat [X]
 * 
 * @see hep.lcio.util.MergeCommandHandler merge [X]
 * 
 * @see hep.lcio.util.Split split [X]
 * 
 * @see hep.lcio.util.SioDump siodump [X]
 * 
 * @see hep.lcio.util.Headers head
 * 
 * @see hep.lcio.util.Compare compare
 * 
 * @see hep.lcio.util.PrintEvent print
 * 
 * FIXME: Implement all of the above commands.
 * 
 * @author jeremym
 * @version $Id: CommandLineTool.java,v 1.6 2006-06-02 00:22:57 jeremy Exp $
 */
public class CommandLineTool
{
	private Map handlers = new HashMap();
	private String command;
	private Parser parser = new PosixParser();
	private Options options = new Options();
	private CommandLine cl;
	private static CommandLineTool instance = new CommandLineTool();
	
	/**
	 * Get an instance of CommandLineTool.
	 * @return A globall unique instance of CommandLineTool.
	 */
	public static CommandLineTool instance()
	{
		return instance;
	}
	
	/**
	 * The private ctor.
	 */
	private CommandLineTool()
	{
		registerOptions();
		registerHandlers();
	}	
	
	/**
	 * Get the global options.
	 * @return A CommandLine object representing the global options,
	 * e.g. the options in front of the command.
	 */
	public CommandLine getGlobalOptions()
	{
		return cl;
	}
	
	/**
	 * The default main method for the CommandLineTool.
	 * @param argv The raw input arguments from the CL.
	 * @throws Exception
	 */
	public static void main(String[] argv) throws Exception
	{
		CommandLineTool cl = CommandLineTool.instance();
		cl.parse(argv);
	}

	/**
	 * Register default command handlers with the CommandLineTool.
	 */
	private void registerHandlers()
	{
		addCommandHandler(new MergeCommandHandler());
		addCommandHandler(new SplitCommandHandler());
		addCommandHandler(new ConcatenateCommandHandler());
		addCommandHandler(new SIODumpCommandHandler());
		
		// addCommandHandler("compare", CompareCommandHandler())
		// addCommandHandler("print", PrintEventCommandHandler());
		// addCommandHandler("header", HeaderScanCommandHandler());		
		// addCommandHandler("random", RandomEventCommandHandler());
	}

	/**
	 * Register the CommandLineTool's global options.
	 */
	private void registerOptions()
	{
		Option opt = new Option("h", false, "Print lcio command-line tool usage.");
		options.addOption(opt);

		opt = new Option("v", false, "Set the verbosity.");
		opt.setArgs(1);
		options.addOption(opt);
	}

	/**
	 * Set the name of the active command.
	 * @param command String of the command.
	 */
	private void setCommand(String command)
	{
		this.command = command;
	}

	/**
	 * Get the name of the currently active command.
	 * @return String of the currently active command.
	 */
	public String getCommand()
	{
		return this.command;
	}

	/**
	 * Parse the argv for global options.
	 * Lookup the appropriate CommandHandler.
	 * Pass command options to @see CommandHandler.parse method.
	 * Execute the CommandHandler's execute method.
	 * @param argv The raw input arguments from CL.
	 * @throws Exception
	 */
	// FIXME: Method needs to handle quoting properly.  
	//        FreeHep's argv package can handle this.
	public void parse(String[] argv) throws Exception
	{
		// Get the index of a command.
		int icmd = scanForCommand(argv);

		if (icmd == -1)
		{
			// No command was found, so print usage and exit.
			printUsage(true);
		}

		// Set the command to execute.
		setCommand(argv[icmd]);

		System.out.println("Executing command: " + getCommand());

		// Get a command handler for this command string.
		CommandHandler handler = getCommandHandler(getCommand());

		// Global arguments.
		int nglob = icmd;
		String[] globargv = new String[0];
		if (nglob > 0)
		{
			globargv = new String[nglob];
			System.arraycopy(argv, 0, globargv, 0, nglob);
			for (int i = 0; i < globargv.length; i++)
			{
				System.out.println("globargv[" + i + "]=" + globargv[i]);
			}
		}

		// Arguments are passed verbatim to the command.
		int ncmd = argv.length - (icmd + 1);
		String[] cmdargv = new String[ncmd];
		System.arraycopy(argv, icmd + 1, cmdargv, 0, ncmd);
		for (int i = 0; i < cmdargv.length; i++)
		{
			System.out.println("cmdargv[" + i + "]=" + cmdargv[i]);
		}

		// Parse global options.
		cl = parser.parse(options, globargv);

		// Pass the command the command line options
		// with globals stripped out.
		handler.parse(cmdargv);

		// Execute the CommandHandler.
		handler.execute();
	}

	/**
	 * Add a @see CommandHandler to the CommandLineTool.
	 * The @see CommandHandler.getName method is the command
	 * that will be handled by this @see CommandHandler. 
	 * @param handler The CommandHandler.
	 */
	public void addCommandHandler(CommandHandler handler)
	{
		handlers.put(handler.getName(), handler);
	}

	/**
	 * Get a CommandHandler for the command.
	 * @param command A String of the command to lookup.
	 * @return A CommandHandler for this command.
	 * @throws Exception
	 */
	public CommandHandler getCommandHandler(String command) throws Exception
	{
		return (CommandHandler) handlers.get(command);
	}

	/**
	 * Print the lcio command usage.
	 * @param doExit Boolean that specifies whether to exit or not.
	 */
	public void printUsage(boolean doExit)
	{
		HelpFormatter help = new HelpFormatter();

		StringBuffer s = new StringBuffer();

		for (Iterator it = handlers.keySet().iterator(); it.hasNext();)
		{
			s.append((String) it.next() + '\n');
		}

		help.printHelp("LcioCommandLineTool", "Commands:\n" + s.toString(),
				options, "");

		if (doExit)
		{
			System.exit(0);
		}
	}
	
	/**
	 * Return the index of a command string in argv.
	 * @param argv The raw input arguments from CL.
	 * @return The index of the first command string in argv or -1 if not found.
	 */
	private int scanForCommand(String[] argv)
	{
		for (int i = 0; i < argv.length; i++)
		{
			if (handlers.get(argv[i]) != null)
			{
				//System.err.println("found " + argv[i] + " @ idx " + i);
				return i;
			}
		}
		return -1;
	}
}