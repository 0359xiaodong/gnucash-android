/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.ui;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.data.Transaction.TransactionType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class NewTransactionFragment extends SherlockFragment implements 
	OnDateSetListener, OnTimeSetListener {
	
	private TransactionsDbAdapter mTransactionsDbAdapter;
	private long mTransactionId = 0;
	private Transaction mTransaction;
	
	public static final String SELECTED_TRANSACTION_ID = "selected_transaction_id";
	
	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy");
	public final static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");
	
	private ToggleButton mTransactionTypeButton;
	private EditText mNameEditText;
	private EditText mAmountEditText;
	private EditText mDescriptionEditText;
	private TextView mDateTextView;
	private TextView mTimeTextView;		
	private Calendar mDate;
	private Calendar mTime;
	private Spinner mAccountsSpinner;
	private AccountsDbAdapter mAccountsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter; 
	
	private MenuItem mSaveMenuItem;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_new_transaction, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle(R.string.add_transaction);
		
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		View v = getView();
		
		mNameEditText = (EditText)getView().findViewById(R.id.input_transaction_name);
		mDescriptionEditText = (EditText)getView().findViewById(R.id.input_description);
		mDateTextView = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText = (EditText) v.findViewById(R.id.input_transaction_amount);
		mAccountsSpinner = (Spinner) v.findViewById(R.id.input_accounts_spinner);
		mTransactionTypeButton = (ToggleButton) v.findViewById(R.id.input_transaction_type);
		
		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		Cursor cursor = mAccountsDbAdapter.fetchAllAccounts();
		
		mCursorAdapter = new SimpleCursorAdapter(getActivity(), 
				android.R.layout.simple_spinner_item, 
				cursor,
				from,
				to, 
				0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mCursorAdapter);
		
		mTransactionId = getArguments().getLong(SELECTED_TRANSACTION_ID);
		mTransaction = mTransactionsDbAdapter.getTransaction(mTransactionId);
		
		setListeners();
		if (mTransaction == null)
			initalizeViews();
		else
			initializeViewsWithTransaction();
		
	}

	private void initializeViewsWithTransaction(){
				
		mNameEditText.setText(mTransaction.getName());
		mTransactionTypeButton.setChecked(mTransaction.getTransactionType() == TransactionType.DEBIT);
		//multiply to balance out division by the TextWatcher attached to this view
		mAmountEditText.setText(Double.toString(mTransaction.getAmount() * 10)); 
		mDescriptionEditText.setText(mTransaction.getDescription());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		mTime = mDate = Calendar.getInstance();
				
		final long accountId = mAccountsDbAdapter.fetchAccountWithUID(mTransaction.getAccountUID());
		final int count = mCursorAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId)
				mAccountsSpinner.setSelection(pos);
		}
		
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.edit_transaction);
	}
	
	/**
	 * Binds the various views to the appropriate text
	 */
	private void initalizeViews() {
		Date time = new Date(System.currentTimeMillis()); 
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();
				
		final long accountId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
		final int count = mCursorAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId)
				mAccountsSpinner.setSelection(pos);
		}
	}
	
	/**
	 * Sets click listeners for the dismiss buttons
	 */
	private void setListeners() {
		ValidationsWatcher validations = new ValidationsWatcher();
		mAmountEditText.addTextChangedListener(validations);
		mNameEditText.addTextChangedListener(validations);
		
		mAmountEditText.addTextChangedListener(new AmountInputFormatter());
		
		mTransactionTypeButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					int red = getResources().getColor(R.color.debit_red);
					mTransactionTypeButton.setTextColor(red);
					mAmountEditText.setTextColor(red);					
				}
				else {
					int green = getResources().getColor(R.color.credit_green);
					mTransactionTypeButton.setTextColor(green);
					mAmountEditText.setTextColor(green);
				}
				mAmountEditText.setText(mAmountEditText.getText().toString()); //trigger an edit to update the number sign
			}
		});

		mDateTextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				 
				long dateMillis = 0;				
				try {
					Date date = DATE_FORMATTER.parse(mDateTextView.getText().toString());
					dateMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}
				DialogFragment newFragment = new DatePickerDialogFragment(NewTransactionFragment.this, dateMillis);
				newFragment.show(ft, "date_dialog");
			}
		});
		
		mTimeTextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				long timeMillis = 0;				
				try {
					Date date = TIME_FORMATTER.parse(mTimeTextView.getText().toString());
					timeMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}
				DialogFragment fragment = new TimePickerDialogFragment(NewTransactionFragment.this, timeMillis);
				fragment.show(ft, "time_dialog");
			}
		});
	}	
	
	private void saveNewTransaction() {
		String name = mNameEditText.getText().toString();
		String description = mDescriptionEditText.getText().toString();
		String amountString = mAmountEditText.getText().toString();
		double amount = Double.parseDouble(stripCurrencyFormatting(amountString))/100;
		amount *= mTransactionTypeButton.isChecked() ? -1 : 1; //set negative for debit
		Calendar cal = new GregorianCalendar(
				mDate.get(Calendar.YEAR), 
				mDate.get(Calendar.MONTH), 
				mDate.get(Calendar.DAY_OF_MONTH), 
				mTime.get(Calendar.HOUR_OF_DAY), 
				mTime.get(Calendar.MINUTE), 
				mTime.get(Calendar.SECOND));
		
		long accountID = mAccountsSpinner.getSelectedItemId();
		Account account = mAccountsDbAdapter.getAccount(accountID);
		String type = mTransactionTypeButton.getText().toString();
		
		if (mTransaction != null){
			mTransaction.setAmount(amount);
			mTransaction.setName(name);
			mTransaction.setTransactionType(TransactionType.valueOf(type));
		} else {
			mTransaction = new Transaction(amount, name, TransactionType.valueOf(type));
		}
		mTransaction.setAccountUID(account.getUID());
		mTransaction.setTime(cal.getTimeInMillis());
		mTransaction.setDescription(description);
		
		mTransactionsDbAdapter.addTransaction(mTransaction);
		mTransactionsDbAdapter.close();
		
		getSherlockActivity().getSupportFragmentManager().popBackStack();
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mAccountsDbAdapter.close();
		mTransactionsDbAdapter.close();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.new_transaction_actions, menu);
		mSaveMenuItem = menu.findItem(R.id.menu_save);
		//only initially enable if we are editing a transaction
		mSaveMenuItem.setEnabled(mTransactionId > 0);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_cancel:
			getSherlockActivity().getSupportFragmentManager().popBackStack();
			return true;
			
		case R.id.menu_save:
			saveNewTransaction();
			return true;

		default:
			return false;
		}
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
		mDateTextView.setText(DATE_FORMATTER.format(cal.getTime()));
		mDate.set(Calendar.YEAR, year);
		mDate.set(Calendar.MONTH, monthOfYear);
		mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
		mTimeTextView.setText(TIME_FORMATTER.format(cal.getTime()));	
		mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		mTime.set(Calendar.MINUTE, minute);
	}
	
	public static String stripCurrencyFormatting(String s){
		String symbol = Currency.getInstance(Locale.getDefault()).getSymbol();
		//if in scientific notation, do not remove the period
		String regex = s.contains("E") ? "[" + symbol + ",-]" : "[" + symbol + ",.-]";
		return s.replaceAll(regex, "");
	}
	
	private class ValidationsWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			boolean valid = (mNameEditText.getText().length() > 0) && 
					(mAmountEditText.getText().length() > 0);
			mSaveMenuItem.setEnabled(valid);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class AmountInputFormatter implements TextWatcher {
		private String current = null;
		
		@Override
		public void afterTextChanged(Editable s) {
			String cleanString = stripCurrencyFormatting(s.toString());
			if (cleanString.length() == 0)
				return;

			double parsed = Double.parseDouble(cleanString);

			mAmountEditText.removeTextChangedListener(this);

			String formattedString = NumberFormat.getCurrencyInstance().format(
					(parsed / 100));

			String prefix = mTransactionTypeButton.isChecked() ? " - " : "";

			current = prefix + formattedString;
			mAmountEditText.setText(current);
			mAmountEditText.setSelection(current.length());

			mAmountEditText.addTextChangedListener(this);

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// nothing to see here, move along
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// nothing to see here, move along
			
		}
		
	}
}
