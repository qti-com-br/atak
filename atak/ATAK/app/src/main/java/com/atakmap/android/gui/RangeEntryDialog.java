
package com.atakmap.android.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.graphics.Color;

import com.atakmap.android.util.SimpleItemSelectedListener;

import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import java.text.DecimalFormat;

/**
 * Dialog which allows user to hand-jam a range value in a variety of units
 */
public class RangeEntryDialog implements DialogInterface.OnDismissListener {

    private static final DecimalFormat DEC_FORMAT_2 = LocaleUtil
            .getDecimalFormat("#.##");

    private final MapView _mapView;
    private final Context _context;
    private final LayoutInflater _inflater;
    private AlertDialog _dialog;
    private View _view;
    private Span _unit;
    private Callback _callback;

    /**
     * Instantiate the range entry dialog
     * @param mapView the mapView for using the dialog
     */
    public RangeEntryDialog(MapView mapView) {
        _mapView = mapView;
        _context = mapView.getContext();
        _inflater = LayoutInflater.from(_context);
    }

    /**
     * Open the range entry dialog
     * @param title Dialog title
     * @param valueM The initial range value in meters
     * @param unit The initial unit
     * @param callback Callback when OK is selected
     */
    public void show(String title, double valueM, Span unit,
            Callback callback) {
        dismiss();
        _view = _inflater.inflate(R.layout.drawing_distance_input,
                _mapView, false);

        _unit = unit;
        _callback = callback;

        final EditText input = _view.findViewById(
                R.id.drawingDistanceInput);
        input.setText(DEC_FORMAT_2.format(SpanUtilities.convert(valueM,
                Span.METER, unit)));
        input.setSelection(input.getText().length());

        final Spinner units = _view.findViewById(
                R.id.drawingDistanceUnits);

        final ArrayAdapter<Span> adapter = new ArrayAdapter<>(_context,
                android.R.layout.simple_spinner_item, Span.values());
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        units.setAdapter(adapter);
        units.setSelection(unit.getValue());
        units.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                    int p, long id) {
                Span newUnit = adapter.getItem(p);
                if (newUnit == null)
                    newUnit = Span.METER;
                _unit = newUnit;
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(Color.WHITE);
            }

        });

        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        if (!FileSystemUtils.isEmpty(title))
            b.setTitle(title);
        b.setView(_view);
        b.setCancelable(false);
        b.setPositiveButton(R.string.ok, null);
        b.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideKeyboard();
                    }
                });
        _dialog = b.create();
        _dialog.show();
        _dialog.setOnDismissListener(this);
        _dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        double d;
                        try {
                            d = SpanUtilities.convert(input.getText()
                                    .toString(), _unit, Span.METER);
                        } catch (Exception e) {
                            Toast.makeText(_context, R.string.nineline_text103,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (_callback != null)
                            _callback.onSetValue(d, _unit);
                        hideKeyboard();
                        dismiss();
                    }
                });
        InputMethodManager imm = ((InputMethodManager) MapView.getMapView()
                .getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null)
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void show(int titleId, double valueM, Span unit,
            Callback callback) {
        show(_context.getString(titleId), valueM, unit, callback);
    }

    public void dismiss() {
        if (_dialog != null)
            _dialog.dismiss();
        _dialog = null;
        _callback = null;
    }

    private void hideKeyboard() {
        if (_view == null)
            return;
        InputMethodManager imm = ((InputMethodManager) MapView.getMapView()
                .getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null)
            imm.hideSoftInputFromWindow(_view.getWindowToken(), 0);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        _dialog = null;
    }

    public interface Callback {
        void onSetValue(double valueM, Span unit);
    }
}
