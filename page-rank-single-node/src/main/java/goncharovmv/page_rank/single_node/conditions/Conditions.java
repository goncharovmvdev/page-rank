package goncharovmv.page_rank.single_node.conditions;

public final class Conditions {

    private Conditions() {
        throw new AssertionError();
    }

    public static <T extends Number> T checkPositive(T number) {
        if (number == null || number.floatValue() < 0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    public static double checkBetween(double number, double lower, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException();
        }
        if (number < lower) {
            throw new IllegalArgumentException();
        }
        if (number > upper) {
            throw new IllegalArgumentException();
        }
        return number;
    }
}
