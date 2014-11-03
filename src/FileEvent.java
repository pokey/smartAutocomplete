package smartAutocomplete;

import java.util.*;

public enum FileEvent {
  SCANNED_FILE(0),
  ANALYZE(1),
  ACCEPTED(2),
  BUF_LEAVE(3),
  BUF_READ(4),
  FILE_READ(5),
  BUF_WRITE(6),
  BUF_UNLOAD(7),
  FILE_CHANGED(8),
  INSERT_ENTER(9),
  INSERT_LEAVE(10);

  private static Map<Integer, FileEvent> map = null;

  private final int id;

  private FileEvent(int id) {
    this.id = id;
  }

  public int getValue() { return id; }

  public static FileEvent fromId(int id) {
    if (map == null) {
      map = new HashMap<Integer, FileEvent>();
      for (FileEvent event : FileEvent.class.getEnumConstants()) {
        map.put(event.id, event);
      }
    }
    return map.get(id);
  }
}
