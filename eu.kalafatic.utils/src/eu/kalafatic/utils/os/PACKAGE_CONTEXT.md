# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.utils/src/eu/kalafatic/utils/os/

## Domain: general

## Components
* `OSUtils.java`: package eu.kalafatic.utils.os; import java.util.Properties; public class OSUtils { private Properties properties = System.getProperties(); public static final OSUtils INSTANCE = new OSUtils(); public String getOSName() { return properties.getProperty("os.name"); } public String getOSArch() { return properties.getProperty("os.arch");
