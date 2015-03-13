package org.ohmage.funf;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by changun on 3/12/15.
 */
public class ConnectionStore {
    public static final String MOVES = "moves";
    public static final String DSU = "dsu";
    public static final String LIFESTREAMS = "lifestreams";
    private final SharedPreferences sharedPreferences;

    public ConnectionStore(Context cxt){
        sharedPreferences = cxt.getSharedPreferences("ConnectionStore", 0);
    }

    public boolean isConnectedWithMoves(){
        return sharedPreferences.getBoolean(MOVES, false);
    }
    public boolean isConnectedWithDSU(){
        return sharedPreferences.getBoolean(DSU, false);
    }
    public boolean isConnectedWithLifestreams(){
        return sharedPreferences.getBoolean(LIFESTREAMS, false);
    }

    public void setConnectedWithMoves(boolean val){
        sharedPreferences.edit().putBoolean(MOVES, val).apply();
    }
    public void setConnectedWithDSU(boolean val){
        sharedPreferences.edit().putBoolean(DSU, val).apply();
    }

    public void setConnectedWithLifestreams(boolean val){
        sharedPreferences.edit().putBoolean(LIFESTREAMS, val).apply();
    }

}
