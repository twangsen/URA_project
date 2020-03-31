import token.*;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class Statement {

  public static List<Statement> getJCStatements(final List<Token> allTokens, boolean setting)
      throws EmptyStackException {

    useGlobal = setting;
    globalIdentifiers = new HashMap<>();

    final List<Statement> statements = new ArrayList<>();
    List<Token> buildingStatementTokens = new ArrayList<>();

    final Stack<Integer> nestLevel = new Stack<>();
    nestLevel.push(Integer.valueOf(1));
    int inAnnotationDepth = 0;
    int inParenDepth = 0;
    int inTernaryOperationDepth = 0;
    int index = 0;
    Token previousToken = null;
    //final boolean isDebug = config.isDEBUG();

    try {
      for (final Token token : allTokens) {

        // token.index = index++;
        if (0 < inAnnotationDepth) {
          final ANNOTATION annotation = new ANNOTATION(token.value);
          annotation.index = index++;
          annotation.line = token.line;
          buildingStatementTokens.add(annotation);
        } else {
          token.index = index++;
          buildingStatementTokens.add(token);
        }

        if ((0 == inParenDepth) && (token instanceof RIGHTBRACKET)) {
          if (0 == nestLevel.peek()
              .intValue()) {
            nestLevel.pop();
            nestLevel.pop();
          } else {
            nestLevel.pop();
          }
        }

        if ((token instanceof QUESTION) && !(previousToken instanceof LESS)) {
          inTernaryOperationDepth++;
        }

        if (token instanceof RIGHTPAREN) {
          inParenDepth--;
          if (0 < inAnnotationDepth) {
            inAnnotationDepth--;
          }
        }

        if ((0 == inParenDepth) && (0 == inTernaryOperationDepth)
            && (token instanceof LEFTBRACKET || token instanceof RIGHTBRACKET
                || token instanceof SEMICOLON || token instanceof COLON)) {

          if (1 < buildingStatementTokens.size()) {

            if (isJCTypeDefinition(buildingStatementTokens)) {
              nestLevel.push(Integer.valueOf(0));
            }
            final int nestDepth = nestLevel.peek()
                .intValue();

            final int fromLine = buildingStatementTokens.get(0).line;
            final int toLine = buildingStatementTokens.get(buildingStatementTokens.size() - 1).line;
            final String rText = makeText(buildingStatementTokens);
            /*
            final List<Token> nonTrivialTokens = config.isCOUNT_MODIFIER() ? buildingStatementTokens
                : removeJCTrivialTokens(buildingStatementTokens);*/
            final List<Token> nonTrivialTokens =  buildingStatementTokens;

            // final List<Token> normalizedTokens = config.isNORMALIZATION()
            // ? normalizeJCTokens(nonTrivialTokens)
            // : nonTrivialTokens;
            final List<Token> normalizedTokens = normalizeJCTokens(nonTrivialTokens);
            final String nText = makeText(normalizedTokens);
            final byte[] hash = getMD5(nText);
            final Statement statement = new Statement(fromLine, toLine, nestDepth, 1 < nestDepth,
                buildingStatementTokens, rText, nText, hash);
            statements.add(statement);
            buildingStatementTokens = new ArrayList<Token>();

            /*
            if (isDebug) {
              System.out.println(statement.toString());
            }*/
          }

          else {
            buildingStatementTokens.clear();
          }
        }

        if ((0 == inParenDepth) && (token instanceof LEFTBRACKET)) {
          nestLevel.push(Integer.valueOf(nestLevel.peek()
              .intValue() + 1));
        }

        if ((0 < inTernaryOperationDepth) && (token instanceof COLON)) {
          inTernaryOperationDepth--;
        }

        if (token instanceof LEFTPAREN) {
          inParenDepth++;
          if ((1 < buildingStatementTokens.size()) && (buildingStatementTokens
              .get(buildingStatementTokens.size() - 2) instanceof ANNOTATION)) {
            inAnnotationDepth++;
            buildingStatementTokens.remove(buildingStatementTokens.size() - 1);
            final ANNOTATION annotation = new ANNOTATION(token.value);
            annotation.index = index++;
            annotation.line = token.line;
            buildingStatementTokens.add(annotation);
          }
        }

        previousToken = token;
      }
    }

    catch (final EmptyStackException e) {
      System.err.println("parsing error has happened.");
    }

    return statements;
  }

  private static List<Token> normalizeJCTokens(final List<Token> tokens) {

    final List<Token> normalizedTokens = new ArrayList<>();
    final Map<String, String> identifiers = new HashMap<>();
    final Map<String, String> types = new HashMap<>();

    for (int index = 0; index < tokens.size(); index++) {

      final Token token = tokens.get(index);

      if (token instanceof IDENTIFIER) {

        if (index < tokens.size() && tokens.get(index + 1) instanceof LEFTPAREN) {
          normalizedTokens.add(token);
        }

        else if (Character.isLowerCase(token.value.charAt(0))) {
          String normalizedValue = identifiers.get(token.value);

          if(useGlobal){
            normalizedValue = globalIdentifiers.get(token.value);
            if (null == normalizedValue) {
              normalizedValue = "$V"+ globalIdentifiers.size();
              globalIdentifiers.put(token.value, normalizedValue);
            }
          } else {
            if (null == normalizedValue) {
              normalizedValue = "$V" + identifiers.size();
              identifiers.put(token.value, normalizedValue);
            }

          }

          final IDENTIFIER normalizedIdentifier = new IDENTIFIER(normalizedValue);
          normalizedIdentifier.index = token.index;
          normalizedIdentifier.line = token.line;
          normalizedTokens.add(normalizedIdentifier);
        }

        else if (Character.isUpperCase(token.value.charAt(0))) {
          // String normalizedValue = types.get(token.value);
          // if (null == normalizedValue) {
          // normalizedValue = "$T" + types.size();
          // types.put(token.value, normalizedValue);
          // }
          // builder.append(normalizedValue);
          normalizedTokens.add(token);
        }
      }

      else if (token instanceof CHARLITERAL) {
        // final CHARLITERAL literal = new CHARLITERAL("C");
        final CHARLITERAL literal = new CHARLITERAL("$L");
        literal.index = token.index;
        literal.line = token.line;
        normalizedTokens.add(literal);
      }

      else if (token instanceof NUMBERLITERAL) {
        // final NUMBERLITERAL literal = new NUMBERLITERAL("N");
        final NUMBERLITERAL literal = new NUMBERLITERAL("$L");
        literal.index = token.index;
        literal.line = token.line;
        normalizedTokens.add(literal);
      }

      else if (token instanceof STRINGLITERAL) {
        // final STRINGLITERAL literal = new STRINGLITERAL("S");
        final STRINGLITERAL literal = new STRINGLITERAL("$L");
        literal.index = token.index;
        literal.line = token.line;
        normalizedTokens.add(literal);
      }

      else if ((token instanceof TRUE) || (token instanceof FALSE)) {
        // final BOOLEANLITERAL literal = new BOOLEANLITERAL("B");
        final BOOLEANLITERAL literal = new BOOLEANLITERAL("$L");
        literal.index = token.index;
        literal.line = token.line;
        normalizedTokens.add(literal);
      }

      else {
        normalizedTokens.add(token);
      }
    }

    return normalizedTokens;
  }

  private static String makeText(final List<Token> tokens) {
    final String[] array = tokens.stream()
        .map(token -> token.value)
        .collect(Collectors.toList())
        .toArray(new String[0]);
    final String text = String.join(" ", array);
    return text;
  }

  private static List<Token> removeJCTrivialTokens(final List<Token> tokens) {
    final List<Token> nonTrivialTokens = new ArrayList<>();
    for (final Token token : tokens) {

      if (token instanceof ABSTRACT || token instanceof FINAL || token instanceof PRIVATE
          || token instanceof PROTECTED || token instanceof PUBLIC || token instanceof STATIC
          || token instanceof STRICTFP || token instanceof TRANSIENT) {
        // not used for making hash
        continue;
      }

      else if (token instanceof ANNOTATION) {
        continue;
      }

      else {
        nonTrivialTokens.add(token);
      }
    }

    return nonTrivialTokens;
  }

  public static byte[] getMD5(final String text) {
    try {
      final MessageDigest md = MessageDigest.getInstance("MD5");
      final byte[] data = text.getBytes("UTF-8");
      md.update(data);
      final byte[] digest = md.digest();
      return digest;
    } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
      e.printStackTrace();
      return new byte[0];
    }
  }

  private static boolean isJCTypeDefinition(final List<Token> tokens) {
    final List<Token> nonTrivialTokens = removeJCTrivialTokens(tokens);
    final Token firstToken = nonTrivialTokens.get(0);
    if (firstToken instanceof CLASS || firstToken instanceof INTERFACE) {
      return true;
    } else {
      return false;
    }
  }


  private static Map<String, String> globalIdentifiers;
  private static boolean useGlobal = true;

  final public int fromLine;
  final public int toLine;
  final public int nestLevel;
  final public boolean isTarget;
  final public List<Token> tokens;
  final public String rText;
  final public String nText;
  final public byte[] hash;

  public Statement(final int fromLine, final int toLine, final int nestLevel,
                   final boolean isTarget, final List<Token> tokens, final String rText, final String nText,
                   final byte[] hash) {
    this.fromLine = fromLine;
    this.toLine = toLine;
    this.tokens = tokens;
    this.nestLevel = nestLevel;
    this.isTarget = isTarget;
    this.rText = rText;
    this.nText = nText;
    this.hash = Arrays.copyOf(hash, hash.length);
  }

  @Override
  public int hashCode() {
    final BigInteger value = new BigInteger(1, this.hash);
    return value.toString(16)
        .hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (null == o) {
      return false;
    }
    if (!(o instanceof Statement)) {
      return false;
    }

    return Arrays.equals(this.hash, ((Statement) o).hash);
  }

  @Override
  public String toString() {
    return this.nText;
  }
}
