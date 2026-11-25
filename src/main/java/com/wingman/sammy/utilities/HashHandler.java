package com.wingman.sammy.utilities;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashHandler {

    @PostConstruct
    private void verifyAlgorithms () throws NoSuchAlgorithmException{
        MessageDigest.getInstance("SHA-256");
    }

    public String encode (byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digestToString(digest.digest(input));
        }catch (NoSuchAlgorithmException e){
            throw new RuntimeException("Algorithm not found");
        }
    }

    private String digestToString (byte[] digest){
        return HexFormat.of().formatHex(digest);
    }
}
