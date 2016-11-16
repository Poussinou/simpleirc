package tk.jordynsmediagroup.simpleirc.model;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Base class for conversations
 * <p/>
 * A conversation can be a channel, a query or server messages
 */
public abstract class Conversation implements Comparable<Conversation> {
  public static final int TYPE_CHANNEL = 1;
  public static final int TYPE_QUERY = 2;
  public static final int TYPE_SERVER = 3;

  public static final int STATUS_DEFAULT = 1;
  public static final int STATUS_SELECTED = 2;
  public static final int STATUS_MESSAGE = 3;
  public static final int STATUS_HIGHLIGHT = 4;
  public static final int STATUS_MISC = 5; // join/part/quit

  private static final int DEFAULT_HISTORY_SIZE = 30;


  private Server _server;

  public void setOwningServer(Server s) {
    _server = s;
  }

  private final LinkedList<Message> buffer;
  private final LinkedList<Message> history;
  private final String name;
  private int status = 1;
  private int newMentions = 0;
  private int historySize = DEFAULT_HISTORY_SIZE;

  /* Type list (order: lowest to highest) */
  private static final int[] typeList = {
      TYPE_QUERY,
      TYPE_CHANNEL,
      TYPE_SERVER
  };

  /**
   * Get the type of conversation (channel, query, ..)
   *
   * @return See the constants: Conversation.TYPE_*
   */
  public abstract int getType();

  /**
   * Create a new conversation with the given name
   *
   * @param name The name of the conversation (channel, user)
   */
  public Conversation(String name) {
    this.buffer = new LinkedList<Message>();
    this.history = new LinkedList<Message>();
    this.name = name;
  }

  /**
   * Compares this Conversation with another Conversation. This
   * compares the two Conversations by their types, and then by
   * their names.
   *
   * @param conversation The Conversation to compare
   */
  @Override
  public int compareTo(Conversation conversation) {
    int i1 = Arrays.binarySearch(typeList, getType());
    int i2 = Arrays.binarySearch(typeList, conversation.getType());

    if( i1 == i2 ) {
      /* Resort to a case-insensitive comparison */
      return name.compareToIgnoreCase(conversation.name);
    }

    /* Reversed comparison to account for an empty type */
    return Integer.valueOf(i2).compareTo(Integer.valueOf(i1));
  }

  /**
   * Get name of the conversation (channel, user)
   */
  public String getName() {
    return name;
  }

  /**
   * Add a message to the channel
   */
  public void addMessage(Message message) {
    // Don't parse smileys and colors and such if we're in a server view.
    if( this.getType() == TYPE_SERVER ) {
      message.setType(Message.TYPE_SERVER);
    }
    buffer.add(0, message);
    history.add(message);

    if( history.size() > historySize ) {
      history.get(0).setConversation(null);
      history.remove(0);
    }
  }

  /**
   * Get the history
   */
  public LinkedList<Message> getHistory() {
    return history;
  }

  /**
   * Get message of the history at the given position
   *
   * @param position
   * @return The message at the given position
   */
  public Message getHistoryMessage(int position) {
    return history.get(position);
  }

  /**
   * Get last buffered message
   *
   * @return
   */
  public Message pollBufferedMessage() {
    Message message = buffer.get(buffer.size() - 1);
    if( buffer.get(buffer.size() - 1) != null ) {
      buffer.get(buffer.size() - 1).setConversation(null);
    }
    buffer.remove(buffer.size() - 1);
    return message;
  }

  /**
   * Get the buffer
   *
   * @return
   */
  public LinkedList<Message> getBuffer() {
    return buffer;
  }

  /**
   * Does the channel have buffered messages?
   */
  public boolean hasBufferedMessages() {
    return buffer.size() > 0;
  }

  /**
   * Clear the message buffer
   */
  public void clearBuffer() {
    buffer.clear();
  }

  /**
   * Set status of conversation
   *
   * @param status
   */
  public void setStatus(int status) {
    // Selected status can only be changed by deselecting
    if( this.status == STATUS_SELECTED && status != STATUS_DEFAULT ) {
      return;
    }

    // Highlight status can only be changed by selecting
    if( this.status == STATUS_HIGHLIGHT && status != STATUS_SELECTED ) {
      return;
    }

    // Misc cannot change any other than default
    if( this.status != STATUS_DEFAULT && status == STATUS_MISC ) {
      return;
    }

    this.status = status;
  }

  /**
   * Get status of conversation
   *
   * @return
   */
  public int getStatus() {
    return status;
  }

  /**
   * Increment the count of unread mentions in this conversation
   */
  public void addNewMention() {
    ++newMentions;
  }

  /**
   * Mark all new mentions as unread
   */
  public void clearNewMentions() {
    newMentions = 0;
  }

  /**
   * Get the number of unread mentions in this conversation
   */
  public int getNewMentions() {
    return newMentions;
  }

  /**
   * Get this conversation's history size.
   *
   * @return The conversation's history size.
   */
  public int getHistorySize() {
    return historySize;
  }

  /**
   * Clears the history of a conversation.
   */
  public void clearHistory() {
    history.clear();
  }

  /**
   * Set this conversation's history size.
   *
   * @param size The new history size for this conversation.
   */
  public void setHistorySize(int size) {
    if( size <= 0 ) {
      return;
    }

    historySize = size;
    if( history.size() > size ) {
      history.subList(size, history.size()).clear();
    }
  }
}
