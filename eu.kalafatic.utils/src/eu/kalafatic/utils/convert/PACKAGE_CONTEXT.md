# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/convert/

## Domain: general

## Components
* `ConvertUtils.java`: package eu.kalafatic.utils.convert; import static eu.kalafatic.utils.constants.FUIConstants.GB; import static eu.kalafatic.utils.constants.FUIConstants.H; import static eu.kalafatic.utils.constants.FUIConstants.KB; import static eu.kalafatic.utils.constants.FUIConstants.M; import static eu.kalafatic.utils.constants.FUIConstants.MB; import static eu.kalafatic.utils.constants.FUIConstants.S; import java.io.IOException; import java.math.BigInteger; import java.nio.ByteBuffer; import java.nio.CharBuffer; import java.nio.charset.CharacterCodingException; import java.nio.charset.Charset; import java.nio.charset.CharsetDecoder; import java.text.DecimalFormat; import java.text.NumberFormat; public class ConvertUtils { public static Integer getInteger(Object object) { Integer integer = null; if (object instanceof Long) {
