/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2013 Sebastian Kaspari

This file is part of Yaaic.

Yaaic is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Yaaic is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Yaaic.  If not, see <http://www.gnu.org/licenses/>.
*/
package indrora.atomic.adapter;

import indrora.atomic.App;
import indrora.atomic.indicator.ConversationStateProvider;
import indrora.atomic.listener.MessageClickListener;
import indrora.atomic.model.Conversation;
import indrora.atomic.model.Server;
import indrora.atomic.view.MessageListView;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;


import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.viewpagerindicator.TitlePageIndicator;

/**
 * Adapter for displaying a pager of conversations.
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ConversationPagerAdapter extends PagerAdapter implements ConversationStateProvider {

  public static final int COLOR_NONE = -1;

  private final Server server;
  private LinkedList<ConversationInfo> conversations;
  private final HashMap<Integer, View> views;

  /**
   * Container class to remember conversation and view association.
   */
  public class ConversationInfo implements Comparable<ConversationInfo> {
    public Conversation conv;
    public MessageListAdapter adapter;
    public MessageListView view;

    public ConversationInfo(Conversation conv) {
      this.conv = conv;
      this.adapter = null;
      this.view = null;
    }

    /**
     * Compares this ConversationInfo with another ConversationInfo.
     * This compares the two ConversationInfos by their Conversations.
     *
     * @param convInfo The ConversationInfo to compare
     */
    @Override
    public int compareTo(ConversationInfo convInfo) {
      return conv.compareTo(convInfo.conv);
    }
  }

  /**
   * Create a new {@link ConversationPagerAdapter} instance.
   */
  public ConversationPagerAdapter(Context context, Server server) {
    this.server = server;

    conversations = new LinkedList<ConversationInfo>();
    views = new HashMap<Integer, View>();
  }

  /**
   * Add a conversation to the adapter.
   *
   * @param conversation
   */
  public void addConversation(Conversation conversation) {
    conversations.add(new ConversationInfo(conversation));
    Collections.sort(conversations);

    notifyDataSetChanged();
  }

  /**
   * Remove the conversation at the given position from the adapter.
   *
   * @param position
   */
  public void removeConversation(int position) {
    conversations.remove(position);

    notifyDataSetChanged();
  }

  /**
   * Get position of given item.
   */
  @Override
  public int getItemPosition(Object object) {
    if( views.containsKey(object) ) {
      return POSITION_UNCHANGED;
    }

    return POSITION_NONE;
  }

  /**
   * Get item at position
   */
  public Conversation getItem(int position) {
    ConversationInfo convInfo = getItemInfo(position);
    if( convInfo != null ) {
      return convInfo.conv;
    } else {
      return null;
    }
  }

  /**
   * Get the adapter of the {@link MessageListView} at the given position.
   *
   * @param position
   * @return
   */
  public MessageListAdapter getItemAdapter(int position) {
    ConversationInfo convInfo = getItemInfo(position);
    if( convInfo != null ) {
      return convInfo.adapter;
    } else {
      return null;
    }
  }

  /**
   * Get the adapter of the {@link MessageListView} for the conversation
   * with the given name.
   *
   * @param name
   * @return
   */
  public MessageListAdapter getItemAdapter(String name) {
    return getItemAdapter(getPositionByName(name));
  }

  /**
   * Get ConversationInfo on item at position
   *
   * @param position
   */
  private ConversationInfo getItemInfo(int position) {
    if( position >= 0 && position < conversations.size() ) {
      return conversations.get(position);
    }
    return null;
  }

  /**
   * Get an item by the channel's name
   *
   * @param name
   * @return The item
   */
  public int getPositionByName(String name) {
    // Optimization - cache field lookups
    int mSize = conversations.size();
    LinkedList<ConversationInfo> mItems = this.conversations;

    name = name.toLowerCase(Locale.US);

    for( int i = 0; i < mSize; i++ ) {
      ConversationInfo ci = mItems.get(i);
      if( ci.conv.getName().toLowerCase(Locale.US).equals(name) ) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Remove all conversations.
   */
  public void clearConversations() {
    conversations = new LinkedList<ConversationInfo>();
  }

  /**
   * Get number of conversations from this adapter.
   */
  @Override
  public int getCount() {
    return conversations.size();
  }

  /**
   * Determines whether a page View is associated with a specific key object.
   */
  @Override
  public boolean isViewFromObject(View view, Object object) {
    return view == object;
  }

  /**
   * Create a view object for the conversation at the given position.
   */
  @Override
  public Object instantiateItem(View collection, int position) {
    // ConversationInfo convInfo = getItemInfo(position);
    ConversationInfo convInfo = conversations.get(position);
    View view;

    if( convInfo.view != null ) {
      view = convInfo.view;
    } else {
      view = renderConversation(convInfo, collection);
    }

    views.put(position, view);
    ((ViewPager)collection).addView(view);

    return view;
  }

  /**
   * Render the given conversation and return the new view.
   *
   * @param convInfo
   * @param parent
   * @return
   */
  private MessageListView renderConversation(ConversationInfo convInfo, View parent) {
    MessageListView list = new MessageListView(parent.getContext());
    convInfo.view = list;
    list.setOnItemClickListener(MessageClickListener.getInstance());

    MessageListAdapter adapter = convInfo.adapter;

    if( adapter == null ) {
      adapter = new MessageListAdapter(convInfo.conv, parent.getContext());
      convInfo.adapter = adapter;
    }


    list.setAdapter(adapter);
    list.setSelection(adapter.getCount() - 1); // scroll to bottom

    return list;
  }

  /**
   * Remove the given view from the adapter and collection.
   */
  @Override
  public void destroyItem(View collection, int position, Object view) {
    ((ViewPager)collection).removeView((View)view);
    views.remove(position);
  }

  /**
   * Get the title for the given position. Used by the {@link TitlePageIndicator}.
   */
  @Override
  public String getPageTitle(int position) {
    Conversation conversation = getItem(position);

    if( conversation.getType() == Conversation.TYPE_SERVER ) {
      return server.getTitle();
    } else {
      return conversation.getName();
    }
  }

  @Override
  public int getColorAt(int position) {
    Conversation conversation = getItem(position);
    switch ( conversation.getStatus() ) {
      case Conversation.STATUS_HIGHLIGHT:
        return App.getColorScheme().getHighlight();
      case Conversation.STATUS_MESSAGE:
        return App.getColorScheme().getUserEvent();
      case Conversation.STATUS_MISC:
        return App.getColorScheme().getChannelEvent();
      default:
        return App.getColorScheme().getForeground();
    }
  }

  @Override
  public Boolean isGreaterSpecial(int position) {
    for(int i = conversations.size()-1; i >position; i--) {
      int status = getItem(i).getStatus();
      if(status == Conversation.STATUS_HIGHLIGHT) return true;
    }
    return false;
  }

  @Override
  public Boolean isLowerSpecial(int position) {
    for( int i = 0; i < position; i++) {
      int status = getItem(i).getStatus();
      if(status == Conversation.STATUS_HIGHLIGHT) return true;
    }
    return false;
  }

  /**
   * Get the state color for all conversations lower than the given position.
   */
  @Override
  public int getColorForLowerThan(int position) {
    int color = COLOR_NONE;

    for(int i = 0; i < position ; i++) {
      int status = getItem(i).getStatus();
      if (status == Conversation.STATUS_HIGHLIGHT) {
        return App.getColorScheme().getHighlight();
      } else if (color == COLOR_NONE && getColorAt(i) != COLOR_NONE) {
        color = getColorAt(i);
      }
    }
    return color;
  }

  /**
   * Get the state color for all conversations greater than the given position.
   */
  @Override
  public int getColorForGreaterThan(int position) {
    int color = COLOR_NONE;

    for(int i = conversations.size()-1; i > position; i--) {
      int status = getItem(i).getStatus();
      if (status == Conversation.STATUS_HIGHLIGHT) {
        return App.getColorScheme().getHighlight();
      } else if (color == COLOR_NONE && getColorAt(i) != COLOR_NONE) {
        color = getColorAt(i);
      }
    }
    return COLOR_NONE;

  }
}
