package com.google.zxing.common.reedsolomon;

public final class ReedSolomonDecoder {
   private final GenericGF field;

   public ReedSolomonDecoder(GenericGF field) {
      this.field = field;
   }

   public void decode(int[] received, int twoS) throws ReedSolomonException {
      this.decodeWithECCount(received, twoS);
   }

   public int decodeWithECCount(int[] received, int twoS) throws ReedSolomonException {
      GenericGFPoly poly = new GenericGFPoly(this.field, received);
      int[] syndromeCoefficients = new int[twoS];
      boolean noError = true;

      for (int i = 0; i < twoS; i++) {
         int eval = poly.evaluateAt(this.field.exp(i + this.field.getGeneratorBase()));
         syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
         if (eval != 0) {
            noError = false;
         }
      }

      if (noError) {
         return 0;
      } else {
         GenericGFPoly syndrome = new GenericGFPoly(this.field, syndromeCoefficients);
         GenericGFPoly[] sigmaOmega = this.runEuclideanAlgorithm(this.field.buildMonomial(twoS, 1), syndrome, twoS);
         GenericGFPoly sigma = sigmaOmega[0];
         GenericGFPoly omega = sigmaOmega[1];
         int[] errorLocations = this.findErrorLocations(sigma);
         int[] errorMagnitudes = this.findErrorMagnitudes(omega, errorLocations);

         for (int ix = 0; ix < errorLocations.length; ix++) {
            int position = received.length - 1 - this.field.log(errorLocations[ix]);
            if (position < 0) {
               throw new ReedSolomonException("Bad error location");
            }

            received[position] = GenericGF.addOrSubtract(received[position], errorMagnitudes[ix]);
         }

         return errorLocations.length;
      }
   }

   private GenericGFPoly[] runEuclideanAlgorithm(GenericGFPoly a, GenericGFPoly b, int R) throws ReedSolomonException {
      if (a.getDegree() < b.getDegree()) {
         GenericGFPoly temp = a;
         a = b;
         b = temp;
      }

      GenericGFPoly rLast = a;
      GenericGFPoly r = b;
      GenericGFPoly tLast = this.field.getZero();
      GenericGFPoly t = this.field.getOne();

      while (2 * r.getDegree() >= R) {
         GenericGFPoly rLastLast = rLast;
         GenericGFPoly tLastLast = tLast;
         rLast = r;
         tLast = t;
         if (r.isZero()) {
            throw new ReedSolomonException("r_{i-1} was zero");
         }

         r = rLastLast;
         GenericGFPoly q = this.field.getZero();
         int denominatorLeadingTerm = rLast.getCoefficient(rLast.getDegree());
         int dltInverse = this.field.inverse(denominatorLeadingTerm);

         while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
            int degreeDiff = r.getDegree() - rLast.getDegree();
            int scale = this.field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
            q = q.addOrSubtract(this.field.buildMonomial(degreeDiff, scale));
            r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
         }

         t = q.multiply(t).addOrSubtract(tLastLast);
         if (r.getDegree() >= rLast.getDegree()) {
            throw new IllegalStateException("Division algorithm failed to reduce polynomial? r: " + r + ", rLast: " + rLast);
         }
      }

      int sigmaTildeAtZero = t.getCoefficient(0);
      if (sigmaTildeAtZero == 0) {
         throw new ReedSolomonException("sigmaTilde(0) was zero");
      } else {
         int inverse = this.field.inverse(sigmaTildeAtZero);
         GenericGFPoly sigma = t.multiply(inverse);
         GenericGFPoly omega = r.multiply(inverse);
         return new GenericGFPoly[]{sigma, omega};
      }
   }

   private int[] findErrorLocations(GenericGFPoly errorLocator) throws ReedSolomonException {
      int numErrors = errorLocator.getDegree();
      if (numErrors == 1) {
         return new int[]{errorLocator.getCoefficient(1)};
      } else {
         int[] result = new int[numErrors];
         int e = 0;

         for (int i = 1; i < this.field.getSize() && e < numErrors; i++) {
            if (errorLocator.evaluateAt(i) == 0) {
               result[e] = this.field.inverse(i);
               e++;
            }
         }

         if (e != numErrors) {
            throw new ReedSolomonException("Error locator degree does not match number of roots");
         } else {
            return result;
         }
      }
   }

   private int[] findErrorMagnitudes(GenericGFPoly errorEvaluator, int[] errorLocations) {
      int s = errorLocations.length;
      int[] result = new int[s];

      for (int i = 0; i < s; i++) {
         int xiInverse = this.field.inverse(errorLocations[i]);
         int denominator = 1;

         for (int j = 0; j < s; j++) {
            if (i != j) {
               int term = this.field.multiply(errorLocations[j], xiInverse);
               int termPlus1 = (term & 1) == 0 ? term | 1 : term & -2;
               denominator = this.field.multiply(denominator, termPlus1);
            }
         }

         result[i] = this.field.multiply(errorEvaluator.evaluateAt(xiInverse), this.field.inverse(denominator));
         if (this.field.getGeneratorBase() != 0) {
            result[i] = this.field.multiply(result[i], xiInverse);
         }
      }

      return result;
   }
}
