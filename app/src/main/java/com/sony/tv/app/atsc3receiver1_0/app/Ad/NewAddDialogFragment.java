package com.sony.tv.app.atsc3receiver1_0.app.Ad;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sony.tv.app.atsc3receiver1_0.R;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewAddDialogFragment extends DialogFragment {

    private EditText adTitleEditText, adUrlEditText;
    private String scheme = "";
    private Realm realm;


    public NewAddDialogFragment() {
        // Required empty public constructor
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder adDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        realm = Realm.getDefaultInstance();

        View convertView = layoutInflater.inflate(R.layout.fragment_new_add_dialog, null);
        adDialog.setView(convertView);

        adTitleEditText = (EditText)convertView.findViewById(R.id.ad_title_edit_text);
        adUrlEditText = (EditText)convertView.findViewById(R.id.ad_uri_edit_text);
        RadioGroup radioGroup = (RadioGroup)convertView.findViewById(R.id.radio_group_scheme_type);
        RadioButton httpButton = (RadioButton) radioGroup.findViewById(R.id.button_http);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId){
                    case R.id.button_http:
                        scheme = "http://";
                        break;
                    case R.id.button_asset:
                        scheme = "asset://";
                        break;
                    case R.id.button_file:
                        scheme = "file://";
                        break;
                    default:
                        Log.d("Oops", "THis should not happend");
                }
            }
        });

        View titleView = layoutInflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(R.string.enter_new_add);
        adDialog.setCustomTitle(titleView);




        adDialog.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        });

        adDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        return adDialog.create();
    }


    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null){
            Button positiveButton = (Button)dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean readyToCloseDialog = false;
                    if (requiredFieldCompleted()) {
                        saveAd();
                        readyToCloseDialog = true;
                    }
                    if (readyToCloseDialog)
                        dismiss();
                }
            });
        }
    }

    private void saveAd() {
//        Ads.addAd(adUrlEditText.getText().toString(), false);
//        EventBus.getDefault().post(new OnNewAdInsertedEvent());
//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                long adId = ATSC3.adPrimaryKey.getAndIncrement();
//                AdContent ad = realm.createObject(AdContent.class, adId);
//                ad.enabled = false;
//                ad.scheme = scheme;
//                ad.title = adTitleEditText.getText().toString();
//                ad.uri
//            }
//        });

    }

    //Checks to see if the required AdContent fields has been completed
    private boolean requiredFieldCompleted() {
        if (scheme.equals("")){
            Toast.makeText(getActivity(), "Select Scheme", Toast.LENGTH_LONG);
            return false;
        }

        if (TextUtils.isEmpty(adTitleEditText.getText())){
            adTitleEditText.setError("Enter Title");
            return false;
        }

        if (TextUtils.isEmpty(adUrlEditText.getText())){
            adUrlEditText.setError("Enter Url");
            return false;
        }


        return true;
    }

    @Override
    public void onDestroy() {
        if (!realm.isClosed()){
            realm.close();
        }
        super.onDestroy();
    }
}
