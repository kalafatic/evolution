# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/parsers/

## Domain: general

## Components
* `StringUtils.java`: package eu.kalafatic.utils.parsers; import java.util.ArrayList; import java.util.List; import java.util.regex.Matcher; import java.util.regex.Pattern; import eu.kalafatic.utils.constants.ERegex; public class StringUtils { public static String split(String name) { return split(name, ERegex.SPLIT_NAME.getRegex()); } public static String split(String name, String regex) { StringBuffer stringBuffer = new StringBuffer(); String[] splitArray = name.split(regex);
