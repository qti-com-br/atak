
package com.atakmap.android.bloodhound;

import android.os.Bundle;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.app.R;

public class BloodHoundPreferenceFragment extends AtakPreferenceFragment {

    public BloodHoundPreferenceFragment() {
        super(R.xml.bloodhound_preferences, R.string.bloodhoundPreferences);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.toolPreferences),
                getSummary());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.addPreferencesFromResource(getResourceID());
        ((PanEditTextPreference) findPreference("bloodhound_flash_eta"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("bloodhound_outer_eta"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("bloodhound_inner_eta"))
                .checkValidInteger();
    }

}
