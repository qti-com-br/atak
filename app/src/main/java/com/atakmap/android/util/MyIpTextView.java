
package com.atakmap.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.atakmap.app.ATAKActivity;
import com.atakmap.app.R;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.comms.NetworkUtils.NetRestartNotification;
import com.virgilsystems.qtoken.VINBridgeCPP;

public class MyIpTextView extends TextView {

    private final Context ctx;

    public MyIpTextView(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        init();
    }

    public MyIpTextView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.ctx = ctx;
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {

        final String PREFACE = ctx.getString(R.string.my_ip);

        setText(PREFACE + NetworkUtils.getIP() +
                "  P: " + VINBridgeCPP.DEFAULT_NODE_PORT +
                "  R: " + VINBridgeCPP.DEFAULT_RECEIPT_PORT);

        NetworkUtils.registerNetRestartNotification(
                NetworkUtils.ALL_CONNECTIONS, new NetRestartNotification() {
                    @Override
                    public void onNetRestartNotification(
                            final String newAddress, int newMask) {
                        ((Activity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setText(PREFACE + newAddress);
                            }
                        });
                    }
                });
    }

}