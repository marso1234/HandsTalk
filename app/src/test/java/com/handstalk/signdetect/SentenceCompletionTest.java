package com.handstalk.signdetect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import com.handstalk.signdetect.sentenceCompletion.SentenceCompletionClient;

import org.junit.Test;

public class SentenceCompletionTest {
    @Test
    public void sentenceCompletion_General(){
        SentenceCompletionClient sentenceCompletionClient = SentenceCompletionClient.getInstance();
        sentenceCompletionClient.reset();

        sentenceCompletionClient.addWord("hi");
        sentenceCompletionClient.addWord("today");
        sentenceCompletionClient.addWord("weather");
        sentenceCompletionClient.addWord("good");
        sentenceCompletionClient.addWord("dance");
        sentenceCompletionClient.addWord("ask");
        sentenceCompletionClient.addWord("together");

        sentenceCompletionClient.run();
        try {
            Thread.sleep(3000);
            System.out.println(sentenceCompletionClient.getResult());
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(sentenceCompletionClient.getResult().isEmpty());
    }

    @Test
    public void sentenceCompletion_NotEnoughWord(){
        SentenceCompletionClient sentenceCompletionClient = SentenceCompletionClient.getInstance();
        sentenceCompletionClient.reset();

        sentenceCompletionClient.addWord("hi");
        sentenceCompletionClient.addWord("today");

        sentenceCompletionClient.run();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(sentenceCompletionClient.getResult().isEmpty());
    }

    @Test
    public void sentenceCompletion_Duplication(){
        SentenceCompletionClient sentenceCompletionClient = SentenceCompletionClient.getInstance();
        sentenceCompletionClient.reset();

        sentenceCompletionClient.addWord("hi");
        sentenceCompletionClient.addWord("hi");
        sentenceCompletionClient.addWord("hi");
        sentenceCompletionClient.addWord("today");

        sentenceCompletionClient.run();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Result: "+sentenceCompletionClient.getResult());
        assertTrue(sentenceCompletionClient.getResult().isEmpty());
    }

    @Test
    public void sentenceCompletion_letters(){
        SentenceCompletionClient sentenceCompletionClient = SentenceCompletionClient.getInstance();
        sentenceCompletionClient.reset();

        sentenceCompletionClient.addWord("Me");
        sentenceCompletionClient.addWord("name");
        sentenceCompletionClient.addWord("H");
        sentenceCompletionClient.addWord("A");
        sentenceCompletionClient.addWord("P");
        sentenceCompletionClient.addWord("P");
        sentenceCompletionClient.addWord("Y");

        sentenceCompletionClient.run();
        try {
            Thread.sleep(3000);
            System.out.println(sentenceCompletionClient.getResult());
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(sentenceCompletionClient.getResult().isEmpty());
    }
}
