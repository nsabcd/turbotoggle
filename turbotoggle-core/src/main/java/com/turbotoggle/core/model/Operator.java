package com.turbotoggle.core.model;

public interface Operator {
    String EQUALS = "EQUALS";
    String NOT_EQUALS = "NOT_EQUALS";
    String IN = "IN";
    String NOT_IN = "NOT_IN";
    String GREATER_THAN = "GREATER_THAN";
    String LESS_THAN = "LESS_THAN";
    String GREATER_THAN_OR_EQUAL = "GREATER_THAN_OR_EQUAL";
    String LESS_THAN_OR_EQUAL = "LESS_THAN_OR_EQUAL";
    String REGEX = "REGEX";
    String CONTAINS = "CONTAINS";
    String NOT_CONTAINS = "NOT_CONTAINS";
    String IS_TRUE = "IS_TRUE";
    String IS_FALSE = "IS_FALSE";
}