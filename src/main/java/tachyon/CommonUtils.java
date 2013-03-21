package tachyon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import tachyon.thrift.InvalidPathException;

/**
 * Utility class shared by all components of the system.
 * 
 * @author haoyuan
 */
public class CommonUtils {
  private static final Logger LOG = Logger.getLogger(Config.LOGGER_TYPE);

  /**
   * Whether the pathname is valid.  Currently prohibits relative paths, 
   * and names which contain a ":" or "/" 
   */
  public static boolean isValidName(String src) {
    // Path must be absolute.
    if (!src.startsWith(Path.SEPARATOR)) {
      return false;
    }

    // Check for ".." "." ":" "/"
    StringTokenizer tokens = new StringTokenizer(src, Path.SEPARATOR);
    while(tokens.hasMoreTokens()) {
      String element = tokens.nextToken();
      if (element.equals("..") || 
          element.equals(".")  ||
          (element.indexOf(":") >= 0)  ||
          (element.indexOf("/") >= 0)) {
        return false;
      }
    }
    return true;
  }

  public static String cleanPath(String path) {
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  public static ByteBuffer cloneByteBuffer(ByteBuffer buf) {
    ByteBuffer ret = ByteBuffer.allocate(buf.limit() - buf.position());
    ret.put(buf);
    ret.flip();
    buf.flip();
    return ret;
  }

  public static List<ByteBuffer> cloneByteBufferList(List<ByteBuffer> source) {
    List<ByteBuffer> ret = new ArrayList<ByteBuffer>(source.size());
    for (int k = 0; k < source.size(); k ++) {
      ret.add(cloneByteBuffer(source.get(k)));
    }
    return ret;
  }

  public static String convertMsToClockTime(long Millis) {
    return String.format("%d hour(s), %d minute(s), and %d second(s)",
        Millis / (1000L * 60 * 60), (Millis % (1000L * 60 * 60)) / (1000 * 60),
        (Millis % (1000L * 60)) / 1000);
  }

  public static String convertMsToShortClockTime(long Millis) {
    return String.format("%d h, %d m, and %d s",
        Millis / (1000L * 60 * 60), (Millis % (1000L * 60 * 60)) / (1000 * 60),
        (Millis % (1000L * 60)) / 1000);
  }

  public static String convertMsToDate(long Millis) {
    DateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
    return formatter.format(new Date(Millis));
  }

  public static String convertMsToSimpleDate(long Millis) {
    DateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
    return formatter.format(new Date(Millis));
  }

  public static void deleteFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) {
      while (!file.delete()) {
        LOG.info("Trying to delete " + file.toString());
        sleep(LOG, 1000);
      }
    }
  }

  public static boolean existFile(String fileName) {
    return (new File(fileName)).exists();
  }

  public static String getCurrentMemStatsInBytes() {
    Runtime runtime = Runtime.getRuntime();
    StringBuilder sb = new StringBuilder();
    sb.append(" MaxMemory=").append((runtime.maxMemory())).append(" bytes");
    sb.append(" TotalMemory=").append((runtime.totalMemory())).append(" bytes");
    sb.append(" FreeMemory=").append((runtime.freeMemory())).append(" bytes");
    sb.append(" UsedMemory=").append((runtime.totalMemory() - runtime.freeMemory())).append(" bytes");
    return sb.toString();
  }

  public static String getCurrentMemStats() {
    Runtime runtime = Runtime.getRuntime();
    StringBuilder sb = new StringBuilder();
    sb.append(" MaxMemory=").append(getSizeFromBytes(runtime.maxMemory()));
    sb.append(" TotalMemory=").append(getSizeFromBytes(runtime.totalMemory()));
    sb.append(" FreeMemory=").append(getSizeFromBytes(runtime.freeMemory()));
    sb.append(" UsedMemory=").append(getSizeFromBytes(runtime.totalMemory() - runtime.freeMemory()));
    return sb.toString();
  }

  public static String getCurrentMemStatsAfterGCs() {
    for (int k = 0; k < 10; k ++) {
      System.gc();
    }
    return getCurrentMemStats();
  }

  public static long getCurrentMs() {
    return System.currentTimeMillis();
  }

  public static long getCurrentNs() {
    return System.nanoTime();
  }

  public static int getFileIdFromFileName(String name) {
    int fileId;
    try {
      fileId = Integer.parseInt(name);
    } catch (Exception e) {
      throw new IllegalArgumentException("Wrong file name: " + name);
    }
    return fileId;
  }

  public static String getLocalFilePath(String localFolder, int fileId) {
    return localFolder + "/" + fileId;
  }

  public static int getKB(int bytes) {
    return bytes / 1024;
  }

  public static long getKB(long bytes) {
    return bytes / 1024;
  }

  public static int getMB(int bytes) {
    return bytes / 1024 / 1024;
  }

  public static long getMB(long bytes) {
    return bytes / 1024 / 1024;
  }

  public static byte[] getMd5(byte[] data) {
    return DigestUtils.md5(data);
  }

  public static String getMd5Hex(byte[] data) {
    return DigestUtils.md5Hex(data);
  }

  public static String getMd5Hex(String fileName) {
    String ret = null;
    try {
      FileInputStream fis = new FileInputStream(fileName);
      ret = DigestUtils.md5Hex(fis);
    } catch (FileNotFoundException e) {
      runtimeException(e);
    } catch (IOException e) {
      runtimeException(e);
    }
    return ret;
  }

  public static String getSizeFromBytes(long bytes) {
    double ret = bytes;
    if (ret <= 1024 * 5) {
      return String.format("%.2f B", ret); 
    }
    ret /= 1024;
    if (ret <= 1024 * 5) {
      return String.format("%.2f KB", ret);
    }
    ret /= 1024;
    if (ret <= 1024 * 5) {
      return String.format("%.2f MB", ret);
    }
    ret /= 1024;
    return String.format("%.2f GB", ret);
  }

  public static void illegalArgumentException(String msg) {
    throw new IllegalArgumentException(msg);
  }

  public static void illegalArgumentException(Exception e) {
    LOG.error(e.getMessage(), e);
    throw new IllegalArgumentException(e);
  }

  public static <T> String listToString(List<T> list) {
    StringBuilder sb = new StringBuilder();
    for (int k = 0; k < list.size(); k ++) {
      sb.append(list.get(k)).append(" ");
    }
    return sb.toString();
  }

  public static String parametersToString(Object ... objs) {
    StringBuilder sb = new StringBuilder("(");
    for (int k = 0; k < objs.length; k ++) {
      if (k != 0) {
        sb.append(", ");
      }
      sb.append(objs[k].toString());
    }
    sb.append(")");
    return sb.toString();
  }

  public static long parseMemorySize(String memorySize) {
    String ori = memorySize;
    String end = "";
    int tIndex = memorySize.length() - 1;
    while (tIndex >= 0) {
      if (memorySize.charAt(tIndex) > '9' || memorySize.charAt(tIndex) < '0') {
        end = memorySize.charAt(tIndex) + end;
      } else {
        break;
      }
      tIndex --;
    }
    memorySize = memorySize.substring(0, tIndex + 1);
    long ret = Long.parseLong(memorySize);
    end = end.toLowerCase();
    if (end.equals("") || end.equals("b")) {
      return ret;
    } else if (end.equals("kb")) {
      return ret * Config.KB;
    } else if (end.equals("mb")) {
      return ret * Config.MB;
    } else if (end.equals("gb")) {
      return ret * Config.GB;
    }
    runtimeException("Fail to parse " + ori + " as memory size");
    return -1;
  }

  public static void printByteBuffer(Logger LOG, ByteBuffer buf) {
    String tmp = "";
    for (int k = 0; k < buf.limit() / 4; k ++) {
      tmp += buf.getInt() + " ";
    }

    LOG.info(tmp);
  }

  public static void printTimeTakenMs(long startTimeMs, Logger logger, String message) {
    logger.info(message + " took " + (getCurrentMs() - startTimeMs) + " ms.");
  }

  public static void printTimeTakenNs(long startTimeNs, Logger logger, String message) {
    logger.info(message + " took " + (getCurrentNs() - startTimeNs) + " ns.");
  }

  public static void renameFile(String src, String dst) {
    File srcFile = new File(src);
    File dstFile = new File(dst);
    if (!srcFile.renameTo(dstFile)) {
      CommonUtils.runtimeException("Failed to rename file from " + src + " to " + dst);
    }
  }

  public static void runtimeException(String msg) {
    throw new RuntimeException(msg);
  }

  public static void runtimeException(Exception e) {
    LOG.error(e.getMessage(), e);
    throw new RuntimeException(e);
  }

  public static void sleep(Logger logger, long timeMs) {
    try {
      Thread.sleep(timeMs);
    } catch (InterruptedException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public static String[] toStringArray(ArrayList<String> src) {
    String[] ret = new String[src.size()];
    return src.toArray(ret);
  }

  public static void tempoaryLog(String msg) {
    LOG.info("Temporary Log ============================== " + msg);
  }

  public static void validatePath(String path) throws InvalidPathException {
    if (path == null || !path.startsWith(Config.SEPARATOR) || 
        (path.length() > 1 && path.endsWith(Config.SEPARATOR))) {
      throw new InvalidPathException("Path " + path + " is invalid.");
    }
  }
}