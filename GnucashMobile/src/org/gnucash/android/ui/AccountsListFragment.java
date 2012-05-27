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
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseHelper;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class AccountsListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, View.OnClickListener {

	private static final int DIALOG_ADD_ACCOUNT = 0x10;
	
	SimpleCursorAdapter mCursorAdapter;
	AddAccountDialogFragment mAddAccountFragment;
	private AccountsDbAdapter mAccountsDbAdapter;	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_accounts_list, container,
				false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity().getApplicationContext());
		
		setHasOptionsMenu(true);
		mCursorAdapter = new AccountsCursorAdapter(getActivity()
				.getApplicationContext(), R.layout.item_accounts, null,
				new String[] { DatabaseHelper.KEY_NAME },
				new int[] { R.id.account_name }, 0);

		setListAdapter(mCursorAdapter);
		getLoaderManager().initLoader(0, null, this);
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.acccount_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_edit_accounts:
			return true;

		case R.id.menu_add_account:
			showAddAccountDialog();
			return true;

		default:
			return true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mAccountsDbAdapter.close();
	}
	
	public void addAccount(String name) {
		mAccountsDbAdapter.addAccount(new Account(name));			
		getLoaderManager().restartLoader(0, null, this);
	}
	
	public void showAddAccountDialog() {

		FragmentTransaction ft = getSherlockActivity()
				.getSupportFragmentManager().beginTransaction();
		Fragment prev = getSherlockActivity().getSupportFragmentManager()
				.findFragmentByTag("add_account_dialog");
		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		mAddAccountFragment = AddAccountDialogFragment
				.newInstance(this);
		mAddAccountFragment.setTargetFragment(this, DIALOG_ADD_ACCOUNT);
		mAddAccountFragment.show(ft, "add_account_dialog");
	}

	@Override
	public void onClick(View v) {		
		addAccount(mAddAccountFragment.getEnteredName());
		mAddAccountFragment.dismiss();
	}
	
	private class AccountsCursorAdapter extends SimpleCursorAdapter {
		public AccountsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			// perform the default binding
			super.bindView(v, context, cursor);

			// add a summary of transactions to the account view
			TextView summary = (TextView) v
					.findViewById(R.id.transactions_summary);
			Account acc = new AccountsDbAdapter(context)
					.buildAccountInstance(cursor);
			double balance = acc.getBalance();
			int count = acc.getTransactionCount();
			String statement = "";
			if (count == 0) {
				statement = "No transactions on this account";
			} else {
				String pluralizedText = count != 1 ? " transactions totalling "
						: " transaction totalling ";

				// TODO: Allow the user to set locale, or get it from phone
				// location
				NumberFormat currencyformatter = NumberFormat
						.getCurrencyInstance(Locale.getDefault());

				String formattedAmount = currencyformatter.format(balance);
				statement = count + pluralizedText + formattedAmount;
			}
			summary.setText(statement);
		}
	}

	private static final class AccountsCursorLoader extends DatabaseCursorLoader {
		AccountsDbAdapter accountsDbAdapter;

		public AccountsCursorLoader(Context context) {
			super(context);
			accountsDbAdapter = new AccountsDbAdapter(context);
		}

		@Override
		public Cursor loadInBackground() {
			return accountsDbAdapter.fetchAllAccounts();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new AccountsCursorLoader(this.getActivity()
				.getApplicationContext());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loaderCursor, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
		mCursorAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mCursorAdapter.swapCursor(null);
	}

}
