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

public class Main extends android.app.Activity
  {
  /* worth comparing the relative complexity of setting up the calculation rules
    here in Java versus the Python version at <https://github.com/ldo/screencalc> */

    static enum FieldName
      {
        HeightMeasure("height"),
        WidthMeasure("width"),
        DiagMeasure("diagonal"),
        PixelDensity("density"),
        AspectRatio("aspect"),
        HeightPixels("heightpx"),
        WidthPixels("widthpx");

        public final String Name;

        private FieldName
          (
            String Name
          )
          {
            this.Name = Name.intern();
          } /*FieldName*/

      } /*FieldName*/;

    static final double cm_per_in = 2.54;

    static enum Units
      {
        UNITS_CM,
        UNITS_IN,
      };

    static double AspectDiag
      (
        double Aspect
      )
      /* returns the ratio of the diagonal to the width. */
      {
        return
            Math.hypot(1.0, Aspect);
      } /*AspectDiag*/

    interface Parser
      {

        public double Parse
          (
            String s
          );

      } /*Parser*/;

    static class ParseInt implements Parser
      {

        public double Parse
          (
            String s
          )
          {
            return
                Integer.parseInt(s);
          } /*Parse*/

      } /*ParseInt*/;

    class ParseDensity implements Parser
      {

        public double Parse
          (
            String s
          )
          /* always returns dots per cm. */
          {
            boolean IsDPI = CurUnits == Units.UNITS_IN; /* default */
            s = s.toLowerCase();
            if (s.endsWith("dpcm"))
              {
                IsDPI = false;
                s = s.substring(0, s.length() - 4);
              }
            else if (s.endsWith("dpi"))
              {
                IsDPI = true;
                s = s.substring(0, s.length() - 3);
              } /*if*/
            return
                Double.parseDouble(s) / (IsDPI ? cm_per_in : 1.0);
          } /*Parse*/

      } /*ParseDensity*/;

    class ParseMeasure implements Parser
      {
        private class Unit
          {
            public final String Name;
            public final double Multiplier;

            public Unit
              (
                String Name,
                double Multiplier
              )
              {
                this.Name = Name;
                this.Multiplier = Multiplier;
              } /*Unit*/

          } /*Unit*/;

        public double Parse
          (
            String s
          )
          {
            double Multiplier = CurUnits == Units.UNITS_CM ? 1.0 : cm_per_in;
            s = s.toLowerCase();
            for
              (
                Unit This :
                    new Unit[]
                        {
                            new Unit("cm", 1.0),
                            new Unit("mm", 0.1),
                            new Unit("in", cm_per_in),
                        }
              )
              {
                if (s.endsWith(This.Name))
                  {
                    s = s.substring(0, s.length() - This.Name.length());
                    Multiplier = This.Multiplier;
                    break;
                  } /*if*/
              } /*for*/
            return
                Double.parseDouble(s) * Multiplier;
          } /*Parse*/

      } /*ParseMeasure*/;

    static class ParseRatio implements Parser
      {

        public double Parse
          (
            String s
          )
          {
            final int SepPos = s.indexOf(":");
            final double Result;
            if (SepPos >= 0)
              {
                final double Numer = Integer.parseInt(s.substring(0, SepPos));
                final double Denom = Integer.parseInt(s.substring(SepPos + 1, s.length()));
                Result = Numer / Denom;
              }
            else
              {
                Result = Double.parseDouble(s);
              } /*if*/
            return
                Result;
          } /*Parse*/

      } /*ParseRatio*/;

    interface CalcFunction
      {

        public double Calculate
          (
            double[] Args
          );

      } /*CalcFunction*/;

    static class ParamDef
      {
        public static enum ParamTypes
          {
            TYPE_RATIO,
            TYPE_MEASURE,
            TYPE_PIXELS,
            TYPE_DENSITY,
          };
        public final ParamTypes Type;
        public final Parser Parse;
        public final HashMap<FieldName[], CalcFunction> Calculate = new HashMap<FieldName[], CalcFunction>();

        public static class Entry
          {
            public final FieldName[] ArgNames;
            public final CalcFunction Calc;

            public Entry
              (
                FieldName[] ArgNames,
                CalcFunction Calc
              )
              {
                this.ArgNames = ArgNames;
                this.Calc = Calc;
              } /*Entry*/

          } /*Entry*/;

        public ParamDef
          (
            ParamTypes Type,
            Parser Parse,
            Entry[] Calculate
          )
          {
            this.Type = Type;
            this.Parse = Parse;
            for (Entry ThisEntry : Calculate)
              {
                this.Calculate.put(ThisEntry.ArgNames, ThisEntry.Calc);
              } /*for*/
          } /*ParamDef*/

      } /*ParamDef*/;

    final HashMap<FieldName, ParamDef> ParamDefs = new HashMap<FieldName, ParamDef>();
      {
        ParamDefs.put
          (
            FieldName.HeightMeasure,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_MEASURE,
                /*Parse =*/ new ParseMeasure(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.DiagMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / AspectDiag(Args[0]) * Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.PixelDensity, FieldName.HeightPixels},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.DiagMeasure, FieldName.WidthMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Math.sqrt(Args[0] * Args[0] - Args[1] * Args[1]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.WidthMeasure,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_MEASURE,
                /*Parse =*/ new ParseMeasure(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.DiagMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / AspectDiag(Args[0]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.PixelDensity, FieldName.WidthPixels},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.DiagMeasure, FieldName.HeightMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Math.sqrt(Args[0] * Args[0] - Args[1] * Args[1]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.DiagMeasure,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_MEASURE,
                /*Parse =*/ new ParseMeasure(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.HeightMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / Args[0] * AspectDiag(Args[0]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.WidthMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] * AspectDiag(Args[0]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.HeightMeasure, FieldName.WidthMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Math.hypot(Args[0], Args[1]);
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.HeightPixels,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_PIXELS,
                /*Parse =*/ new ParseInt(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.PixelDensity, FieldName.HeightMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] * Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.WidthPixels,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_PIXELS,
                /*Parse =*/ new ParseInt(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.PixelDensity, FieldName.WidthMeasure},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] * Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.PixelDensity,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_DENSITY,
                /*Parse =*/ new ParseDensity(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.HeightMeasure, FieldName.HeightPixels},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.WidthMeasure, FieldName.WidthPixels},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[1] / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
        ParamDefs.put
          (
            FieldName.AspectRatio,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_RATIO,
                /*Parse =*/ new ParseRatio(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.HeightPixels, FieldName.WidthPixels},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            Args[0] / Args[1];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
          );
      }

    final int[] UnitsButtons = new int[] {R.id.units_cm, R.id.units_in};
    Units CurUnits = Units.UNITS_CM; /* no relevant locale setting? */

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

    public String FormatField
      (
        FieldName Name,
        double FieldValue
      )
      {
        double Multiplier = CurUnits == Units.UNITS_CM ? 1.0 : 1.0 / cm_per_in;
        String Suffix = "";
        String Format = "%.2f";
        switch (ParamDefs.get(Name).Type)
          {
        case TYPE_RATIO:
            Multiplier = 1.0;
        break;
        case TYPE_MEASURE:
            Suffix = CurUnits == Units.UNITS_CM ? "cm" : "in";
        break;
        case TYPE_PIXELS:
            Format = "%.0f";
            Multiplier = 1.0;
        break;
        case TYPE_DENSITY:
            Format = "%.1f";
            switch (CurUnits)
              {
            case UNITS_CM:
                Suffix = "dpcm";
            break;
            case UNITS_IN:
                Multiplier = cm_per_in;
                Suffix = "dpi";
            break;
              } /*switch*/
        break;
          } /*switch*/
        return
            String.format(Format, FieldValue * Multiplier) + Suffix;
      } /*FormatField*/

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
        SetField(Name, FieldState.States.STATE_VALID, FormatField(Name, NewValue));
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

    @Override
    public void onCreate
      (
        android.os.Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.main);
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
                        CurUnits = UnitsID == R.id.units_cm ? Units.UNITS_CM : Units.UNITS_IN;
                      } /*onClick*/
                  } /*View.OnClickListener*/
              );
            if (ToRestore == null)
              {
                ThisButton.setChecked
                  (
                    UnitsID == (CurUnits == Units.UNITS_CM ? R.id.units_cm : R.id.units_in)
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
                                FieldValue = ParamDefs.get(Name).Parse.Parse(FieldStr);
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
                    for (;;)
                      {
                        boolean DidSomething = false;
                        boolean LeftUndone = false;
                        for (FieldName Name : FieldName.values())
                          {
                            if (!Known.containsKey(Name))
                              {
                                final ParamDef ThisParam = ParamDefs.get(Name);
                                boolean DidThis = false;
                                for (FieldName[] ArgNames : ThisParam.Calculate.keySet())
                                  {
                                    boolean GotAll = true; /* to begin with */
                                    for (FieldName ArgName : ArgNames)
                                      {
                                        if (!Known.containsKey(ArgName))
                                          {
                                            GotAll = false;
                                            break;
                                          } /*if*/
                                      } /*for*/
                                    if (GotAll)
                                      {
                                        final double[] Args = new double[ArgNames.length];
                                        for (int i = 0; i < ArgNames.length; ++i)
                                          {
                                            Args[i] = Known.get(ArgNames[i]);
                                          } /*for*/
                                        final double FieldValue =
                                            ThisParam.Calculate.get(ArgNames).Calculate(Args);
                                        Known.put(Name, FieldValue);
                                        SetValid(Name, FieldValue);
                                        DidThis = true;
                                        break;
                                      } /*if*/
                                  } /*for*/
                                if (DidThis)
                                  {
                                    DidSomething = true;
                                  }
                                else
                                  {
                                    LeftUndone = true;
                                  } /*if*/
                              } /*if*/
                          } /*for*/
                        if (!LeftUndone)
                            break;
                        if (!DidSomething)
                          {
                            android.widget.Toast.makeText
                              (
                                /*context =*/ Main.this,
                                /*text =*/ R.string.calc_incomplete,
                                /*duration =*/ android.widget.Toast.LENGTH_SHORT
                              ).show();
                            break;
                          } /*if*/
                      } /*for*/
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
        ToSave.putBoolean("CurUnits", CurUnits == Units.UNITS_CM);
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
        CurUnits = ToRestore.getBoolean("CurUnits") ? Units.UNITS_CM : Units.UNITS_IN;
        for (int UnitsID : UnitsButtons)
          {
            ((android.widget.RadioButton)findViewById(UnitsID)).setChecked
              (
                UnitsID == (CurUnits == Units.UNITS_CM ? R.id.units_cm : R.id.units_in)
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

  } /*Main*/;
