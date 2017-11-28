package com.iam725.kunal.navigation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

        private static final String TAG = "CATCHAPP";
        private FirebaseAuth mAuth;
        private EditText emailEditText;
        private EditText passwordEditText;
        private String email;
        private String password;
        ProgressDialog progressDialog;
        //private TextView mStatusTextView;
        //private TextView mDetailTextView;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.user_login);

//                Toolbar toolbar = new Toolbar(this);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                        toolbar.setBackground(new ColorDrawable(Color.parseColor("#08B34A")));
//                }
//                setSupportActionBar(toolbar);
//                ActionBar bar = getActionBar();
//                if (bar != null) {
//                        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#08B34A")));
//                }
                progressDialog = new ProgressDialog(Login.this);
                mAuth = FirebaseAuth.getInstance();
                emailEditText = (EditText) findViewById(R.id.input_email);
                passwordEditText = (EditText) findViewById(R.id.input_password);
                Button loginButton = (Button) findViewById(R.id.btn_login);

                loginButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                signIn ();
                        }
                });
                //mStatusTextView = (TextView) findViewById(R.id.status);
                // mDetailTextView = (TextView) findViewById(R.id.detail);

        }

        @Override
        public void onStart() {
                super.onStart();
                // Check if user is signed in (non-null) and update UI accordingly.
                FirebaseUser currentUser = mAuth.getCurrentUser();
                mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                        @Override
                        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                        }
                });

                if (currentUser != null) {
                        Intent i = new Intent(Login.this, MainActivity.class);
                        /*SharedPreferences prefs = Login.this.getSharedPreferences("contact", MODE_WORLD_READABLE);
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putString("password", password);
                        prefsEditor.apply();*/
                        startActivity(i);
                }

        }

        private void signIn() {

                email = emailEditText.getText().toString();
                password = passwordEditText.getText().toString();

                Log.d(TAG, "signIn:" + email);
                if (!validateForm()) {
                        return;
                }
                Log.d(TAG, "EMAIL : " + email);
                Log.d(TAG, "PASSWORD : " + password);

                //showProgressDialog();

                // [START sign_in_with_email]
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                                // Sign in success, update UI with the signed-in user's information
                                                Log.d(TAG, "signInWithEmail:success");
                                                //FirebaseUser user = mAuth.getCurrentUser();
                                                Intent i = new Intent(Login.this, MainActivity.class);
//                                                SharedPreferences sp = getSharedPreferences("Login", MODE_PRIVATE);
//                                                SharedPreferences.Editor Ed = sp.edit();
//                                                Ed.putString("Unm",Value );
//                                                Ed.putString("Psw",Value);
//                                                Ed.commit();
                                                SharedPreferences prefs = Login.this.getSharedPreferences("contact", MODE_PRIVATE);
                                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                                prefsEditor.putString("email", email);
                                                prefsEditor.apply();

//                                                i.putExtra("email", email);
                                                progressDialog.setTitle("Catch App");
                                                progressDialog.setMessage("Logging In...");
                                                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                                progressDialog.show();
                                                progressDialog.setCancelable(true);
                                                new Thread(new Runnable() {
                                                        public void run() {
                                                                try {
                                                                        Thread.sleep(5000);
                                                                } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                }
                                                                if (progressDialog != null)
                                                                        progressDialog.dismiss();
                                                        }
                                                }).start();
//                                                ProgressDialogForLoggingIn dialog = new ProgressDialogForLoggingIn(Login.this);
//                                                dialog.execute();
                                                startActivity(i);
                                                finish();
                                        } else {
                                                // If sign in fails, display a message to the user.
                                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                                Toast.makeText(Login.this, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show();
                                        }

                                        // [START_EXCLUDE]
                        /*if (!task.isSuccessful()) {
                           mStatusTextView.setText(R.string.auth_failed);
                        }*/
                                        //hideProgressDialog();
                                        // [END_EXCLUDE]
                                }
                        });
                // [END sign_in_with_email]
        }

        private boolean validateForm() {
                boolean valid = true;

                String email = emailEditText.getText().toString();
                if (TextUtils.isEmpty(email)) {
                        emailEditText.setError("Required.");
                        valid = false;
                } else {
                        emailEditText.setError(null);
                }

                String password = passwordEditText.getText().toString();
                if (TextUtils.isEmpty(password)) {
                        passwordEditText.setError("Required.");
                        valid = false;
                } else {
                        passwordEditText.setError(null);
                }

                return valid;
        }
        private class ProgressDialogForLoggingIn extends AsyncTask<Void, Void, Void> {
                private ProgressDialog dialog;

                ProgressDialogForLoggingIn(Login activity) {
                        dialog = new ProgressDialog(activity);
                }

                @Override
                protected void onPreExecute() {
                        dialog.setMessage("Doing something, please wait.");
                        dialog.show();
                }

                protected Void doInBackground(Void... args) {
                        // do background work here
                        return null;
                }

                protected void onPostExecute(Void result) {
                        // do UI work here
                        if (dialog.isShowing()) {
                                dialog.dismiss();
                        }
                }
        }
        @Override
        public void onDestroy() {
                super.onDestroy();
                if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                }
        }

}
