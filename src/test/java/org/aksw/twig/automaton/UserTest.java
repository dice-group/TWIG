package org.aksw.twig.automaton;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

public class UserTest {

    @Test
    public void incrementTest() {
        Random r = new Random();
        byte[] test = new byte[User.NAME_LENGTH];

        for (int i = 0; i < 10; i++) {
            r.nextBytes(test);
            test[0] = 0; // To preserve overflow

            BigInteger bigInteger = new BigInteger(test);
            User.increment(test);
            Assert.assertEquals(bigInteger.add(BigInteger.ONE), new BigInteger(test));
        }
    }

    @Test
    public void nameTest() {
        User user1 = new User();
        User user2 = new User();

        Assert.assertEquals(new BigInteger(user1.getName()).add(BigInteger.ONE), new BigInteger(user2.getName()));
    }
}
