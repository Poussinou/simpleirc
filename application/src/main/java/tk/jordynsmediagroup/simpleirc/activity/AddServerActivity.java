package tk.jordynsmediagroup.simpleirc.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import tk.jordynsmediagroup.simpleirc.Atomic;
import tk.jordynsmediagroup.simpleirc.R;
import tk.jordynsmediagroup.simpleirc.db.Database;
import tk.jordynsmediagroup.simpleirc.dialog.AddAliasView;
import tk.jordynsmediagroup.simpleirc.dialog.AddChannelView;
import tk.jordynsmediagroup.simpleirc.dialog.AuthenticationView;
import tk.jordynsmediagroup.simpleirc.dialog.CommandListView;
import tk.jordynsmediagroup.simpleirc.exception.ValidationException;
import tk.jordynsmediagroup.simpleirc.model.Authentication;
import tk.jordynsmediagroup.simpleirc.model.Extra;
import tk.jordynsmediagroup.simpleirc.model.Identity;
import tk.jordynsmediagroup.simpleirc.model.Server;
import tk.jordynsmediagroup.simpleirc.model.Settings;
import tk.jordynsmediagroup.simpleirc.model.Status;

/**
 * Add a new server to the list
 */
public class AddServerActivity extends Activity implements OnClickListener {
  private static final int REQUEST_CODE_CHANNELS = 1;
  private static final int REQUEST_CODE_COMMANDS = 2;
  private static final int REQUEST_CODE_ALIASES = 3;
  private static final int REQUEST_CODE_AUTHENTICATION = 4;

  public static final String ACTION_NEW_SERVER = "new_server";
  public static final String ACTION_EDIT_SERVER = "edit_server";
  public static final String ACTION_DUPE_SERVER = "dupe_server";

  private String _action = ACTION_NEW_SERVER;

  private Server server;
  private Authentication authentication;
  private ArrayList<String> aliases;
  private ArrayList<String> channels;
  private ArrayList<String> commands;

  private Settings s;

