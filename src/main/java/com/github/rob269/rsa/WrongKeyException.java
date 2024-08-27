package com.github.rob269.rsa;

public class WrongKeyException extends Exception{
    public WrongKeyException(String message) {
        super(message);
    }
}
