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

package org.gnucash.android.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Account.AccountType;
import org.gnucash.android.data.Transaction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Manages persistence of {@link Account}s in the database
 * Handles adding, modifying and deleting of account records.
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class AccountsDbAdapter extends DatabaseAdapter {

	/**
	 * Transactions database adapter for manipulating transactions associated with accounts
	 */
	private TransactionsDbAdapter mTransactionsAdapter;
	
	/**
	 * Creates new instance
	 * @param context Application context
	 */
	public AccountsDbAdapter(Context context) {
		super(context);
		mTransactionsAdapter = new TransactionsDbAdapter(context);
	}
	
	@Override
	public void close() {		
		super.close();
		mTransactionsAdapter.close();
	}
	
	/**
	 * Adds an account to the database. 
	 * If an account already exists in the database with the same unique ID, 
	 * then just update that account. 
	 * @param account {@link Account} to be inserted to database
	 * @return Database row ID of the inserted account
	 */
	public long addAccount(Account account){
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseHelper.KEY_NAME, account.getName());
		contentValues.put(DatabaseHelper.KEY_TYPE, account.getAccountType().name());
		contentValues.put(DatabaseHelper.KEY_UID, account.getUID());
		
		long rowId = -1;
		if ((rowId = fetchAccountWithUID(account.getUID())) > 0){
			//if account already exists, then just update
			Log.d(TAG, "Updating existing account");
			mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME, contentValues, DatabaseHelper.KEY_ROW_ID + " = " + rowId, null);
		} else {
			Log.d(TAG, "Adding new account to db");
			rowId = mDb.insert(DatabaseHelper.ACCOUNTS_TABLE_NAME, null, contentValues);
		}
		
		//now add transactions if there are any
		if (rowId > 0){
			for (Transaction t : account.getTransactions()) {
				mTransactionsAdapter.addTransaction(t);
			}
		}
		return rowId;
	}
	
	/**
	 * Deletes an account with database id <code>rowId</code>
	 * All the transactions in the account will also be deleted
	 * @param rowId Database id of the account record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean destructiveDeleteAccount(long rowId){
		Log.d(TAG, "Delete account with rowId: " + rowId);
		boolean result = false;
		//first remove all transactions for the account
		Cursor c = mTransactionsAdapter.fetchAllTransactionsForAccount(rowId);
		if (c == null)
			return result; 
		
		while (c.moveToNext()){
			long id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			result &= mTransactionsAdapter.deleteTransaction(id);
		}
		result &= deleteRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
		return result;
	}
	
	/**
	 * Deletes an account while preserving the linked transactions
	 * Reassigns all transactions belonging to the account with id <code>rowId</code> to 
	 * the account with id <code>accountReassignId</code> before deleting the account.
	 * @param rowIdToDelete
	 * @param accountReassignId
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean transactionPreservingDelete(long rowIdToDelete, long accountReassignId){
		Cursor transactionsCursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ACCOUNT_UID}, 
				DatabaseHelper.KEY_ACCOUNT_UID + " = " + rowIdToDelete, 
				null, null, null, null);
		if (transactionsCursor != null && transactionsCursor.getCount() > 0){
			Log.d(TAG, "Found transactions. Migrating to new account");
			ContentValues contentValues = new ContentValues();
			contentValues.put(DatabaseHelper.KEY_ACCOUNT_UID, accountReassignId);
			mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
					contentValues, 
					DatabaseHelper.KEY_ACCOUNT_UID + "=" + rowIdToDelete, 
					null);
			transactionsCursor.close();
		}
		return destructiveDeleteAccount(rowIdToDelete);
	}
	
	/**
	 * Builds an account instance with the provided cursor.
	 * The cursor should already be pointing to the account record in the database
	 * @param c Cursor pointing to account record in database
	 * @return {@link Account} object constructed from database record
	 */
	public Account buildAccountInstance(Cursor c){
		Account account = new Account(c.getString(DatabaseAdapter.COLUMN_NAME));
		String uid = c.getString(DatabaseAdapter.COLUMN_UID);
		account.setUID(uid);
		account.setAccountType(AccountType.valueOf(c.getString(DatabaseAdapter.COLUMN_TYPE)));
		account.setTransactions(mTransactionsAdapter.getAllTransactionsForAccount(uid));
		
		return account;
	}
		
	/**
	 * Fetch an account from the database which has a unique ID <code>uid</code>
	 * @param uid Unique Identifier of account to be retrieved
	 * @return Database row ID of account with UID <code>uid</code>
	 */
	public long fetchAccountWithUID(String uid){
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_UID + " = '" + uid + "'", null, null, null, null);
		long result = -1;
		if (cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "Account already exists. Returning existing id");
			result = cursor.getLong(0); //0 because only one row was requested

			cursor.close();
		}
		return result;
	}
	
	/**
	 * Retrieves an account object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the account record to be retrieved
	 * @return {@link Account} object corresponding to database record
	 */
	public Account getAccount(long rowId){
		Account account = null;
		Log.v(TAG, "Fetching account with id " + rowId);
		Cursor c =	fetchRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
		if (c != null && c.moveToFirst()){
			account = buildAccountInstance(c);	
			c.close();
		}
		return account;
	}
	
	/**
	 * Returns the {@link Account} object populated with data from the database
	 * for the record with UID <code>uid</code>
	 * @param uid Unique ID of the account to be retrieved
	 * @return {@link Account} object for unique ID <code>uid</code>
	 */
	public Account getAccount(String uid){
		return getAccount(getId(uid));
	}	
	
	/**
	 * Returns a list of all account objects in the system
	 * @return List of {@link Account}s in the database
	 */
	public List<Account> getAllAccounts(){
		ArrayList<Account> accounts = new ArrayList<Account>();
		Cursor c = fetchAllAccounts();
		
		if (c == null)
			return accounts;
		
		while(c.moveToNext()){
			accounts.add(buildAccountInstance(c));
		}
		c.close();
		return accounts;
	}
	
	public List<Account> getExportableAccounts(){
		List<Account> accountsList = getAllAccounts();
		Iterator<Account> it = accountsList.iterator();
		
		while (it.hasNext()){
			Account account = it.next();
			
			if (account.hasUnexportedTransactions() == false)
				it.remove();
		}
		return accountsList;
	}
	
	/**
	 * Returns a cursor to all account records in the database
	 * @return {@link Cursor} to all account records
	 */
	public Cursor fetchAllAccounts(){
		Log.v(TAG, "Fetching all accounts from db");
		return fetchAllRecords(DatabaseHelper.ACCOUNTS_TABLE_NAME);
	}

	/**
	 * Return the record ID for the account with UID <code>accountUID</code>
	 * @param accountUID String Unique ID of the account
	 * @return Record ID belonging to account UID
	 */
	public long getId(String accountUID){
		long id = -1;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_UID + "='" + accountUID + "'", 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			c.close();
		}
		return id;
	}
	
	/**
	 * Deletes all accounts and their transactions from the database
	 */
	public void deleteAllAccounts(){
		mDb.delete(DatabaseHelper.ACCOUNTS_TABLE_NAME, null, null);
		mDb.delete(DatabaseHelper.TRANSACTIONS_TABLE_NAME, null, null);
	}
}
