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

package org.gnucash.android.ui.accounts;

import java.util.Currency;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.ui.transactions.TransactionsActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.util.OnAccountClickedListener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the list of accounts and summary of transactions
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 * 
 */
public class AccountsActivity extends SherlockFragmentActivity implements OnAccountClickedListener {

	public static final String FRAGMENT_ACCOUNTS_LIST 	= "accounts_list";
	
	public static String DEFAULT_CURRENCY_CODE 	= "USD";
	static final int DIALOG_ADD_ACCOUNT 		= 0x01;

	protected static final String TAG = "AccountsActivity";	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accounts);

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		String currencyCode = prefs.getString(getString(R.string.pref_default_currency), Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		DEFAULT_CURRENCY_CODE = currencyCode;		
		
		FragmentManager fragmentManager = getSupportFragmentManager();

		AccountsListFragment accountsListFragment = (AccountsListFragment) fragmentManager
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);

		if (accountsListFragment == null) {
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			fragmentTransaction.add(R.id.fragment_container,
					new AccountsListFragment(), FRAGMENT_ACCOUNTS_LIST);

			fragmentTransaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.global_actions, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	        FragmentManager fm = getSupportFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        }
	        return true;

		default:
			return false;
		}
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		// TODO Auto-generated method stub
		super.onActivityResult(arg0, arg1, arg2);
	}
	
	/**
	 * Opens a dialog fragment to create a new account
	 * @param v View which triggered this callback
	 */
	public void onNewAccountClick(View v) {
		AccountsListFragment accountFragment = (AccountsListFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);
		if (accountFragment != null)
			accountFragment.showAddAccountDialog(0);
	}

	@Override
	public void accountSelected(long accountRowId, String accountName) {
		Intent intent = new Intent(this, TransactionsActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);
		intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_NAME, accountName);
		
		startActivity(intent);
	}
	
	

}