package nz.gen.geek_central.screencalc;
/*
    Calculator for parameters for a display screen: given any of height,
    width or diagonal in distance units, aspect ratio, pixel density,
    or height or width in pixels, try to calculate the rest.

    This class defines the parameters and the rules for calculating
    them from each other. There should be nothing Android-specific
    in this source file.

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

public class Rules
  {
  /* worth comparing the relative complexity of setting up the calculation rules
    here in Java versus the Python version at <https://github.com/ldo/screencalc> */

    public Units CurUnits = Units.UNITS_CM; /* no relevant locale setting? */

    public static enum FieldName
      {
      /* all the valid screen parameters, plus useful string names for each */
        HeightMeasure("height"),
        WidthMeasure("width"),
        DiagMeasure("diagonal"),
        PixelDensity("density"),
        AspectRatio("aspect"),
        ViewingDistance("viewdist"),
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

    public static final double cm_per_in = 2.54;

    public static final double pixel_angle = Math.PI / 180 / 60;
      /* one arc-minute in radians, minimum angle between pixels
        distinguishable to typical human eye */

    public static final double acuity_factor = Math.sqrt(2) / Math.tan(pixel_angle);
      /* sqrt(2) comes in to account for greater pixel spacing diagonally */

    public static enum Units
      {
        UNITS_CM,
        UNITS_IN,
      };

    public static double AspectDiag
      (
        double Aspect
      )
      /* returns the ratio of the diagonal to the width. */
      {
        return
            Math.hypot(1.0, Aspect);
      } /*AspectDiag*/

    public interface Parser
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

    static class UnitEntry
      {
        public final String Name;
        public final double Multiplier;

        public UnitEntry
          (
            String Name,
            double Multiplier
          )
          {
            this.Name = Name;
            this.Multiplier = Multiplier;
          } /*UnitEntry*/

      } /*UnitEntry*/;

    class ParseMeasure implements Parser
      {

        private final java.util.Map<String, Double> AcceptableUnits;
        private final String DefaultUnitsSI, DefaultUnitsImp; /* optional */

        public ParseMeasure
          (
            UnitEntry[] AcceptableUnits,
            String DefaultUnitsSI,
            String DefaultUnitsImp
          )
          {
            this.AcceptableUnits = new java.util.HashMap<String, Double>();
            boolean ValidDefaultUnitsSI = DefaultUnitsSI == null;
            boolean ValidDefaultUnitsImp = DefaultUnitsImp == null;
            if (ValidDefaultUnitsSI != ValidDefaultUnitsImp)
              {
                throw new RuntimeException("either specify both or neither DefaultUnits");
              } /*if*/
            for (UnitEntry ThisUnit : AcceptableUnits)
              {
                if (!ValidDefaultUnitsSI && ThisUnit.Name.equals(DefaultUnitsSI))
                  {
                    ValidDefaultUnitsSI = true;
                  } /*if*/
                if (!ValidDefaultUnitsImp && ThisUnit.Name.equals(DefaultUnitsImp))
                  {
                    ValidDefaultUnitsImp = true;
                  } /*if*/
                this.AcceptableUnits.put(ThisUnit.Name, ThisUnit.Multiplier);
              } /*for*/
            if (!ValidDefaultUnitsSI || !ValidDefaultUnitsImp)
              {
                throw new RuntimeException
                  (
                    String.format
                      (
                        "invalid DefaultUnits “%s” or “%s”",
                        DefaultUnitsSI,
                        DefaultUnitsImp
                      )
                  );
              } /*if*/
            this.DefaultUnitsSI = DefaultUnitsSI;
            this.DefaultUnitsImp = DefaultUnitsImp;
          } /*ParseMeasure*/

        public double Parse
          (
            String s
          )
          {
            s = s.toLowerCase();
            final java.util.regex.Matcher MeasureMatch =
                java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d*)?|\\.\\d+)", 0).matcher(s);
            if (!MeasureMatch.find())
              {
                throw new NumberFormatException("invalid measure");
              } /*if*/
            final String Units = s.substring(MeasureMatch.end(1));
            final String DefaultUnits =
                Rules.this.CurUnits == Rules.Units.UNITS_CM ? DefaultUnitsSI : DefaultUnitsImp;
            final Double Multiplier;
            if (Units.length() != 0)
              {
                Multiplier = AcceptableUnits.get(Units);
                if (Multiplier == null)
                  {
                    throw new NumberFormatException
                      (
                        String.format("unrecognized units “%s”", Units)
                      );
                  } /*if*/
              }
            else if (DefaultUnits != null)
              {
                Multiplier = AcceptableUnits.get(DefaultUnits);
              }
            else
              {
                throw new NumberFormatException("missing units and no default");
              } /*if*/
            return
                Double.parseDouble(MeasureMatch.group(1)) * Multiplier;
          } /*Parse*/

      } /*ParseMeasure*/;

    class ParseDensity extends ParseMeasure
      {
        public ParseDensity()
          {
            super
              (
                /*Units =*/
                    new UnitEntry[]
                      {
                        new UnitEntry("dpi", 1.0 / cm_per_in),
                        new UnitEntry("dpcm", 1.0),
                      },
                /*DefaultUnitsSI =*/ "dpcm",
                /*DefaultUnitsImp =*/ "dpi"
              );
          } /*ParseDensity*/
      } /*ParseDensity*/;

    class ParseDistance extends ParseMeasure
      {
        public ParseDistance()
          {
            super
              (
                /*Units =*/
                    new UnitEntry[]
                        {
                            new UnitEntry("cm", 1.0),
                            new UnitEntry("mm", 0.1),
                            new UnitEntry("in", cm_per_in),
                        },
                /*DefaultUnitsSI =*/ "cm",
                /*DefaultUnitsImp =*/ "in"
              );
          } /*ParseDistance*/
      } /*ParseDistance*/;

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
                final double Numer = Double.parseDouble(s.substring(0, SepPos));
                final double Denom = Double.parseDouble(s.substring(SepPos + 1, s.length()));
                if (Denom <= 0.0 || Numer <= 0.0)
                  {
                    throw new NumberFormatException("ratio cannot be zero or negative");
                  } /*if*/
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

    public interface CalcFunction
      {

        public double Calculate
          (
            double[] Args
          );

      } /*CalcFunction*/;

    public static class ParamDef
      {
      /* information about each screen parameter: how to parse from string,
        format to string, and compute from other parameters */
        public static enum ParamTypes
          {
            TYPE_RATIO,
            TYPE_DISTANCE,
            TYPE_PIXELS,
            TYPE_DENSITY,
          };
        public final ParamTypes Type;
        public final Parser Parse;
        public final HashMap<FieldName[], CalcFunction> Calculate =
            new HashMap<FieldName[], CalcFunction>();

        public static class Entry
          {
          /* one particular calculation rule */
            public final FieldName[] ArgNames; /* names of other parameters that must be known */
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

    public final HashMap<FieldName, ParamDef> ParamDefs = new HashMap<FieldName, ParamDef>();
      {
        ParamDefs.put
          (
            FieldName.HeightMeasure,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_DISTANCE,
                /*Parse =*/ new ParseDistance(),
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
                                            Args[1] * Args[0];
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
                /*Type =*/ ParamDef.ParamTypes.TYPE_DISTANCE,
                /*Parse =*/ new ParseDistance(),
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
                                            Args[1] / Args[0];
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
                /*Type =*/ ParamDef.ParamTypes.TYPE_DISTANCE,
                /*Parse =*/ new ParseDistance(),
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
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.WidthPixels},
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
                            /*ArgNames =*/ new FieldName[] {FieldName.AspectRatio, FieldName.HeightPixels},
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
                            /*ArgNames =*/ new FieldName[] {FieldName.ViewingDistance},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            acuity_factor / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
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
                                            Args[0] / Args[1];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
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
        ParamDefs.put
          (
            FieldName.ViewingDistance,
            new ParamDef
              (
                /*Type =*/ ParamDef.ParamTypes.TYPE_DISTANCE,
                /*Parse =*/ new ParseDistance(),
                /*Calculate =*/ new ParamDef.Entry[]
                    {
                        new ParamDef.Entry
                          (
                            /*ArgNames =*/ new FieldName[] {FieldName.PixelDensity},
                            /*Calc =*/
                                new CalcFunction()
                                  {
                                    public double Calculate
                                      (
                                        double[] Args
                                      )
                                      {
                                        return
                                            acuity_factor / Args[0];
                                      } /*Calculate*/
                                  } /*CalcFunction*/
                          ),
                    }
              )
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
        final ParamDef.ParamTypes ParamType = ParamDefs.get(Name).Type;
        switch (ParamType)
          {
        case TYPE_RATIO:
          /* handled specially below */
        break;
        case TYPE_DISTANCE:
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
            ParamType == ParamDef.ParamTypes.TYPE_RATIO ?
                NumberUseful.Fraction.FromReal(FieldValue).toString()
            :
                String.format(Format, FieldValue * Multiplier) + Suffix;
      } /*FormatField*/

    public static enum ComputeStatus
      {
        COMPUTE_DONE,
        COMPUTE_INCOMPLETE,
      } /*ComputeStatus*/;

    public ComputeStatus ComputeParams
      (
        java.util.Map<FieldName, Double> Params,
        java.util.Set<FieldName> Computed
          /* optional for returning names of fields which were actually computed */
      )
      /* tries to fill in Params with all missing parameter values based
        on the existing ones. */
      {
        ComputeStatus Status = ComputeStatus.COMPUTE_DONE;
        for (;;)
          {
            boolean DidSomething = false;
            boolean LeftUndone = false;
            for (FieldName Name : FieldName.values())
              {
                if (!Params.containsKey(Name))
                  {
                    final ParamDef ThisParam = ParamDefs.get(Name);
                    boolean DidThis = false;
                    for (FieldName[] ArgNames : ThisParam.Calculate.keySet())
                      {
                        boolean GotAll = true; /* to begin with */
                        for (FieldName ArgName : ArgNames)
                          {
                            if (!Params.containsKey(ArgName))
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
                                Args[i] = Params.get(ArgNames[i]);
                              } /*for*/
                            final double FieldValue =
                                ThisParam.Calculate.get(ArgNames).Calculate(Args);
                            Params.put(Name, FieldValue);
                            if (Computed != null)
                              {
                                Computed.add(Name);
                              } /*if*/
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
                Status = ComputeStatus.COMPUTE_INCOMPLETE;
                break;
              } /*if*/
          } /*for*/
        return
            Status;
      } /*ComputeParams*/

  } /*Rules*/;
