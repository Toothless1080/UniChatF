package com.philipplauer.unichat.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.philipplauer.unichat.R;
import com.philipplauer.unichat.Utilities;
import com.philipplauer.unichat.model.Chat;
import com.philipplauer.unichat.model.ChatModel;
import com.philipplauer.unichat.xmpp.RoosterConnection;
import com.philipplauer.unichat.xmpp.RoosterConnectionService;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatHolder> {
    private static final String LOGTAG ="ChatListAdapter";
    public interface OnItemClickListener {
        public void onItemClick(String contactJid, Chat.ContactType chatType);
    }
    public interface OnItemLongClickListener{
        public void onItemLongClick(String contactJid, int chatUniqueId, View anchor);
    }
    List<Chat> chatList;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private Context mContext;
    public ChatListAdapter(Context context) {
        this.chatList = ChatModel.get(context).getChats();
        this.mContext = context;
    }
    public OnItemClickListener getmOnItemClickListener() {
        return mOnItemClickListener;
    }
    public void setmOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
    public OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }
    @Override
    public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.chat_list_item, parent, false);
        return new ChatHolder(view,this);
    }
    @Override
    public void onBindViewHolder(ChatHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bindChat(chat);
    }
    @Override
    public int getItemCount() {
        return chatList.size();
    }
    public void onChatCountChange() {
        chatList = ChatModel.get(mContext).getChats();
        notifyDataSetChanged();
    }
}
class ChatHolder extends RecyclerView.ViewHolder{
    private static final String LOGTAG = "ChatHolder";
    private TextView contactTextView;
    private TextView messageAbstractTextView;
    private TextView timestampTextView;
    private ImageView profileImage;
    private Chat mChat;
    private ChatListAdapter mChatListAdapter;
    public ChatHolder(final View itemView , ChatListAdapter adapter) {
        super(itemView);
        contactTextView = itemView.findViewById(R.id.contact_jid);
        messageAbstractTextView = itemView.findViewById(R.id.message_abstract);
        timestampTextView = itemView.findViewById(R.id.text_message_timestamp);
        profileImage = itemView.findViewById(R.id.profile);
        mChatListAdapter = adapter;
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatListAdapter.OnItemClickListener listener = mChatListAdapter.getmOnItemClickListener();
                if ( listener!= null)
                {
                    listener.onItemClick(contactTextView.getText().toString(),mChat.getContactType());
                }
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ChatListAdapter.OnItemLongClickListener listener = mChatListAdapter.getOnItemLongClickListener();
                if(listener != null)
                {
                    listener.onItemLongClick(mChat.getJid(),mChat.getPersistID(),itemView);
                    return true;
                }
                return false;
            }
        });
    }
    public void bindChat(Chat chat)
    {
        mChat = chat;
        contactTextView.setText(chat.getJid());
        messageAbstractTextView.setText(chat.getLastMessage());
        timestampTextView.setText(Utilities.getFormattedTime(mChat.getLastMessageTimeStamp()));
        profileImage.setImageResource(R.drawable.ic_profile);
        RoosterConnection rc = RoosterConnectionService.getConnection();
        if(rc != null)
        {
            String imageAbsPath = rc.getProfileImageAbsolutePath(mChat.getJid());
            if ( imageAbsPath != null)
            {
                Drawable d = Drawable.createFromPath(imageAbsPath);
                profileImage.setImageDrawable(d);
            }
        }
    }
}