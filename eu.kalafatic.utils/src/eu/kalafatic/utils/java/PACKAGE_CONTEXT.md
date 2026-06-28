# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.utils/src/eu/kalafatic/utils/java/

## Domain: general

## Components
* `EnumUtils.java`: package eu.kalafatic.utils.java; import java.util.ArrayList; import java.util.List; public class EnumUtils { public static List<String> getEnumItems(Enum... enums) { List<String> result = new ArrayList<String>(); for (Enum enu : enums) { result.add(enu.toString()); } return result; } public static Enum<?> getByName(Object[] values, String layoutName) { for (int i = 0; i < values.length; i++) { if (values[i].toString().equalsIgnoreCase(layoutName)) { return (Enum<?>) values[i]; } } return null; } }
