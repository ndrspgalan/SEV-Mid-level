package console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleInput {

    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleInput(InputStream input, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
    }

    public String readRequiredText(String prompt) {
        while (true) {
            output.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            output.println("El valor no puede estar vacío.");
        }
    }


    public String readOptionalText(String prompt) {
        output.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            output.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                int parsed = Integer.parseInt(value);
                if (parsed >= minimum && parsed <= maximum) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // The validation message below is sufficient for the user.
            }
            output.println(
                    "Introduce un número entero entre "
                            + minimum
                            + " y "
                            + maximum
                            + "."
            );
        }
    }

    public int readPositiveInt(String prompt) {
        while (true) {
            output.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // The validation message below is sufficient for the user.
            }
            output.println("Introduce un número entero mayor que cero.");
        }
    }

    public double readRatio(String prompt) {
        while (true) {
            output.print(prompt);
            String value = scanner.nextLine().trim().replace(',', '.');
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isFinite(parsed) && parsed >= 0.0 && parsed <= 1.0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // The validation message below is sufficient for the user.
            }
            output.println("Introduce una proporción comprendida entre 0 y 1.");
        }
    }

    public <E extends Enum<E>> E readEnumChoice(
            String title,
            E[] values
    ) {
        output.println(title);
        for (int index = 0; index < values.length; index++) {
            output.println((index + 1) + ". " + values[index]);
        }
        int choice = readInt("Selecciona una opción: ", 1, values.length);
        return values[choice - 1];
    }
}
