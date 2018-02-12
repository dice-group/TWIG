package org.aksw.twig.automaton;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;

class User {

  /**
   * Byte count of usernames.
   */
  public static final int NAME_LENGTH = 16;

  private static byte[] lastName = new byte[NAME_LENGTH];

  private byte[] name = new byte[NAME_LENGTH];

  /**
   * Creates a new user and generates a unique name. Name is unique only in regards to other
   * generated names. Usernames that have been set by {@link User#User(byte[])} won't be considered
   * when generating a new username.
   */
  User() {
    name = Arrays.copyOfRange(lastName, 0, lastName.length);
    increment(lastName);
  }

  /**
   * Creates a new user with given name. Username must bee at least {@link #NAME_LENGTH} bytes long.
   * 
   * @param name Name to set.
   */
  User(final byte[] name) {
    if (name.length < NAME_LENGTH) {
      throw new IllegalArgumentException("name is too short");
    }

    this.name = Arrays.copyOf(name, NAME_LENGTH);
  }

  /**
   * Returns a pointer to the username.
   * 
   * @return Username.
   */
  public byte[] getName() {
    return name;
  }

  /**
   * Returns the hexadecimal string representation of the username.
   * 
   * @return Hexadecimal number.
   */
  String getNameAsHexString() {
    return Hex.encodeHexString(name);
  }

  /**
   * Increments the value of a byte array as if it was a {@code byteArray.length} byte precision
   * decimal number.
   * 
   * @param byteArray Byte array to increment.
   */
  static void increment(final byte[] byteArray) {
    for (int i = byteArray.length - 1; i >= 0; i--) {
      byteArray[i]++;
      if (byteArray[i] != 0) {
        break;
      }
    }
  }
}
