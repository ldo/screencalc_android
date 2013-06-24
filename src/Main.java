package nz.gen.geek_central.screencalc;
/*
    Calculator for parameters for a display screen: given any of height,
    width or diagonal in distance units, aspect ratio, pixel density,
    or height or width in pixels, try to calculate the rest.

    Copyright 2013 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    This program is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see
    <http://www.gnu.org/licenses/>.
*/

import java.util.HashMap;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import static nz.gen.geek_central.screencalc.Rules.FieldName;
import static nz.gen.geek_central.screencalc.Rules.Units;

public class Main extends android.app.Activity
  {
    android.text.ClipboardManager Clipboard;

    final Rules CurRules = new Rules();
    final int[] UnitsButtons = new int[] {R.id.units_cm, R.id.units_in};

    static class FieldDef
      {
        public final int FieldID, ClearButtonID;

        public FieldDef
          (
            int FieldID,
            int ClearButtonID
          )
          {
            this.FieldID = FieldID;
            this.ClearButtonID = ClearButtonID;
          } /*FieldDef*/

      } /*FieldDef*/;

    static final HashMap<FieldName, FieldDef> FieldDefs = new HashMap<FieldName, FieldDef>();
      {
        FieldDefs.put
          (
            FieldName.HeightMeasure,
            new FieldDef(R.id.height_measure, R.id.clear_height_measure)
          );
        FieldDefs.put
          (
            FieldName.WidthMeasure,
            new FieldDef(R.id.width_measure, R.id.clear_width_measure)
          );
        FieldDefs.put
          (
            FieldName.DiagMeasure,
            new FieldDef(R.id.diag_measure, R.id.clear_diag_measure)
          );
        FieldDefs.put
          (
            FieldName.PixelDensity,
            new FieldDef(R.id.pixel_density, R.id.clear_pixel_density)
          );
        FieldDefs.put
          (
            FieldName.AspectRatio,
            new FieldDef(R.id.aspect_ratio, R.id.clear_aspect_ratio)
          );
        FieldDefs.put
          (
            FieldName.HeightPixels,
            new FieldDef(R.id.height_pixels, R.id.clear_height_pixels)
          );
        FieldDefs.put
          (
            FieldName.WidthPixels,
            new FieldDef(R.id.width_pixels, R.id.clear_width_pixels)
          );
      }

    private static class FieldState
      {
        public static enum States
          {
            STATE_INPUT(0),
            STATE_VALID(1),
            STATE_ERROR(2);

            public final int Val;

            private States
              (
                int Val
              )
              {
                this.Val = Val;
              } /*States*/

            public static States ToState
              (
                int Val
              )
              {
                States Result = null;
                for (int i = 0;;)
                  {
                    if (values()[i].Val == Val)
                      {
                        Result = values()[i];
                        break;
                      } /*if*/
                    ++i;
                  } /*for*/
                return
                    Result;
              } /*ToState*/

          } /*States*/;

        public final States State;
        public final String Value;

        public FieldState
          (
            States State,
            String Value
          )
          {
            this.State = State;
            this.Value = Value;
          } /*FieldState*/

      } /*FieldState*/;

    final HashMap<FieldName, FieldState> FieldStates = new HashMap<FieldName, FieldState>();

    private int ColorValidValue, ColorUnknownValue, ColorErrorValue;

    private void SetField
      (
        FieldName Name,
        FieldState.States NewState,
        String NewValue /* only for STATE_VALID */
      )
      {
        final FieldDef TheField = FieldDefs.get(Name);
        final EditText EditField = (EditText)findViewById(TheField.FieldID);
        int FieldColor = ColorErrorValue;
        switch (NewState)
          {
        case STATE_INPUT:
            NewValue = "";
            EditField.setText("");
            FieldColor = ColorUnknownValue;
        break;
        case STATE_VALID:
            EditField.setText(NewValue);
            FieldColor = ColorValidValue;
        break;
        case STATE_ERROR:
            NewValue = EditField.getText().toString(); /* keep existing value */
            FieldColor = ColorErrorValue;
        break;
          } /*switch*/
        EditField.setBackgroundColor(FieldColor);
        EditField.setFocusable(NewState != FieldState.States.STATE_VALID);
        EditField.setFocusableInTouchMode(NewState != FieldState.States.STATE_VALID);
        findViewById(TheField.ClearButtonID)
            .setVisibility
              (
                NewState == FieldState.States.STATE_VALID ? View.VISIBLE : View.INVISIBLE
              );
        FieldStates.put(Name, new FieldState(NewState, NewValue));
      } /*SetField*/

    private void SetUnknown
      (
        FieldName Name
      )
      {
        SetField(Name, FieldState.States.STATE_INPUT, null);
      } /*SetUnknown*/

    private void SetValid
      (
        FieldName Name,
        double NewValue
      )
      {
        SetField(Name, FieldState.States.STATE_VALID, CurRules.FormatField(Name, NewValue));
      } /*SetValid*/

    private void SetValid
      (
        FieldName Name,
        String ValueStr
      )
      {
        SetField(Name, FieldState.States.STATE_VALID, ValueStr);
      } /*SetValid*/

    private void SetError
      (
        FieldName Name
      )
      {
        SetField(Name, FieldState.States.STATE_ERROR, null);
      } /*SetError*/

    private class FieldClearAction implements View.OnClickListener
      {
        final FieldName Field;

        public FieldClearAction
          (
            FieldName Field
          )
          {
            this.Field = Field;
          } /*FieldClearAction*/

        public void onClick
          (
            View ClearButton
          )
          {
            SetUnknown(Field);
          } /*onClick*/

      } /*FieldClearAction*/;

    private void ClearAll()
      {
        for (FieldName Field : FieldName.values())
          {
            SetUnknown(Field);
          } /*for*/
      } /*ClearAll*/

    java.util.Map<android.view.MenuItem, Runnable> ContextMenu;

    @Override
    public void onCreateContextMenu
      (
        android.view.ContextMenu TheMenu,
        View TheView,
        android.view.ContextMenu.ContextMenuInfo TheMenuInfo
      )
      {
        final EditText TheText = (EditText)TheView;
        final boolean Writeable = TheView.isFocusable();
        ContextMenu = new java.util.HashMap<android.view.MenuItem, Runnable>();
        if (Writeable)
          {
            ContextMenu.put
              (
                TheMenu.add(R.string.cut),
                new Runnable()
                  {
                    public void run()
                      {
                        Clipboard.setText(TheText.getText().toString());
                        TheText.setText("");
                      } /*run*/
                  } /*Runnable*/
              );
          } /*if*/
        ContextMenu.put
          (
            TheMenu.add(R.string.copy),
            new Runnable()
              {
                public void run()
                  {
                    Clipboard.setText(TheText.getText().toString());
                  } /*run*/
              } /*Runnable*/
          );
        if (Writeable)
          {
            ContextMenu.put
              (
                TheMenu.add(R.string.paste),
                new Runnable()
                  {
                    public void run()
                      {
                        final CharSequence ToPaste = Clipboard.getText();
                        if (ToPaste != null)
                          {
                            TheText.setText(ToPaste.toString());
                          }
                        else
                          {
                            android.widget.Toast.makeText
                              (
                                /*context =*/ Main.this,
                                /*text =*/ R.string.no_text_on_clipboard,
                                /*duration =*/ android.widget.Toast.LENGTH_SHORT
                              ).show();
                          } /*if*/
                      } /*run*/
                  } /*Runnable*/
              );
          } /*if*/
      } /*onCreateContextMenu*/

    @Override
    public void onCreate
      (
        android.os.Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.main);
        Clipboard = (android.text.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
          {
            final android.content.res.Resources Res = getResources();
            ColorValidValue = Res.getColor(R.color.valid_value);
            ColorUnknownValue = Res.getColor(R.color.unknown_value);
            ColorErrorValue = Res.getColor(R.color.error_value);
          }
          {
            final android.widget.AutoCompleteTextView Aspect =
                (android.widget.AutoCompleteTextView)findViewById(R.id.aspect_ratio);
            Aspect.setAdapter
              (
                android.widget.ArrayAdapter.createFromResource
                  (
                    /*context =*/ this,
                    /*textArrayResId =*/ R.array.common_aspect_ratios,
                    /*textViewResId*/ android.R.layout.simple_dropdown_item_1line
                  )
              );
            Aspect.setThreshold(1);
          }
        for (FieldName Name : FieldName.values())
          {
            registerForContextMenu(findViewById(FieldDefs.get(Name).FieldID));
            findViewById(FieldDefs.get(Name).ClearButtonID)
                .setOnClickListener(new FieldClearAction(Name));
          } /*for*/
        for (final int UnitsID : UnitsButtons)
          {
            final android.widget.RadioButton ThisButton =
                (android.widget.RadioButton)findViewById(UnitsID);
            ThisButton.setOnClickListener
              (
                new View.OnClickListener()
                  {
                    public void onClick
                      (
                        View TheButton
                      )
                      {
                        CurRules.CurUnits = UnitsID == R.id.units_cm ? Units.UNITS_CM : Units.UNITS_IN;
                      } /*onClick*/
                  } /*View.OnClickListener*/
              );
            if (ToRestore == null)
              {
                ThisButton.setChecked
                  (
                    UnitsID == (CurRules.CurUnits == Units.UNITS_CM ? R.id.units_cm : R.id.units_in)
                  );
              } /*if*/
          } /*for*/
        findViewById(R.id.clear_all).setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheButton
                  )
                  {
                    ClearAll();
                  } /*onClick*/
              } /*View.OnClickListener*/
          );
        findViewById(R.id.calculate).setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheButton
                  )
                  {
                    final HashMap<FieldName, Double> Known = new HashMap<FieldName, Double>();
                    for (FieldName Name : FieldName.values())
                      {
                        Double FieldValue = null;
                        final String FieldStr =
                            ((TextView)findViewById(FieldDefs.get(Name).FieldID)).getText().toString();
                        if (FieldStr.length() != 0)
                          {
                            try
                              {
                                FieldValue = CurRules.ParamDefs.get(Name).Parse.Parse(FieldStr);
                              }
                            catch (NumberFormatException Bad)
                              {
                                SetError(Name);
                              } /*try*/
                          }
                        else
                          {
                            SetUnknown(Name);
                          } /*if*/
                        if (FieldValue != null)
                          {
                            SetValid(Name, FieldValue);
                            Known.put(Name, FieldValue);
                          } /*if*/
                      } /*for*/
                    final java.util.HashSet<FieldName> Computed = new java.util.HashSet<FieldName>();
                    final Rules.ComputeStatus Status = CurRules.ComputeParams(Known, Computed);
                    for (FieldName Name : Computed)
                      {
                        SetValid(Name, Known.get(Name));
                      } /*for*/
                    if (Status != Rules.ComputeStatus.COMPUTE_DONE)
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/ R.string.calc_incomplete,
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      } /*if*/
                  } /*onClick*/
              } /*View.OnClickListener*/
          );
        if (ToRestore == null)
          {
            ClearAll();
          } /*if*/
      } /*onCreate*/

    @Override
    public void onSaveInstanceState
      (
        android.os.Bundle ToSave
      )
      {
        ToSave.putBoolean("CurUnits", CurRules.CurUnits == Units.UNITS_CM);
        for (FieldName Name : FieldName.values())
          {
            final FieldState ThisField = FieldStates.get(Name);
            ToSave.putInt(Name.Name + ".state", ThisField.State.Val);
            ToSave.putString(Name.Name + ".value", ThisField.Value);
          } /*for*/
        super.onSaveInstanceState(ToSave);
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        android.os.Bundle ToRestore
      )
      {
        super.onRestoreInstanceState(ToRestore);
        CurRules.CurUnits = ToRestore.getBoolean("CurUnits") ? Units.UNITS_CM : Units.UNITS_IN;
        for (int UnitsID : UnitsButtons)
          {
            ((android.widget.RadioButton)findViewById(UnitsID)).setChecked
              (
                UnitsID == (CurRules.CurUnits == Units.UNITS_CM ? R.id.units_cm : R.id.units_in)
              );
          } /*for*/
        for (FieldName Name : FieldName.values())
          {
            final String FieldValue =
                ToRestore.containsKey(Name.Name + ".value") ?
                    ToRestore.getString(Name.Name + ".value")
                :
                    "";
            SetField
              (
                Name,
                ToRestore.containsKey(Name.Name + ".state") ?
                    FieldState.States.ToState(ToRestore.getInt(Name.Name + ".state"))
                :
                    FieldState.States.STATE_INPUT,
                FieldValue
              );
          } /*for*/
      } /*onRestoreInstanceState*/

    @Override
    public boolean onContextItemSelected
      (
        android.view.MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = ContextMenu.get(TheItem);
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onContextItemSelected*/

  } /*Main*/;
