package tk.jordynsmediagroup.simpleirc.exception;

/**
 * The CommandException is thrown on command execution if the
 * command couldn't be executed due to invalid params
 */
public class CommandException extends Throwable {
  private static final long serialVersionUID = -8317993941455253288L;

  /**
   * Create a new CommandException object
   */
  public CommandException(String message) {
    super(message);
  }
}
