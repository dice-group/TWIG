package org.aksw.twig.application;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RuntimeUtil {
  private static final Logger LOG = LogManager.getLogger(RuntimeUtil.class);
  private static final long MEGABYTE_FACTOR = 1024L * 1024L;
  private static final DecimalFormat ROUNDED_DOUBLE_DECIMALFORMAT;
  private static final String MIB = "MiB";

  static {
    final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
    otherSymbols.setDecimalSeparator('.');
    otherSymbols.setGroupingSeparator(',');
    ROUNDED_DOUBLE_DECIMALFORMAT = new DecimalFormat("####0.00", otherSymbols);
    ROUNDED_DOUBLE_DECIMALFORMAT.setGroupingUsed(false);
  }

  private RuntimeUtil() {}

  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory();
  }

  public static long getUsedMemory() {
    return getTotalMemory() - getFreeMemory();
  }

  public static long getTotalMemory() {
    return Runtime.getRuntime().totalMemory();
  }

  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory();
  }

  public static String getTotalMemoryInMiB() {
    final double totalMiB = bytesToMiB(getTotalMemory());
    return String.format("%s %s", ROUNDED_DOUBLE_DECIMALFORMAT.format(totalMiB), MIB);
  }

  public static String getFreeMemoryInMiB() {
    final double freeMiB = bytesToMiB(getFreeMemory());
    return String.format("%s %s", ROUNDED_DOUBLE_DECIMALFORMAT.format(freeMiB), MIB);
  }

  public static String getUsedMemoryInMiB() {
    final double usedMiB = bytesToMiB(getUsedMemory());
    return String.format("%s %s", ROUNDED_DOUBLE_DECIMALFORMAT.format(usedMiB), MIB);
  }

  public static String getMaxMemoryInMiB() {
    final double maxMiB = bytesToMiB(getMaxMemory());
    return String.format("%s %s", ROUNDED_DOUBLE_DECIMALFORMAT.format(maxMiB), MIB);
  }

  public static String inMiB(final long in) {
    final double maxMiB = bytesToMiB(in);
    return String.format("%s %s", ROUNDED_DOUBLE_DECIMALFORMAT.format(maxMiB), MIB);
  }

  public static double getPercentageUsed() {
    return (double) getUsedMemory() / getMaxMemory() * 100;
  }

  public static String getPercentageUsedFormatted() {
    final double usedPercentage = getPercentageUsed();
    return ROUNDED_DOUBLE_DECIMALFORMAT.format(usedPercentage) + "%";
  }

  public static double bytesToMiB(final long bytes) {
    return (double) bytes / MEGABYTE_FACTOR;
  }

  public static String getHostAdress() {
    try {
      final java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
      return addr.getHostAddress();
    } catch (final UnknownHostException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return "";
  }

  public static String getHostName() {
    try {
      final java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (final UnknownHostException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    return "";
  }

  public static String getSystemInformation() {
    return String.format(
        "SystemInfo ; Current heap:%s ; Used:%s  ; Free:%s  ; Maximum Heap:%s  ; Percentage Used:%s",
        getTotalMemoryInMiB(), getUsedMemoryInMiB(), getFreeMemoryInMiB(), getMaxMemoryInMiB(),
        getPercentageUsedFormatted());
  }
}
