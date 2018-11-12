package com.jpstudiosonline.bccsms.bccsms;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.ContactElement;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.contact.Contact;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.contact.ContactDescription;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.contact.ContactSortOrder;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.core.ContactPickerActivity;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.group.Group;
import com.jpstudiosonline.bccsms.bccsms.contactpicker.picture.ContactPictureType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int RESULT_PICK_CONTACT = 1;
    public Handler handler = new Handler();
    Runnable runnable;

    public EditText etToField;
    public TextView tvMessageStatus;
    public boolean sendButtonPressedMoreThanOnce, sendingSMSActive;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView, mAdViewBottom, mAdViewMiddle;

    private static final String EXTRA_DARK_THEME = "EXTRA_DARK_THEME";
    private static final String EXTRA_GROUPS = "EXTRA_GROUPS";
    private static final String EXTRA_CONTACTS = "EXTRA_CONTACTS";

    private static final int REQUEST_CONTACT = 0;

    private boolean mDarkTheme;
    private List<Contact> mContacts;
    private List<Group> mGroups;
    public EditText etSMSMessage;
    public String fullNumbers;
    Handler adhandler;
    Runnable adrunnable;
    public int timeCounter = 0;
    public int adCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, "ca-app-pub-2289684703180323~4019081113");

        Button btSendSMS = (Button) findViewById(R.id.btSendSMS);
        Button btContacts = (Button) findViewById(R.id.btContacts);
        Button btNewContacts = (Button) findViewById(R.id.btNewContacts);
        final Button btStopSending = (Button) findViewById(R.id.btStopSending);
        etSMSMessage = (EditText) findViewById(R.id.etSMSMessage);
        etToField = (EditText) findViewById(R.id.etToField);
        tvMessageStatus = (TextView) findViewById(R.id.tvMessageStatus);
        btStopSending.setVisibility(View.GONE);

        mAdView = (AdView) findViewById(R.id.adsViewTop);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //mAdViewMiddle = (AdView) findViewById(R.id.adViewMiddle);
        //AdRequest adRequestMiddle = new AdRequest.Builder().build();
        //mAdViewMiddle.loadAd(adRequestMiddle);

        mAdViewBottom = (AdView) findViewById(R.id.adViewBottom);
        AdRequest adRequestBottom = new AdRequest.Builder().build();
        mAdViewBottom.loadAd(adRequestBottom);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-2289684703180323/6532783566");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        // read parameters either from the Intent or from the Bundle
        if (savedInstanceState != null) {
            mDarkTheme = savedInstanceState.getBoolean(EXTRA_DARK_THEME);
            mGroups = (List<Group>) savedInstanceState.getSerializable(EXTRA_GROUPS);
            mContacts = (List<Contact>) savedInstanceState.getSerializable(EXTRA_CONTACTS);
        }
        else {
            Intent intent = getIntent();
            mDarkTheme = intent.getBooleanExtra(EXTRA_DARK_THEME, false);
            mGroups = (List<Group>) intent.getSerializableExtra(EXTRA_GROUPS);
            mContacts = (List<Contact>) intent.getSerializableExtra(EXTRA_CONTACTS);
        }

        btNewContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, ContactPickerActivity.class)
                        .putExtra(ContactPickerActivity.EXTRA_THEME, mDarkTheme ?
                                R.style.AppTheme : R.style.AppTheme)

                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_BADGE_TYPE,
                                ContactPictureType.ROUND.name())

                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION,
                                ContactDescription.ADDRESS.name())
                        .putExtra(ContactPickerActivity.EXTRA_SHOW_CHECK_ALL, true)
                        .putExtra(ContactPickerActivity.EXTRA_SELECT_CONTACTS_LIMIT, 0)
                        .putExtra(ContactPickerActivity.EXTRA_ONLY_CONTACTS_WITH_PHONE, false)

                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION_TYPE,
                                ContactsContract.CommonDataKinds.Email.TYPE_WORK)

                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_SORT_ORDER,
                                ContactSortOrder.AUTOMATIC.name());

                startActivityForResult(intent, REQUEST_CONTACT);


                populateContactList(mGroups, mContacts);


            }
        });

        btSendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //sendSMSMessage();
                new sendSMSTask().execute("Send");

            }
        });

        btContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pickContact(v);
            }
        });

        btStopSending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendingSMSActive = false;
                tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "Sending SMS Has been stopped.");

            }
        });

        getPermissionContacts();
        initAds();


    }

    //Check for adblock
    public static boolean isAdBlockerPresent(boolean showAd) {
        if (showAd){
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        new FileInputStream("/etc/hosts")));
                String line;

                while ((line = in.readLine()) != null) {
                    if (line.contains("admob")) {
                        return true;
                    }
                }
            } catch (Exception e) {
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return false;
    }

    void initAds(){

        isAdBlockerPresent(true);

        mAdView.setAdListener(new AdBlockerListener(){
            public void onAdBlocked(){
                //User is using ad blocker
                //Toast.makeText(getApplicationContext(), "Adblocker detected! please disable it to use app!",Toast.LENGTH_LONG).show();
                //finish();
            }

            public boolean shouldCheckAdBlock(){
                //User not using adblocker
                //Toast.makeText(getApplicationContext(), "NOT using Adblocker",Toast.LENGTH_LONG).show();
                return true;
            }

        });

        adhandler = new Handler();

        adrunnable = new Runnable() {
            public void run() {
                timeReset();
            }
        };

        startTimer();
    }

    public void timeReset(){
        timeCounter++;

        //Log.e("adCounter", String.valueOf(adCounter));
        if (adCounter >= 180){

            showAd();
            adCounter = 0;

        }

        if (timeCounter > 15 ){

            adCounter += 15;
            timeCounter = 0;

        }
        //Log.e("TimerTicking", String.valueOf(timeCounter));

        adhandler.postDelayed(adrunnable,1000);
    }

    public void showAd(){

        if (mInterstitialAd.isLoaded()) {

            mInterstitialAd.show();

        } else {

            //Log.e("ad", "Notloaded");

        }
        //Log.e("showingInstAd", "showingInstAd");

    }

    public void startTimer() {
        adrunnable.run();
    }

    public void stopTimer() {
        handler.removeCallbacks(adrunnable);
    }

    private class sendSMSTask extends AsyncTask<String, Integer, String> {

        String updateMessageStatus, showFailedToSendMsg, showSMSStoppedMsg;

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Do something like display a progress bar
        }

        // This is run in a background thread
        @Override
        protected String doInBackground(String... params) {
            // get the string from params, which is an array
            String myString = params[0];
            String msgStatus = "";

            // Do something that takes a long time, for example:
            String smsMessage = etSMSMessage.getText().toString();


            if (etSMSMessage.length() >0 && fullNumbers != null){

                final String[] numbers = fullNumbers.split(",");

                if (sendButtonPressedMoreThanOnce){



                } else {

                    //prepareSMSSendUI(numbers.length);
                    updateMessageStatus = "Total SMS to send: " + String.valueOf(numbers.length);
                    updateMessageStatus = updateMessageStatus.toString() + "\n" + "Preparing to send";
                    //btStopSending.setVisibility(View.VISIBLE);

                    sendingSMSActive = true;


                }

                for (int i=0; i< numbers.length; i++){

                    sendButtonPressedMoreThanOnce = true;
                    sendingSMSActive = true;

                    if (sendingSMSActive){

                        final String numberToSendTo = numbers[i];
                        final String message = smsMessage;
                        final int sendCount = i;
                        //mInterstitialAd.loadAd(new AdRequest.Builder().build());

                                if (sendSMS2(numberToSendTo, message)){

                                    //updateMessageStatus(Integer.parseInt(numberToSendTo));
                                    updateMessageStatus = (updateMessageStatus.toString() + "\n" + "SMS Sent to " + String.valueOf(numberToSendTo));
                                    //tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "SMS Sent to " + String.valueOf(numberToSendTo));

                                    if (sendCount + 1 == numbers.length){

                                        sendButtonPressedMoreThanOnce = false;
                                        sendingSMSActive = false;
                                        //btStopSending.setVisibility(View.GONE);
                                    }


                                }else {

                                    //showFailedToSendMsg(Integer.parseInt(numberToSendTo));
                                    updateMessageStatus = (updateMessageStatus.toString() + "\n" +"Failed to send to " + String.valueOf(numberToSendTo));
                                    //tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" +"Failed to send to " + String.valueOf(numberToSendTo));
                                }


                    } else {

                        //showSMSStoppedMsg();
                        //tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "Sending SMS Has been stopped.");
                    }



                }

            } else {

                msgStatus = "InvalidNumbers";
                //Toast.makeText(getApplicationContext(), "Message or number list cannot be empty.",Toast.LENGTH_LONG).show();
                //mInterstitialAd.loadAd(new AdRequest.Builder().build());

            }

            return msgStatus;
        }

        // This is called from background thread but runs in UI
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            // Do things like update the progress bar
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String msgStatus = result;

            if (msgStatus.equals("InvalidNumbers")){

                showInvalidNumbers();
            }
            // Do things like hide the progress bar or change a TextView
            tvMessageStatus.setText(updateMessageStatus);
            loadNewAd();
        }
    }

    public void showInvalidNumbers(){

        Toast.makeText(getBaseContext(), "Message or number list cannot be empty.",Toast.LENGTH_LONG).show();
    }

    public void loadNewAd(){

        mAdView = (AdView) findViewById(R.id.adsViewTop);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //mAdViewMiddle = (AdView) findViewById(R.id.adViewMiddle);
        //AdRequest adRequestMiddle = new AdRequest.Builder().build();
        //mAdViewMiddle.loadAd(adRequestMiddle);

        mAdViewBottom = (AdView) findViewById(R.id.adViewBottom);
        AdRequest adRequestBottom = new AdRequest.Builder().build();
        mAdViewBottom.loadAd(adRequestBottom);

        mInterstitialAd = new InterstitialAd(getApplicationContext());
        mInterstitialAd.setAdUnitId("ca-app-pub-2289684703180323/6532783566");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    public void sendSMSMessage(){


        String smsMessage = etSMSMessage.getText().toString();


        if (etSMSMessage.length() >0 && fullNumbers != null){

            final String[] numbers = fullNumbers.split(",");

            if (sendButtonPressedMoreThanOnce){



            } else {

                tvMessageStatus.setText("Total SMS to send: " + String.valueOf(numbers.length));
                tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "Preparing to send");
                //btStopSending.setVisibility(View.VISIBLE);

                sendingSMSActive = true;


            }

            for (int i=0; i< numbers.length; i++){

                sendButtonPressedMoreThanOnce = true;
                sendingSMSActive = true;

                if (sendingSMSActive){

                    final String numberToSendTo = numbers[i];
                    final String message = smsMessage;
                    final int sendCount = i;
                    //mInterstitialAd.loadAd(new AdRequest.Builder().build());


                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms
                            runnable=this;

                            if (sendSMS2(numberToSendTo, message)){

                                tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "SMS Sent to " + String.valueOf(numberToSendTo));

                                if (sendCount + 1 == numbers.length){

                                    sendButtonPressedMoreThanOnce = false;
                                    sendingSMSActive = false;
                                    //btStopSending.setVisibility(View.GONE);
                                }

                                handler.removeCallbacks(runnable);

                            }else {

                                tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" +"Failed to send to " + String.valueOf(numberToSendTo));
                            }

                            handler.removeCallbacks(runnable);

                        }
                    }, 1000);


                } else {

                    tvMessageStatus.setText(tvMessageStatus.getText().toString() + "\n" + "Sending SMS Has been stopped.");
                }



            }

        } else {

            Toast.makeText(getApplicationContext(), "Message or number list cannot be empty.",Toast.LENGTH_LONG).show();
            //mInterstitialAd.loadAd(new AdRequest.Builder().build());

        }

        mAdView = (AdView) findViewById(R.id.adsViewTop);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //mAdViewMiddle = (AdView) findViewById(R.id.adViewMiddle);
        //AdRequest adRequestMiddle = new AdRequest.Builder().build();
        //mAdViewMiddle.loadAd(adRequestMiddle);

        mAdViewBottom = (AdView) findViewById(R.id.adViewBottom);
        AdRequest adRequestBottom = new AdRequest.Builder().build();
        mAdViewBottom.loadAd(adRequestBottom);

        mInterstitialAd = new InterstitialAd(getApplicationContext());
        mInterstitialAd.setAdUnitId("ca-app-pub-2289684703180323/6532783566");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void populateContactList(List<Group> groups, List<Contact> contacts) {
        // we got a result from the contact picker --> show the picked contacts
        TextView contactsView = (TextView) findViewById(R.id.contacts);
        SpannableStringBuilder result = new SpannableStringBuilder();
        fullNumbers = "";


        try {
            if (groups != null && ! groups.isEmpty() && contacts.isEmpty()) {
                result.append("Sending To:\n");
                for (Group group : groups) {
                    populateContact(result, group, "");
                    for (Contact contact : group.getContacts()) {
                        if (fullNumbers.length() >= 1){
                            fullNumbers += ",";
                        }

                        fullNumbers += String.valueOf(contact.getPhone(0));
                        populateContact(result, contact, String.valueOf(contact.getPhone(0)));
                    }
                }
            }
            if (contacts != null && !contacts.isEmpty()) {
                result.append("Sending To:\n");
                for (Contact contact : contacts) {
                    if (fullNumbers.length() >= 1){
                        fullNumbers += ",";
                    }

                    fullNumbers += String.valueOf(contact.getPhone(0));
                    populateContact(result, contact, String.valueOf(contact.getPhone(0)));
                }
            }
        }
        catch (Exception e) {
            result.append(e.getMessage());
        }

        contactsView.setText(result);
    }

    private void populateContact(SpannableStringBuilder result, ContactElement element, String prefix) {
        //int start = result.length();
        String displayName = ": " + element.getDisplayName() ;
        result.append(prefix);
        result.append(displayName + "\n");
        //result.setSpan(new BulletSpan(15), start, result.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private void getPermissionContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            //getContactList();
            getPermissionSMS();

        }
    }

    private void getPermissionSMS() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_READ_CONTACTS);

        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            //getContactList();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                getPermissionSMS();
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactList() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        //Toast.makeText(this, name + " " + phoneNo, Toast.LENGTH_SHORT).show();
                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }
    }

    public void sendSMS(String phoneNo, String msg) {
        try {

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            //Toast.makeText(getApplicationContext(), "Message Sent" + phoneNo,Toast.LENGTH_LONG).show();



        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public boolean sendSMS2(String phoneNo, String msg) {

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            //Toast.makeText(getApplicationContext(), "Message Sent" + phoneNo,Toast.LENGTH_LONG).show();
            return true;



        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
            return false;
        }
    }

    public void pickContact(View v)
    {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_DARK_THEME, mDarkTheme);
        if (mGroups != null) {
            outState.putSerializable(EXTRA_GROUPS, (Serializable) mGroups);
        }
        if (mContacts != null) {
            outState.putSerializable(EXTRA_CONTACTS, (Serializable) mContacts);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK && data != null &&
                (data.hasExtra(ContactPickerActivity.RESULT_GROUP_DATA) ||
                        data.hasExtra(ContactPickerActivity.RESULT_CONTACT_DATA))) {

            // we got a result from the contact picker --> show the picked contacts
            mGroups = (List<Group>) data.getSerializableExtra(ContactPickerActivity.RESULT_GROUP_DATA);
            mContacts = (List<Contact>) data.getSerializableExtra(ContactPickerActivity.RESULT_CONTACT_DATA);
            populateContactList(mGroups, mContacts);
        }
    }
    /**
     * Query the Uri and read contact details. Handle the picked contact data.
     * @param data
     */
    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null ;
            String name = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int  nameIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            // Set the value to the textviews
            //Log.e("dd", phoneNo);
            etToField.setText(etToField.getText() + phoneNo + ",");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard
     */
    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }
}
