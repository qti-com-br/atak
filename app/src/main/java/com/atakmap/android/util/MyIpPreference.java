
package com.atakmap.android.util;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.atakmap.android.tools.menu.AtakActionBarListData;
import com.atakmap.android.tools.menu.AtakActionBarMenuData;
import com.atakmap.app.ATAKActivity;
import com.atakmap.app.R;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.comms.NetworkUtils.NetRestartNotification;
import com.virgilsystems.qtoken.VINBridgeCPP;

public class MyIpPreference extends Preference {

    private final Context ctx;
    private final String title;

    public MyIpPreference(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        title = ctx.getString(R.string.my_ip);

        init();
    }

    public MyIpPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.ctx = ctx;
        title = ctx.getString(R.string.my_ip);

        init();
    }

    // Initializer block
    public void init() {
        if (AtakActionBarListData.getOrientation(
                ctx) == AtakActionBarMenuData.Orientation.portrait) {
            setTitle(title);
            setSummary(getMyVINIP());
        } else
            setTitle(title + ": " + getMyVINIP());

        NetworkUtils.registerNetRestartNotification(
                NetworkUtils.ALL_CONNECTIONS, new NetRestartNotification() {
                    @Override
                    public void onNetRestartNotification(
                            final String newAddress, int newMask) {
                        ((Activity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (AtakActionBarListData
                                        .getOrientation(
                                                ctx) == AtakActionBarMenuData.Orientation.portrait) {
                                    setTitle(title);
                                    setSummary(getMyVINIP());
                                } else
                                    setTitle(title + ": "
                                            + getMyVINIP());
                            }
                        });
                    }
                });
    }

    private String getMyVINIP() {
        return NetworkUtils.getIP() +
                "  P: " + VINBridgeCPP.DEFAULT_NODE_PORT +
                "  R: " + VINBridgeCPP.DEFAULT_RECEIPT_PORT;
    }
}