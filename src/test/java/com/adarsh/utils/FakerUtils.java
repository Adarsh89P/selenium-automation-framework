package com.adarsh.utils;

import com.adarsh.domain.Customer;
import com.adarsh.domain.Payee;
import java.math.BigDecimal;
import java.util.Locale;
import net.datafaker.Faker;

/**
 * Generated test data, for the cases where uniqueness is the point.
 *
 * <p>Registration is the clearest example: a fixed username in a JSON file works exactly once and
 * then collides forever. Anything that must be unique per run is generated here; anything that
 * must be stable across runs lives in {@code testdata/*.json} instead.
 */
public final class FakerUtils {

    // Locale is pinned so generated values stay ASCII and fit ParaBank's field validation.
    private static final Faker FAKER = new Faker(Locale.US);

    private FakerUtils() {
        // utility holder
    }

    /** A registrable customer whose username is unique for this run. */
    public static Customer newCustomer() {
        var firstName = FAKER.name().firstName();
        var lastName = FAKER.name().lastName();
        return new Customer(
                firstName,
                lastName,
                FAKER.address().streetAddress(),
                FAKER.address().city(),
                FAKER.address().stateAbbr(),
                FAKER.address().zipCode().substring(0, 5),
                numericPhone(),
                numeric(9),
                uniqueUsername(firstName),
                "Passw0rd!" + numeric(4));
    }

    public static Payee newPayee(BigDecimal amount) {
        return new Payee(
                FAKER.company().name(),
                FAKER.address().streetAddress(),
                FAKER.address().city(),
                FAKER.address().stateAbbr(),
                FAKER.address().zipCode().substring(0, 5),
                numericPhone(),
                numeric(6),
                amount);
    }

    /**
     * Username collisions are the one failure mode that would make registration scenarios
     * order-dependent, so this mixes the name with a timestamp and a random suffix.
     */
    public static String uniqueUsername(String seed) {
        var prefix = seed.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return prefix + System.currentTimeMillis() % 1_000_000 + numeric(3);
    }

    public static String numericPhone() {
        return numeric(10);
    }

    public static String numeric(int digits) {
        return FAKER.number().digits(digits);
    }

    /** A small money amount, kept low so it never overdraws a demo account. */
    public static BigDecimal smallAmount() {
        return BigDecimal.valueOf(FAKER.number().numberBetween(1, 50));
    }

    public static Faker faker() {
        return FAKER;
    }
}
