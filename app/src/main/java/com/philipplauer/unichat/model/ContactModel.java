package com.philipplauer.unichat.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.philipplauer.unichat.persistence.ContactCursorWrapper;
import com.philipplauer.unichat.persistence.DatabaseBackend;

import java.util.ArrayList;
import java.util.List;

public class ContactModel {
    private static final String LOGTAG = "ContactModel";
    private static ContactModel sContactMoel;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    public static ContactModel get(Context context)
    {
        if(sContactMoel == null)
        {
            sContactMoel = new ContactModel(context);
        }
        return sContactMoel;
    }
    private ContactModel(Context context)
    {
        mContext = context;
        mDatabase = DatabaseBackend.getInstance(mContext).getWritableDatabase();
    }
    public List<Contact> getContacts()
    {
        List<Contact> contacts = new ArrayList<>();
        ContactCursorWrapper cursor = queryContacts(null,null);
        try
        {
            cursor.moveToFirst();
            while( !cursor.isAfterLast())
            {
                contacts.add(cursor.getContact());
                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return contacts;
    }
    public Contact getContactByJidString(String jidString)
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();
        Contact mContact = null;
        for(Contact contact :contacts)
        {
            Log.d(LOGTAG,"Contact Jid :"+contact.getJid());
            Log.d(LOGTAG,"Subscription type :"+ contact.getTypeStringValue(contact.getSubscriptionType()));
            if(contact.getJid().equals(jidString))
            {
                mContact = contact;
            }
        }
        return mContact;
    }
    public List<String> getContactsJidStrings()
    {
        List<Contact> contacts = getContacts();
        List<String> stringJids = new ArrayList<>();
        for(Contact contact :contacts)
        {
            Log.d(LOGTAG,"Contact Jid :"+contact.getJid());
            stringJids.add(contact.getJid());
        }
        return stringJids;
    }
    public boolean isContactStranger(String contactJid)
    {
        List<String> contacts = getContactsJidStrings();
        return !contacts.contains(contactJid);
    }
    private ContactCursorWrapper queryContacts(String whereClause , String[] whereArgs)
    {
        Cursor cursor = mDatabase.query(Contact.TABLE_NAME, null , whereClause, whereArgs, null , null, null);
        return new ContactCursorWrapper(cursor);
    }
    public boolean addContact(Contact c)
    {
        //TODO: Check if contact already in db before adding.
        ContentValues values = c.getContentValues();
        if ((mDatabase.insert(Contact.TABLE_NAME, null, values)== -1))
        {
            return false;
        }else
        {
            return true;
        }
    }
    public boolean updateContactSubscription(Contact contact)
    {
        Contact mContact = contact;
        String jidString = contact.getJid();
        ContentValues values = contact.getContentValues();
        int rows = mDatabase.update(Contact.TABLE_NAME, values, "jid = ? ", new String[] { jidString } );
        if( rows != 0)
        {
            Log.d(LOGTAG,"DB Update erfolgreich ");
            return true;
        }
        return false;
    }
    public void updateContactSubscriptionOnSendSubscribed(String contact)
    {
        Contact mContact = getContactByJidString(contact);
        mContact.setPendingFrom(false);
        updateContactSubscription(mContact);
    }
    public boolean deleteContact(Contact c)
    {
        int uniqueId = c.getPersistID();
        return deleteContact(uniqueId);
    }
    public boolean deleteContact ( int uniqueId)
    {
        int value =mDatabase.delete(Contact.TABLE_NAME,Contact.Cols.CONTACT_UNIQUE_ID+"=?",new String[] {String.valueOf(uniqueId)});
        if(value == 1)
        {
            return true;
        }else
        {
            return false;
        }
    }
}