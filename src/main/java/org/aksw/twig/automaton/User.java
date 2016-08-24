package org.aksw.twig.automaton;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;
import java.util.Random;

class User {

    private static final int NAME_LENGTH = 16;

    private byte[] name = new byte[NAME_LENGTH];

    public void setName(final byte[] name) {
        if (name.length < NAME_LENGTH) {
            throw new IllegalArgumentException("name is too short");
        }

        this.name = Arrays.copyOf(name, NAME_LENGTH);
    }

    void setNameOfRandom(final Random r) {
        r.nextBytes(name); // TODO: there can be collisions
    }

    String getNameAsHexString() {
        return Hex.encodeHexString(name);
    }
}