  /**
   * On create
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    s = new Settings(this);

    setContentView(R.layout.serveradd);

    ActionBar actionBar = getActionBar();


    authentication = new Authentication();
    aliases = new ArrayList<String>();
    channels = new ArrayList<String>();
    commands = new ArrayList<String>();

    ((Button)findViewById(R.id.add)).setOnClickListener(this);
    ((Button)findViewById(R.id.cancel)).setOnClickListener(this);
    ((Button)findViewById(R.id.aliases)).setOnClickListener(this);
    ((Button)findViewById(R.id.channels)).setOnClickListener(this);
    ((Button)findViewById(R.id.commands)).setOnClickListener(this);
    ((Button)findViewById(R.id.authentication)).setOnClickListener(this);

    Spinner spinner = (Spinner)findViewById(R.id.charset);
    String[] charsets = getResources().getStringArray(R.array.charsets);
    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, charsets);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);

    if( getIntent().getAction() != null ) {
      _action = getIntent().getAction();
    }

    ((EditText)findViewById(R.id.username)).setText(s.getDefaultUsername());
    ((EditText)findViewById(R.id.nickname)).setText(s.getDefaultNick());
    ((EditText)findViewById(R.id.realname)).setText(s.getDefaultRealname());


    Bundle extras = getIntent().getExtras();
    if( extras != null && extras.containsKey(Extra.SERVER) ) {

      if( _action.equals(ACTION_EDIT_SERVER) ) {
        setTitle(R.string.edit_server_label);
      }

      // Request to edit an existing server
      Database db = new Database(this);
      this.server = db.getServerById(extras.getInt(Extra.SERVER));
      aliases.addAll(server.getIdentity().getAliases());
      this.channels = db.getChannelsByServerId(server.getId());
      this.commands = db.getCommandsByServerId(server.getId());
      this.authentication = server.getAuthentication();
      db.close();


      // Set server values
      if( _action.equals(ACTION_EDIT_SERVER) ) {
        ((EditText)findViewById(R.id.title)).setText(server.getTitle());
      }
      ((EditText)findViewById(R.id.host)).setText(server.getHost());
      ((EditText)findViewById(R.id.port)).setText(String.valueOf(server.getPort()));
      ((EditText)findViewById(R.id.password)).setText(server.getPassword());

      ((EditText)findViewById(R.id.nickname)).setText(server.getIdentity().getNickname());
      ((EditText)findViewById(R.id.username)).setText(server.getIdentity().getIdent());
      ((EditText)findViewById(R.id.realname)).setText(server.getIdentity().getRealName());
      ((CheckBox)findViewById(R.id.useSSL)).setChecked(server.useSSL());
      ((CheckBox)findViewById(R.id.autoconnect)).setChecked(server.getAutoconnect());

      // Select charset
      if( server.getCharset() != null ) {
        for( int i = 0; i < charsets.length; i++ ) {
          if( server.getCharset().equals(charsets[i]) ) {
            spinner.setSelection(i);
            break;
          }
        }
      }

      // Make the requested server null, since we don't care anymore.
      if( _action.equals(ACTION_DUPE_SERVER) ) {
        this.server = null;
      }


    }

    // Disable suggestions for host name
    if( android.os.Build.VERSION.SDK_INT >= 5 ) {
      EditText serverHostname = (EditText)findViewById(R.id.host);
      serverHostname.setInputType(0x80000);
    }

    Uri uri = getIntent().getData();
    if( uri != null && uri.getScheme().equals("irc") ) {
      // handling an irc:// uri

      ((EditText)findViewById(R.id.host)).setText(uri.getHost());
      if( uri.getPort() != -1 ) {
        ((EditText)findViewById(R.id.port)).setText(String.valueOf(uri.getPort()));
      }
      if( uri.getPath() != null ) {
        channels.add(uri.getPath().replace('/', '#'));
      }
      if( uri.getQuery() != null ) {
        ((EditText)findViewById(R.id.password)).setText(String.valueOf(uri.getQuery()));
      }
    }
  }

  /**
   * On options menu requested
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    MenuInflater inflater = new MenuInflater(this);
    inflater.inflate(R.menu.addserver, menu);

    return true;
  }

  /**
   * On menu item selected
   */
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch ( item.getItemId() ) {
      case R.id.save:
        save();
        return true;

      case android.R.id.home:
        finish();
        break;
    }

