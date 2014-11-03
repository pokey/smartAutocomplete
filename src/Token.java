package smartAutocomplete;

import fig.basic.*;

public class Token implements MemUsage.Instrumented {
  public enum Type { ACTUAL, OOV, BOS, INDENT, DEDENT, NEWLINE, EOS }
  public enum Context { NONE, COMMENT, STRING}

  private final String str_;
  private final int loc_;
  private final Type type_;
  private final Context context_;

  public Token(String _str, int _loc, Type _type, Context _context) {
    str_ = _str;
    loc_ = _loc;
    type_ = _type;
    context_ = _context;
  }

  public Token(String _str, int _loc) {
    this(_str, _loc, Type.ACTUAL, Context.NONE);
  }

  public String str() { return str_; }
  public int loc() { return loc_; }
  public Type type() { return type_; }
  public Context context() { return context_; }

  public String toString() { return str_; }

  public long getBytes() {
    return MemUsage.objectSize(MemUsage.pointerSize*3 + MemUsage.intSize) +
           MemUsage.getBytes(str_);
  }
}
