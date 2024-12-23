package outer;

import inner.LibInner;

public class BinOuter {
  public static void main(String[] args) {
    LibInner inner = new LibInner();
    inner.doInner();

    LibOuter outer = new LibOuter();
    outer.doOuter();
  }
}
