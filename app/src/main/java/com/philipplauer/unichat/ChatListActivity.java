package com.philipplauer.unichat;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.philipplauer.unichat.adapters.ChatListAdapter;
import com.philipplauer.unichat.model.Chat;
import com.philipplauer.unichat.model.ChatModel;
import com.philipplauer.unichat.xmpp.RoosterConnectionService;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnItemClickListener, ChatListAdapter.OnItemLongClickListener {
    protected static final int REQUEST_EXCEMPT_OP = 188;
    private static final String LOGTAG = "ChatListActivity";
    ChatListAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setTitle(R.string.chatListTitle);
        //Loginstatus abrufen, Falls nicht eingeloggt zurück zum LoginScreen
        boolean logged_in_state = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("xmpp_logged_in", false);
        if (!logged_in_state) {
            Log.d(LOGTAG, "Login Status :" + logged_in_state);
            Intent i = new Intent(ChatListActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        } else {
            if (!Utilities.isServiceRunning(RoosterConnectionService.class, getApplicationContext())) {
                Log.d(LOGTAG, "Service aus, wird gestartet");
                Intent i1 = new Intent(this, RoosterConnectionService.class);
                startService(i1);
            } else {
                Log.d(LOGTAG, "Service läuft");
            }
        }
        RecyclerView chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        mAdapter = new ChatListAdapter(getApplicationContext());
        mAdapter.setmOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
        chatsRecyclerView.setAdapter(mAdapter);
        FloatingActionButton newConversationButton = findViewById(R.id.new_conversation_floating_button);
        newConversationButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        newConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChatListActivity.this, ContactListActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case Constants.BroadCastMessages.UI_NEW_CHAT_ITEM:
                        mAdapter.onChatCountChange();
                        return;
                }
            }
        };
        IntentFilter filter = new IntentFilter(Constants.BroadCastMessages.UI_NEW_CHAT_ITEM);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_me_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.me) {
            Intent i = new Intent(ChatListActivity.this, MeActivity.class);
            startActivity(i);
        } else if (item.getItemId() == R.id.logout) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.edit().clear().commit();
            Intent l = new Intent(ChatListActivity.this, LoginActivity.class);
            startActivity(l);
        } else if (item.getItemId() == R.id.groupchat) {
            // Gruppenchat Dialog
            final Dialog dialog = new Dialog(ChatListActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.groupchat_dialog);
            Button groupcreate = dialog.findViewById(R.id.groupchat_d_create);
            Button groupcancel = dialog.findViewById(R.id.groupchat_d_cancel);
            final EditText groupchatname = dialog.findViewById(R.id.groupchat_d_chatname);
            final Spinner member1 = dialog.findViewById(R.id.groupchat_d_spin1);
            //Spinner wird mit Testwerten gefüllt
            List<String> spinArray = new ArrayList<>();
            spinArray.add("");
            spinArray.add("test");
            spinArray.add("test2");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinArray);
            member1.setAdapter(adapter);
            groupcreate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!groupchatname.getText().toString().equals("") && !member1.getSelectedItem().toString().equals("")) {
                        String serverurl = "saar-force.de";
                        try {
                            //XMPPConnection ohne login nur für Gruppenchat
                            XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                                    .setXmppDomain(serverurl)
                                    .setHost(serverurl)
                                    .setResource("saar-force.de")
                                    .setKeystoreType(null)
                                    .setSendPresence(true)
                                    .setDebuggerEnabled(true)
                                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                                    .setCompressionEnabled(true).build();
                            SmackConfiguration.DEBUG = true;
                            XMPPTCPConnection.setUseStreamManagementDefault(true);
                            final AbstractXMPPConnection mConnection = new XMPPTCPConnection(conf);
                            Thread thread = new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        while(true) {
                                            try {
                                                mConnection.connect();
                                                createGroupChat(mConnection);
                                            } catch (SmackException | IOException | XMPPException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            thread.start();
                        } catch (XmppStringprepException e) {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    } else if (groupchatname.getText().toString().equals("")) {
                        groupchatname.setHint("Bitte Gruppennamen angeben");
                        groupchatname.setHintTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else if (member1.getSelectedItem().toString().equals("")) {
                        TextView bg = dialog.findViewById(R.id.groupchat_d_spinback);
                        bg.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    }
                }
            });
            groupcancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    //Aufruf eines Chats bei Klick
    @Override
    public void onItemClick(String contactJid, Chat.ContactType chatType) {
        Intent i = new Intent(ChatListActivity.this, ChatViewActivity.class);
        i.putExtra("contact_jid", contactJid);
        i.putExtra("chat_type", chatType);
        startActivity(i);
    }

    // Optionsmenu bei Chat nach langem Klick
    @Override
    public void onItemLongClick(final String contactJid, final int chatUniqueId, View anchor) {
        PopupMenu popup = new PopupMenu(ChatListActivity.this, anchor, Gravity.CENTER);
        popup.getMenuInflater().inflate(R.menu.chat_list_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete_chat:
                        if (ChatModel.get(getApplicationContext()).deleteChat(chatUniqueId)) {
                            mAdapter.onChatCountChange();
                            Toast.makeText(ChatListActivity.this, "Chat erfolgreich gelöscht", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    // Ein Versuch zum Erstellen eines GroupChat auf dem Openfire Server
    // Leider ohne Funktion. Nähere Details im Projektbericht
    public void createGroupChat(XMPPConnection connection) {
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
        try {
            EntityBareJid jid = JidCreate.entityBareFrom("toothless");
            MultiUserChat muc = manager.getMultiUserChat(jid);
            Set<Jid> owners = JidUtil.jidSetFrom(new String[]{"toothless@saar-force.de"});
            Resourcepart nickname = Resourcepart.from("toothless");
            muc.create(nickname).getConfigFormManager().setRoomOwners(owners).submitConfigurationForm();
        } catch (XmppStringprepException | MultiUserChatException.MucAlreadyJoinedException | InterruptedException | XMPPException.XMPPErrorException | MultiUserChatException.MissingMucCreationAcknowledgeException |
                SmackException.NotConnectedException | SmackException.NoResponseException | MultiUserChatException.NotAMucServiceException | MultiUserChatException.MucConfigurationNotSupportedException e) {
            e.printStackTrace();
        }
    }
}