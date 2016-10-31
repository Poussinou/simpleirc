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
package indrora.atomic.listener;

import indrora.atomic.activity.MessageActivity;
import indrora.atomic.adapter.MessageListAdapter;
import indrora.atomic.model.Extra;
import indrora.atomic.model.Message;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Listener for clicks on conversation messages
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class MessageClickListener implements OnItemClickListener,AdapterView.OnItemLongClickListener {
  private static MessageClickListener instance;

  /**
   * Private constructor
   */
  private MessageClickListener() {
  }

  /**
   * Get global instance of message click listener
   *
   * @return
   */
  public static synchronized MessageClickListener getInstance() {
    if( instance == null ) {
      instance = new MessageClickListener();
    }

    return instance;
  }

  private void doThing(AdapterView<?> group, int position) {
    android.util.Log.d("MessageClickListener", "clicking on item => "+position);
    MessageListAdapter adapter = (MessageListAdapter)group.getAdapter();
    Message m = adapter.getItem(position);
    Intent intent = new Intent(group.getContext(), MessageActivity.class);
    // this is going to be a parcelable.
    // Woo parcelables.
    intent.putExtra(Extra.MESSAGE, m);

    group.getContext().startActivity(intent);
  }

  /**
   * On message item clicked
   */
  @Override
  public void onItemClick(AdapterView<?> group, View view, int position, long id) {
    doThing(group, position);
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
    doThing(adapterView, i);

    return true;
  }
}