    return super.onMenuItemSelected(featureId, item);
  }

  /**
   * On activity result

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if( resultCode != RESULT_OK ) {
      return; // ignore everything else
    }

    switch ( requestCode ) {
      case REQUEST_CODE_ALIASES:
        aliases.clear();
        aliases.addAll(data.getExtras().getStringArrayList(Extra.ALIASES));
        break;

      case REQUEST_CODE_CHANNELS:
        channels = data.getExtras().getStringArrayList(Extra.CHANNELS);
        break;

      case REQUEST_CODE_COMMANDS:
        commands = data.getExtras().getStringArrayList(Extra.COMMANDS);
        break;
    }
  }*/

  private void showAuthDialog() {
    AlertDialog.Builder b = new AlertDialog.Builder(this);

    final AuthenticationView v = new AuthenticationView(this, authentication);
    b.setView(v);
    b.setCancelable(true);
    b.setTitle(R.string.authentication);
    b.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        // woo
        authentication.setNickservPassword(v.getNickservPassword());
        authentication.setSaslUsername(v.getSaslUsername());
        authentication.setSaslPassword(v.getSaslPassword());
      }
    });
    b.setNegativeButton(R.string.action_cancel, null);

    b.show();

  }

  private void showCommmandList() {
    AlertDialog.Builder b = new AlertDialog.Builder(this);
    final CommandListView cv = new CommandListView(this, commands);
    b.setView(cv);
    b.setTitle(R.string.commands);
    b.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        commands = cv.getCommands();
      }
    });
    b.setNegativeButton(R.string.action_cancel, null);
    b.show();
  }

  private void showAliasList() {
    AlertDialog.Builder b = new AlertDialog.Builder(this);
    final AddAliasView av = new AddAliasView(this, aliases);
    b.setView(av);
    b.setTitle(R.string.aliases);
    b.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        aliases = av.getAliases();
      }
    });
    b.setNegativeButton(R.string.action_cancel, null);
    b.show();
  }

  private void showChannelList() {
    AlertDialog.Builder b = new AlertDialog.Builder(this);
    final AddChannelView channelview = new AddChannelView(this, channels);
    b.setView(channelview);
    b.setTitle(R.string.channels);
    b.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        channels = channelview.getChannels();
      }
    });
    b.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        return;
      }
    });
    b.show();
  }

  /**
   * On click add server or cancel activity
   */
  @Override
  public void onClick(View v) {
    switch ( v.getId() ) {
      case R.id.aliases:
        showAliasList();
        break;

      case R.id.authentication:
        showAuthDialog();
        break;

      case R.id.channels:
        showChannelList();
        break;

      case R.id.commands:
        showCommmandList();
        break;

      case R.id.add:
        save();
        break;

      case R.id.cancel:
        setResult(RESULT_CANCELED);
        finish();
        break;
    }
  }

  /**
   * Try to save server.
   */
  private void save() {
    try {
      validateServer();
      validateIdentity();
      if( server == null || _action.equals(ACTION_DUPE_SERVER) ) {
        addServer();
      } else {
        updateServer();
      }
      setResult(RESULT_OK);
      finish();
    } catch ( ValidationException e ) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Add server to database
   */
  private void addServer() {
    Database db = new Database(this);

    Identity identity = getIdentityFromView();
    long identityId = db.addIdentity(
        identity.getNickname(),
        identity.getIdent(),
        identity.getRealName(),
        identity.getAliases()
    );

    Server server = getServerFromView();
    server.setAuthentication(authentication);

    long serverId = db.addServer(server, (int)identityId);

    db.setChannels((int)serverId, channels);
    db.setCommands((int)serverId, commands);

    db.close();

    server.setId((int)serverId);
    server.setIdentity(identity);
    server.setAutoJoinChannels(channels);
    server.setConnectCommands(commands);

    Atomic.getInstance().addServer(server);
  }

  /**
   * Update server
   */
  private void updateServer() {
    Database db = new Database(this);

    int serverId = this.server.getId();
    int identityId = db.getIdentityIdByServerId(serverId);

    Server server = getServerFromView();
    server.setAuthentication(authentication);
    db.updateServer(serverId, server, identityId);

    Identity identity = getIdentityFromView();
    db.updateIdentity(
        identityId,
        identity.getNickname(),
        identity.getIdent(),
        identity.getRealName(),
        identity.getAliases()
    );

    db.setChannels(serverId, channels);
    db.setCommands(serverId, commands);

    db.close();

    server.setId(this.server.getId());
    server.setIdentity(identity);
    server.setAutoJoinChannels(channels);
    server.setConnectCommands(commands);

    Atomic.getInstance().updateServer(server);
  }

  /**
   * Populate a server object from the data in the view
   *
   * @return The server object
   */
  private Server getServerFromView() {
    String title = ((EditText)findViewById(R.id.title)).getText().toString().trim();
    String host = ((EditText)findViewById(R.id.host)).getText().toString().trim();
    int port = Integer.parseInt(((EditText)findViewById(R.id.port)).getText().toString().trim());
    String password = ((EditText)findViewById(R.id.password)).getText().toString().trim();
    String charset = ((Spinner)findViewById(R.id.charset)).getSelectedItem().toString();
    Boolean useSSL = ((CheckBox)findViewById(R.id.useSSL)).isChecked();
    Boolean autoConnect = ((CheckBox)findViewById(R.id.autoconnect)).isChecked();

    Server server = new Server();
    server.setHost(host);
    server.setPort(port);
    server.setPassword(password);
    server.setTitle(title);
    server.setCharset(charset);
    server.setUseSSL(useSSL);
    server.setAutoconnect(autoConnect);
    server.setStatus(Status.DISCONNECTED);

    return server;
  }

  /**
   * Populate an identity object from the data in the view
   *
   * @return The identity object
   */
  private Identity getIdentityFromView() {
    String nickname = ((EditText)findViewById(R.id.nickname)).getText().toString().trim();
    String username = ((EditText)findViewById(R.id.username)).getText().toString().trim();
    String realname = ((EditText)findViewById(R.id.realname)).getText().toString().trim();

    Identity identity = new Identity();
    identity.setNickname(nickname);
    identity.setUsername(username);
    identity.setRealName(realname);

    identity.setAliases(aliases);

    return identity;
  }

  /**
   * Validate the input for a server
   *
   * @throws ValidationException
   */
  private void validateServer() throws ValidationException {
    String title = ((EditText)findViewById(R.id.title)).getText().toString();
    String host = ((EditText)findViewById(R.id.host)).getText().toString();
    String port = ((EditText)findViewById(R.id.port)).getText().toString();
    String charset = ((Spinner)findViewById(R.id.charset)).getSelectedItem().toString();

    if( title.trim().equals("") ) {
      throw new ValidationException(getResources().getString(R.string.validation_blank_title));
    }

    if( host.trim().equals("") ) {
      // XXX: We should use some better host validation
      throw new ValidationException(getResources().getString(R.string.validation_blank_host));
    }

    try {
      Integer.parseInt(port);
    } catch ( NumberFormatException e ) {
      throw new ValidationException(getResources().getString(R.string.validation_invalid_port));
    }

    try {
      "".getBytes(charset);
    } catch ( UnsupportedEncodingException e ) {
      throw new ValidationException(getResources().getString(R.string.validation_unsupported_charset));
    }

    Database db = new Database(this);
    if( db.isTitleUsed(title) && (server == null || !server.getTitle().equals(title)) ) {
      db.close();
      throw new ValidationException(getResources().getString(R.string.validation_title_used));
    }
    db.close();
  }

  /**
   * Validate the input for a identity
   *
   * @throws ValidationException
   */
  private void validateIdentity() throws ValidationException {
    String nickname = ((EditText)findViewById(R.id.nickname)).getText().toString();
    String username = ((EditText)findViewById(R.id.username)).getText().toString();
    String realname = ((EditText)findViewById(R.id.realname)).getText().toString();

    if( nickname.trim().equals("") ) {
      throw new ValidationException(getResources().getString(R.string.validation_blank_nickname));
    }

    if( username.trim().equals("") ) {
      throw new ValidationException(getResources().getString(R.string.validation_blank_ident));
    }

    if( realname.trim().equals("") ) {
      throw new ValidationException(getResources().getString(R.string.validation_blank_realname));
    }

    // RFC 1459:  <nick> ::= <letter> { <letter> | <number> | <special> }
    // <special>    ::= '-' | '[' | ']' | '\' | '`' | '^' | '{' | '}'
    // Chars that are not in RFC 1459 but are supported too:
    // | and _
    Pattern nickPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9^\\-`\\[\\]{}|_\\\\]*$");
    if( !nickPattern.matcher(nickname).matches() ) {
      throw new ValidationException(getResources().getString(R.string.validation_invalid_nickname));
    }

    // We currently only allow chars, numbers and some special chars for ident
    // We should accept @ as well so that users who like ZNC's client tags
    // http://wiki.znc.in/FAQ#Why_do_I_get_an_.22Incorrect_Password.22_every_time_I_connect_even_though_my_pass_is_correct.3F
    Pattern identPattern = Pattern.compile("^[a-zA-Z0-9\\[\\]\\-@_/]+$");
    if( !identPattern.matcher(username).matches() ) {
      throw new ValidationException(getResources().getString(R.string.validation_invalid_ident));
    }
  }
}
