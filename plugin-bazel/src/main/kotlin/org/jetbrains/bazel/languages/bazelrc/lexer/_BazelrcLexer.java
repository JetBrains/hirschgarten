// Generated by JFlex 1.9.2 http://jflex.de/  (tweaked for IntelliJ platform)
// source: Bazelrc.flex

package org.jetbrains.bazel.languages.bazelrc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;

@SuppressWarnings("ALL")

class _BazelrcLexer implements FlexLexer {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int IMPORT = 2;
  public static final int CMD = 4;
  public static final int CONFIG = 6;
  public static final int CMD_DQ = 8;
  public static final int CONFIG_DQ = 10;
  public static final int CMD_SQ = 12;
  public static final int CONFIG_SQ = 14;
  public static final int FLAGS = 16;
  public static final int VALUE = 18;
  public static final int FLAG_DQ = 20;
  public static final int VALUE_DQ = 22;
  public static final int FLAG_SQ = 24;
  public static final int VALUE_SQ = 26;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
    0,  0,  1,  1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,  7,  7,
    8,  8,  9,  9, 10, 10, 11, 11, 12, 12, 13, 13
  };

  /**
   * Top-level table for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_TOP = zzUnpackcmap_top();

  private static final String ZZ_CMAP_TOP_PACKED_0 =
    "\1\0\u10ff\u0100";

  private static int [] zzUnpackcmap_top() {
    int [] result = new int[4352];
    int offset = 0;
    offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_top(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Second-level tables for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

  private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
    "\11\0\1\1\1\2\2\0\1\3\22\0\1\1\1\0"+
      "\1\4\1\5\3\0\1\6\5\0\1\7\1\0\1\10"+
      "\12\0\1\11\2\0\1\12\36\0\1\13\14\0\1\14"+
      "\3\0\1\15\1\0\1\16\1\17\1\0\1\20\1\0"+
      "\1\21\4\0\1\22\u0186\0";

  private static int [] zzUnpackcmap_blocks() {
    int [] result = new int[512];
    int offset = 0;
    offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_blocks(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\16\0\1\1\1\2\1\3\3\1\1\4\1\2\1\5"+
      "\3\4\1\6\1\7\1\10\1\11\1\12\1\13\1\6"+
      "\1\14\1\10\1\15\1\16\1\13\1\14\1\6\1\17"+
      "\1\20\1\21\1\6\2\22\1\6\1\23\1\24\1\6"+
      "\2\22\1\25\1\2\1\26\1\27\1\30\1\25\1\31"+
      "\1\25\1\4\1\7\1\32\1\4\1\33\1\4\1\10"+
      "\1\34\2\4\1\10\1\35\2\4\1\0\1\3\4\0"+
      "\3\4\7\0\1\27\3\36\1\4\1\0\2\4\1\0"+
      "\3\36\1\0\3\36\11\0\1\7\2\0\2\4\1\0"+
      "\2\4\7\0\1\37";

  private static int [] zzUnpackAction() {
    int [] result = new int[131];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\23\0\46\0\71\0\114\0\137\0\162\0\205"+
      "\0\230\0\253\0\276\0\321\0\344\0\367\0\u010a\0\u011d"+
      "\0\u0130\0\u0143\0\u0156\0\u0169\0\u017c\0\u018f\0\u010a\0\u01a2"+
      "\0\u01b5\0\u01c8\0\u01db\0\u01ee\0\u0201\0\u010a\0\u010a\0\u010a"+
      "\0\u0214\0\u0227\0\u010a\0\u010a\0\u010a\0\u0227\0\u023a\0\u024d"+
      "\0\u0201\0\u010a\0\u010a\0\u0260\0\u0273\0\u0286\0\u0299\0\u010a"+
      "\0\u010a\0\u02ac\0\u02bf\0\u02d2\0\u010a\0\u02e5\0\u010a\0\u02f8"+
      "\0\u010a\0\u030b\0\u010a\0\u031e\0\u0331\0\u0344\0\u010a\0\u0357"+
      "\0\u010a\0\u036a\0\u037d\0\u010a\0\u0390\0\u03a3\0\u03b6\0\u010a"+
      "\0\u03c9\0\u03dc\0\u0143\0\u03ef\0\u0402\0\u0415\0\u0428\0\u031e"+
      "\0\u010a\0\u043b\0\u044e\0\u0461\0\u0474\0\u0487\0\u049a\0\u04ad"+
      "\0\u04c0\0\u04d3\0\u04e6\0\u04f9\0\u0331\0\u050c\0\u051f\0\u0532"+
      "\0\u0545\0\u0558\0\u056b\0\u057e\0\u0591\0\u05a4\0\u05b7\0\u05ca"+
      "\0\u05dd\0\u05f0\0\u0603\0\u0616\0\u0629\0\u063c\0\u064f\0\u0662"+
      "\0\u0675\0\u0688\0\u069b\0\u06ae\0\u06c1\0\u06d4\0\u0591\0\u06e7"+
      "\0\u06fa\0\u05dd\0\u070d\0\u0720\0\u0733\0\u0746\0\u0759\0\u076c"+
      "\0\u077f\0\u0156\0\u010a";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[131];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length() - 1;
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpacktrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\17\3\20\1\17\1\21\5\17\1\22\1\23\4\17"+
      "\1\24\1\17\1\25\1\26\2\27\1\30\1\25\1\31"+
      "\4\25\1\32\7\25\1\33\1\34\2\35\1\36\1\33"+
      "\1\37\2\33\1\40\1\33\1\41\7\33\1\42\1\43"+
      "\2\35\1\44\1\42\1\45\2\42\1\46\1\42\1\47"+
      "\7\42\2\50\2\51\1\52\4\50\1\53\1\50\1\54"+
      "\7\50\2\55\2\51\1\52\4\55\1\43\1\55\1\56"+
      "\7\55\2\57\2\51\2\57\1\60\2\57\1\61\1\57"+
      "\1\62\7\57\2\63\2\51\2\63\1\60\2\63\1\43"+
      "\1\63\1\64\7\63\1\65\1\66\2\51\1\67\1\70"+
      "\1\71\1\72\2\65\1\73\1\74\7\65\1\75\1\76"+
      "\2\51\1\77\1\100\1\101\4\75\1\102\7\75\4\43"+
      "\1\52\2\43\1\103\2\43\1\104\10\43\2\105\2\43"+
      "\1\52\6\105\1\106\7\105\6\43\1\60\1\107\2\43"+
      "\1\110\10\43\2\111\2\43\2\111\1\60\4\111\1\112"+
      "\7\111\24\0\3\20\7\0\1\113\7\0\2\21\2\0"+
      "\7\21\1\114\7\21\2\0\1\20\1\115\34\0\1\116"+
      "\25\0\1\117\2\0\1\25\4\0\1\25\1\0\14\25"+
      "\1\0\1\26\11\0\1\120\7\0\2\30\2\0\1\121"+
      "\6\30\1\122\7\30\2\31\2\0\2\31\1\121\4\31"+
      "\1\123\7\31\1\25\1\0\1\26\1\124\1\0\1\25"+
      "\1\0\14\25\1\33\4\0\1\33\1\0\2\33\1\0"+
      "\1\33\1\41\7\33\1\0\1\34\22\0\3\51\17\0"+
      "\1\33\1\0\1\33\1\125\1\0\1\33\1\0\2\33"+
      "\1\0\1\33\1\41\7\33\1\42\4\0\1\42\1\0"+
      "\4\42\1\47\10\42\1\0\1\42\1\126\1\0\1\42"+
      "\1\0\4\42\1\47\7\42\2\50\3\0\4\50\1\0"+
      "\1\50\1\54\12\50\1\127\5\50\1\0\1\50\1\54"+
      "\7\50\2\55\3\0\4\55\1\0\1\55\1\56\12\55"+
      "\1\130\5\55\1\0\1\55\1\56\7\55\2\57\2\0"+
      "\2\57\1\0\2\57\1\0\1\57\1\62\12\57\1\131"+
      "\5\57\1\0\1\57\1\62\7\57\2\63\2\0\2\63"+
      "\1\0\2\63\1\0\1\63\1\64\12\63\1\132\5\63"+
      "\1\0\1\63\1\64\7\63\1\0\1\66\2\51\7\0"+
      "\1\120\7\0\2\70\2\0\7\70\1\133\7\70\1\134"+
      "\4\0\1\134\1\0\1\134\1\135\1\134\1\0\1\136"+
      "\7\134\2\0\1\26\1\124\17\0\1\75\4\0\1\75"+
      "\1\0\4\75\1\137\7\75\1\0\1\76\2\51\7\0"+
      "\1\140\7\0\1\100\1\70\2\0\1\70\1\100\1\70"+
      "\4\100\1\141\7\100\1\75\1\0\1\142\1\143\1\0"+
      "\1\75\1\0\4\75\1\137\7\75\2\144\3\0\3\144"+
      "\1\145\1\144\1\0\1\146\7\144\2\105\3\0\6\105"+
      "\1\106\12\105\1\147\7\105\1\106\7\105\2\150\2\0"+
      "\2\150\1\0\1\150\1\151\1\150\1\0\1\152\7\150"+
      "\2\111\2\0\2\111\1\0\4\111\1\112\12\111\1\153"+
      "\7\111\1\112\7\111\3\21\1\154\7\21\1\114\7\21"+
      "\2\0\1\20\37\0\1\155\25\0\1\156\3\30\1\157"+
      "\7\30\1\122\7\30\3\31\1\160\7\31\1\123\7\31"+
      "\2\0\1\26\22\0\1\33\22\0\1\42\22\0\1\50"+
      "\22\0\1\55\22\0\1\57\22\0\1\63\20\0\3\70"+
      "\1\161\7\70\1\133\7\70\1\134\4\0\1\134\1\0"+
      "\3\134\1\0\1\136\10\134\1\0\1\134\1\162\1\0"+
      "\1\134\1\0\3\134\1\0\1\136\7\134\1\75\1\0"+
      "\1\75\1\163\1\0\1\75\1\0\4\75\1\137\7\75"+
      "\2\0\1\164\1\165\17\0\1\100\1\70\1\100\1\166"+
      "\1\70\1\100\1\70\4\100\1\141\7\100\1\75\1\164"+
      "\3\0\1\75\1\0\4\75\1\102\7\75\2\0\1\142"+
      "\20\0\2\144\3\0\5\144\1\0\1\146\7\144\1\167"+
      "\1\144\3\0\1\167\1\144\3\167\1\75\1\170\7\167"+
      "\3\144\1\171\6\144\1\0\1\146\7\144\2\0\1\105"+
      "\20\0\2\150\2\0\2\150\1\0\3\150\1\0\1\152"+
      "\7\150\1\172\1\150\2\0\1\150\1\172\1\0\3\172"+
      "\1\75\1\173\7\172\3\150\1\174\6\150\1\0\1\152"+
      "\7\150\2\0\1\111\22\0\1\21\36\0\1\175\13\0"+
      "\1\176\15\0\1\30\22\0\1\31\22\0\1\70\22\0"+
      "\1\134\22\0\1\75\21\0\1\164\11\0\1\140\11\0"+
      "\1\164\22\0\1\100\20\0\1\167\1\144\1\167\1\177"+
      "\1\144\1\167\1\144\3\167\1\75\1\170\7\167\2\0"+
      "\1\144\20\0\1\172\1\150\1\172\1\200\1\150\1\172"+
      "\1\150\3\172\1\75\1\173\7\172\2\0\1\150\40\0"+
      "\1\201\16\0\1\202\10\0\1\167\22\0\1\172\41\0"+
      "\1\203\1\0";

  private static int [] zzUnpacktrans() {
    int [] result = new int[1938];
    int offset = 0;
    offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpacktrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\16\0\1\11\7\1\1\11\6\1\3\11\2\1\3\11"+
      "\4\1\2\11\4\1\2\11\3\1\1\11\1\1\1\11"+
      "\1\1\1\11\1\1\1\11\3\1\1\11\1\1\1\11"+
      "\2\1\1\11\3\1\1\11\2\1\1\0\1\1\4\0"+
      "\1\11\2\1\7\0\5\1\1\0\2\1\1\0\3\1"+
      "\1\0\3\1\11\0\1\1\2\0\2\1\1\0\2\1"+
      "\7\0\1\11";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[131];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
   the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
   from input */
  private int zzEndRead;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** Number of newlines encountered up to the start of the matched text. */
  @SuppressWarnings("unused")
  private int yyline;

  /** Number of characters from the last newline up to the start of the matched text. */
  @SuppressWarnings("unused")
  protected int yycolumn;

  /** Number of characters up to the start of the matched text. */
  @SuppressWarnings("unused")
  private long yychar;

  /** Whether the scanner is currently at the beginning of a line. */
  @SuppressWarnings("unused")
  private boolean zzAtBOL = true;

  /** Whether the user-EOF-code has already been executed. */
  @SuppressWarnings("unused")
  private boolean zzEOFDone;


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  _BazelrcLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
  private int zzMaxBufferLen() {
    return Integer.MAX_VALUE;
  }

  /**  Whether the scanner buffer can grow to accommodate a larger token. */
  private boolean zzCanGrow() {
    return true;
  }

  /**
   * Translates raw input code points to DFA table row
   */
  private static int zzCMap(int input) {
    int offset = input & 255;
    return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException
  {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMap(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1:
          { yybegin(CMD); yypushback(1);
          }
          // fall through
          case 32: break;
          case 2:
          { return TokenType.WHITE_SPACE;
          }
          // fall through
          case 33: break;
          case 3:
          { return BazelrcTokenTypes.COMMENT;
          }
          // fall through
          case 34: break;
          case 4:
          { return BazelrcTokenTypes.VALUE;
          }
          // fall through
          case 35: break;
          case 5:
          { yybegin(YYINITIAL); yypushback(1);
          }
          // fall through
          case 36: break;
          case 6:
          { return BazelrcTokenTypes.COMMAND;
          }
          // fall through
          case 37: break;
          case 7:
          { yybegin(FLAGS); return TokenType.WHITE_SPACE;
          }
          // fall through
          case 38: break;
          case 8:
          { yybegin(FLAGS); yypushback(1);
          }
          // fall through
          case 39: break;
          case 9:
          { yybegin(CMD_DQ); return BazelrcTokenTypes.DOUBLE_QUOTE;
          }
          // fall through
          case 40: break;
          case 10:
          { yybegin(CMD_SQ); return BazelrcTokenTypes.SINGLE_QUOTE;
          }
          // fall through
          case 41: break;
          case 11:
          { yybegin(CONFIG); return BazelrcTokenTypes.COLON;
          }
          // fall through
          case 42: break;
          case 12:
          { yybegin(FLAGS); return BazelrcTokenTypes.CONFIG;
          }
          // fall through
          case 43: break;
          case 13:
          { yybegin(CONFIG_DQ); return BazelrcTokenTypes.DOUBLE_QUOTE;
          }
          // fall through
          case 44: break;
          case 14:
          { yybegin(CONFIG_SQ); return BazelrcTokenTypes.SINGLE_QUOTE;
          }
          // fall through
          case 45: break;
          case 15:
          { yybegin(YYINITIAL); return TokenType.WHITE_SPACE;
          }
          // fall through
          case 46: break;
          case 16:
          { yybegin(FLAGS); return BazelrcTokenTypes.DOUBLE_QUOTE;
          }
          // fall through
          case 47: break;
          case 17:
          { yybegin(CONFIG_DQ); return BazelrcTokenTypes.COLON;
          }
          // fall through
          case 48: break;
          case 18:
          { return BazelrcTokenTypes.CONFIG;
          }
          // fall through
          case 49: break;
          case 19:
          { yybegin(FLAGS); return BazelrcTokenTypes.SINGLE_QUOTE;
          }
          // fall through
          case 50: break;
          case 20:
          { yybegin(CONFIG_SQ); return BazelrcTokenTypes.COLON;
          }
          // fall through
          case 51: break;
          case 21:
          { yybegin(VALUE); yypushback(1);
          }
          // fall through
          case 52: break;
          case 22:
          { yybegin(FLAG_DQ); return BazelrcTokenTypes.DOUBLE_QUOTE;
          }
          // fall through
          case 53: break;
          case 23:
          { yybegin(YYINITIAL); return BazelrcTokenTypes.COMMENT;
          }
          // fall through
          case 54: break;
          case 24:
          { yybegin(FLAG_SQ); return BazelrcTokenTypes.SINGLE_QUOTE;
          }
          // fall through
          case 55: break;
          case 25:
          { yybegin(VALUE); return BazelrcTokenTypes.EQ;
          }
          // fall through
          case 56: break;
          case 26:
          { yybegin(VALUE_DQ); return BazelrcTokenTypes.DOUBLE_QUOTE;
          }
          // fall through
          case 57: break;
          case 27:
          { yybegin(VALUE_SQ); return BazelrcTokenTypes.SINGLE_QUOTE;
          }
          // fall through
          case 58: break;
          case 28:
          { yybegin(VALUE_DQ); return BazelrcTokenTypes.EQ;
          }
          // fall through
          case 59: break;
          case 29:
          { yybegin(VALUE_SQ); return BazelrcTokenTypes.EQ;
          }
          // fall through
          case 60: break;
          case 30:
          { return BazelrcTokenTypes.FLAG;
          }
          // fall through
          case 61: break;
          case 31:
          { yybegin(IMPORT); return BazelrcTokenTypes.IMPORT;
          }
          // fall through
          case 62: break;
          default:
            zzScanError(ZZ_NO_MATCH);
        }
      }
    }
  }


}
