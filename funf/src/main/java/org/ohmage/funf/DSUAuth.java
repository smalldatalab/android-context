package org.ohmage.funf;

import android.accounts.Account;

/**
 * Created by changun on 3/12/15.
 */
public class DSUAuth {
    public static final String ACCOUNT_TYPE = "io.smalldata.dsu";
    public static final String ACCOUNT_NAME = "Context";
    public static final String ACCESS_TOKEN_TYPE = "access_token";
    public static final String REFRESH_TOKEN_TYPE = "refresh_token";
    public  static final Account ACCOUNT = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
}
