package dice.helene.util;

public class MathUtil {
	public static double[] quadraticEquationRoots(double a, double b, double c) {
		double root1, root2; // This is now a double, too.
		root1 = (-b + Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
		root2 = (-b - Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
		double res[] = { root1, root2 };
		return res;
	}
}
