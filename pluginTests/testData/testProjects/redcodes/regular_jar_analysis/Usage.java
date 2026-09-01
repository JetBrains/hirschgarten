import kotlin.jvm.internal.Intrinsics;
import org.apache.commons.lang3.BooleanUtils;

class Usage {

  static int javaLibraryContract() {
    if (<warning descr="Condition 'BooleanUtils.toBoolean((Boolean) null)' is always 'false'">BooleanUtils.toBoolean((Boolean) null)</warning>) {
      return 1;
    }
    return 0;
  }

  static int kotlinLibraryContract() {
    if (<warning descr="Condition 'Intrinsics.areEqual((Double) null, 1.0)' is always 'false'">Intrinsics.areEqual((Double) null, 1.0)</warning>) {
      return 1;
    }
    return 0;
  }
}
