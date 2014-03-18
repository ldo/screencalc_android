package nz.gen.geek_central.screencalc;
/*
    Useful numeric routines for Screencalc.

    Copyright 2013, 2014 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

public class NumberUseful
  {
    public static int gcd
      (
        int a,
        int b
      )
      /* returns greatest common divisor of a and b. */
      {
        if (a < 0 || b < 0)
          {
            throw new IllegalArgumentException("gcd of negative numbers");
          } /*if*/
        for (;;)
          {
            if (a < b)
              {
                final int tmp = a;
                a = b;
                b = tmp;
              } /*if*/
            if (b == 0)
                break;
            a = a % b;
          } /*for*/
        return
            a;
      } /*gcd*/

    public static class Fraction
      {
        public final int Numer, Denom;

        public Fraction
          (
            int Numer,
            int Denom
          )
          {
            if (Numer <= 0 || Denom <= 0)
              {
                throw new IllegalArgumentException("Fraction numerator and denominator must both be positive");
              } /*if*/
            int GCD = gcd(Numer, Denom);
            this.Numer = Numer / GCD;
            this.Denom = Denom / GCD;
          } /*Fraction*/

        @Override
        public String toString()
          {
            return
                String.format("%d:%d", Numer, Denom);
          } /*toString*/

        public static Fraction FromString
          (
            String s
          )
          {
            final int SepPos = s.indexOf(":");
            final Fraction Result;
            if (SepPos >= 0)
              {
                final String NumerStr = s.substring(0, SepPos);
                final String DenomStr = s.substring(SepPos + 1, s.length());
                if (NumerStr.indexOf(".") >= 0 || DenomStr.indexOf(".") >= 0)
                  {
                    Result = FromReal
                      (
                        Double.parseDouble(NumerStr) / Double.parseDouble(DenomStr)
                      );
                  }
                else
                  {
                    final int Numer = Integer.parseInt(NumerStr);
                    final int Denom = Integer.parseInt(DenomStr);
                    Result = new Fraction(Numer, Denom);
                  } /*if*/
              }
            else
              {
                Result = FromReal(Double.parseDouble(s));
              } /*if*/
            return
                Result;
          } /*FromString*/

        public double ToReal()
          {
            return
                Numer * 1.0 / Denom;
          } /*ToReal*/

        public static Fraction FromReal
          (
            double Val
          )
          {
            final int Multiplier = 3240; /* something with lots of factors */
            final int Places = 2; /* decimal places of precision needed */
            final double RangeFactor = 1.0; /* how far to look */
            final double Tol = Math.pow(10, - Places);

            int Denom = (int)Math.round
              (
                Math.pow(Multiplier, Math.ceil(Places / Math.log10(Multiplier)))
              );
            int Numer = (int)Math.round(Val * Denom);
              {
                final int GCD = gcd(Numer, Denom);
                Numer /= GCD;
                Denom /= GCD;
              }
            for (;;)
              {
                int BestNumer = Numer;
                int BestDenom = Denom;
                final double DeltaILow = Val * (1 - Tol) * Denom - Numer;
                final double DeltaIHigh = Val * (1 + Tol) * Denom - Numer;
                final int ILow = Math.max(Numer + (int)Math.floor(DeltaILow * RangeFactor), 1);
                final int IHigh = Numer + (int)Math.ceil(DeltaIHigh * RangeFactor);
                boolean IAscending = false;
                for (int i = Numer;;)
                  {
                    if (IAscending && i > IHigh)
                        break;
                    if (!IAscending && i < ILow)
                      {
                        i = Numer + 1;
                        IAscending = true;
                      } /*if*/
                    final double DeltaJLow = Numer / (Val * (1 + Tol)) - Denom;
                    final double DeltaJHigh = Numer / (Val * (1 - Tol)) - Denom;
                    final int JLow = Math.max((int)Math.floor(Denom + DeltaJLow * RangeFactor), 1);
                    final int JHigh = (int)Math.ceil(Denom + DeltaJHigh);
                    boolean JAscending = false;
                    for (int j = Denom;;)
                      {
                        if (JAscending && j > JHigh)
                            break;
                        if (!JAscending && j < JLow)
                          {
                            j = Denom + 1;
                            JAscending = true;
                          } /*if*/
                        if
                          (
                                i > 0
                            &&
                                j > 0
                            &&
                                (i != Numer || j != Denom)
                            &&
                                Math.abs((i * 1.0 / j - Val) / Val) <= Tol
                          )
                          {
                            final int ThisGCD = gcd(i, j);
                            if (j / ThisGCD < BestDenom)
                              {
                                BestNumer = i / ThisGCD;
                                BestDenom = j / ThisGCD;
                              } /*if*/
                          } /*if*/
                        j = JAscending ? j + 1 : j - 1;
                      } /*for*/
                    i = IAscending ? i + 1 : i - 1;
                  } /*for*/
                if (BestDenom >= Denom)
                    break;
                Denom = BestDenom;
                Numer = BestNumer;
              } /*for*/
            return
                new Fraction(Numer, Denom);
          } /*FromReal*/

      } /*Fraction*/;
    
  } /*NumberUseful*/;
